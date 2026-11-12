# Aurora 透明代理（ROOT）实现方案

当前实现为实验性功能。需要通过完整 APK 构建和已 Root 设备验收后再作为稳定功能发布。

## 目标

在「设置 → 代理模式」提供 VPN、代理、透明代理。使用 Aurora 现有配置、订阅、策略组、连接与诊断界面；无需 Android VPN 授权或系统代理地址。三种模式互斥。

## 当前约束

- 代理方式目前由 `UiStore.enableVpn` 布尔值表示，启动时在 `TunService` 与 `ClashService` 之间选择。先迁移到三值枚举，旧值分别映射到 VPN 和代理服务。
- mihomo 当前作为 Android 服务中的共享库运行。TProxy 的 TCP/UDP 监听需要设置 `IP_TRANSPARENT`，不能假定普通应用进程拥有所需权限；只安装 Root 防火墙规则无法构成完整方案。
- mihomo 配置加载流程会关闭配置里的 TUN 和自动路由，并过滤 TProxy/Redirect/TUN 自定义监听。ROOT 模式必须建立单独的加载与运行路径。
- 现有 UI 通过 `IClashManager` 访问应用内核。ROOT 模式若使用独立核心进程，必须让这些查询和操作指向实际承载流量的核心，不能同时运行两个状态不一致的核心。

## 推荐架构：Aurora 自带 Root 运行组件

1. 从仓库已有的 mihomo 源码构建 Android 各 ABI 的独立可执行文件。APK 随对应 ABI 的共享内核附带校验过的差量数据；用户点击「安装透明代理」时在本机还原可执行文件，不在运行时下载核心。
2. 从 Aurora 已导入的配置生成 ROOT 专用快照，启用 mihomo TUN 自动路由与自动重定向，并将控制接口限定在本机。代理核心在 `/data/adb/aurora/` 运行，不要求用户维护第二份配置。
3. 通过 `su` 启停独立核心；流量规则由 mihomo 的 TUN 自动路由管理。ROOT 配置使用 Aurora 专用的路由表 20222、规则优先级 9000 和 `aurora-*` 防火墙链。卸载时停止核心，并只清理这些专用资源。
4. ROOT 模式的管理层连接独立核心的本机控制接口，适配 `IClashManager` 所需的状态、策略、连接、流量、日志与配置操作。控制接口使用随机密钥，限制在本机。
5. 安装顺序：用户点击安装 → 确认 ROOT 权限 → 从共享库和差量数据还原并校验可执行文件 → 写入 `/data/adb/aurora/<packageName>/mihomo` → 记录当前 APK 的安装时间。切换到透明代理不会自动安装。
6. 启动顺序：确认已安装且与当前 APK 匹配 → 生成配置 → 启动核心 → 通过控制接口确认就绪 → 标记运行。未安装时提醒用户先到设置页安装。
7. 卸载顺序：停止核心 → 清理 Aurora 的防火墙链、路由表、规则和 TUN 接口 → 验证这些资源不再存在 → 删除 Aurora ROOT 目录和应用内临时文件 → 清除安装标记并切回 VPN。清理验证失败时保留标记以便重试。
8. 停止时向核心发送 TERM 并等待退出；Aurora 进程消失后，独立的监护 shell 也会尝试停止核心。核心被强制杀死或设备异常断电后的规则恢复仍需实机验证。

## 第一版范围

- 首版以本机 IPv4 TCP/UDP 和 DNS 接管为验收范围；代理核心自身和 Aurora 的流量不能自环。
- 保留 VPN 与代理服务行为；ROOT 模式缺少权限或组件时不可启动，并给出明确原因。
- ROOT 模式暂不提供分应用范围；现有 `VpnService` 专属设置只在 VPN 模式显示。后续可接入 mihomo 的 `include-uid` / `exclude-uid`。
- IPv6、热点转发及不同 Root 管理器的兼容性需要单独验证后再开放对应开关。未覆盖的流量行为必须明确展示，不能默默宣称全流量接管。

## 验收

- 模式切换和正常停止后，不残留 Aurora 自有规则导致断网。应用进程被杀、设备重启与升级仍需在真实设备上验证。
- TCP、UDP、DNS 均按当前配置的规则和策略出口运行，策略组与连接页面显示实际 ROOT 核心状态。
- 非 Root 设备仍能使用现有 VPN 与代理服务；旧的 `enableVpn` 偏好迁移后不变。
- 在 Magisk、KernelSU、APatch 的真实设备上分别验证，记录 IPv4/IPv6、Wi-Fi/移动网络切换结果。
- ROOT 管理器的授权记录和操作日志由管理器控制，Aurora 的卸载操作不负责清除这些记录。

## 参考

- boxproxy/box: <https://github.com/boxproxy/box>
- box Wiki: <https://github.com/boxproxy/box/wiki>
- box 当前为 GPL-3.0；直接复用代码时保留来源、版权及许可声明。

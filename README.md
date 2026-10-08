# Aurora

Aurora 是使用 [mihomo](https://github.com/MetaCubeX/mihomo) 内核的原生 Android 代理客户端，基于 [Clash Meta for Android](https://github.com/MetaCubeX/ClashMetaForAndroid) 的代码继续开发。原工程的代理核心、VPN 服务、配置兼容性和规则处理能力得以保留。

## 下载

在 [GitHub Releases](https://github.com/koude/Aurora/releases) 下载 APK。正式版提供 ARM、ARM64、x86、x86_64 和通用包；预发布仅提供 ARM64 包。历史版本 v1.0 仅提供 ARM64 包。

Aurora 支持 Android 5.0 及以上系统。使用前需自行导入本地配置或远程订阅；本项目不提供代理节点或订阅服务。

## 与原项目相比

### 技术框架

界面逐步迁移到 Jetpack Compose 与 Material 3。首页、代理、连接和设置采用同级导航；界面状态与操作逐步整理到 ViewModel 和数据层。mihomo 内核及现有 VPN 服务仍沿用原工程，不另行重写。

### 优化

- 重新设计四个主要页面及配置管理，统一导航、标题和交互方式。
- 调整策略组与组内选项的层级表达，改善节点选择和延迟测试的可读性。
- 为配置导入、订阅更新和文件下载增加更明确的过程反馈。

### 增加

- 常用网站的连通性检测。
- 输入目标网站后，预判当前配置会命中的规则、策略和出口。
- 连接搜索，以及仅关闭当前筛选结果中的连接。

### 精简

移除已替换的旧界面、重复入口和不再使用的资源；将不常用的配置管理放入设置。以上调整不删减 mihomo 的代理核心能力。

## 开源与致谢

感谢 Clash Meta for Android、mihomo 及其贡献者。许可证见 [LICENSE](LICENSE)，第三方软件声明见 [NOTICE](NOTICE)。

# Aurora

Aurora 是一款使用 [mihomo](https://github.com/MetaCubeX/mihomo) 内核的原生 Android 客户端，基于 [Clash Meta for Android](https://github.com/MetaCubeX/ClashMetaForAndroid) 的代码继续开发。原有的代理、规则、配置与 VPN 能力得以保留；Aurora 主要改进日常使用体验。

## 下载与安装

从 [GitHub Releases](https://github.com/koude/Aurora/releases) 下载 APK，支持 Android 5.0 及以上系统。预发布仅提供 arm64-v8a；今后的正式版将提供各架构及通用 APK。历史正式版 v1.0 仍仅提供 arm64-v8a。

使用前需自行导入本地配置或远程订阅；项目不提供代理节点或订阅服务。

## Aurora 的改动

- **优化界面与操作：**重新设计首页、代理、连接和设置页面；让策略组与其中的选项更易区分，配置导入和订阅更新有明确的进度反馈。
- **增加日常工具：**常用网站连通性检测、输入网址后预判命中的规则与出口，以及连接搜索和按筛选结果关闭连接。
- **精简旧界面：**移除已替换的旧版页面、重复入口和不再使用的资源，将较少使用的配置管理收进设置。代理核心能力没有因此删减。

Aurora 支持 VPN 接管和本地代理服务、配置导入与切换、策略组和节点选择、延迟测试、连接查看，以及分应用代理。部分功能的可用性取决于配置文件、Android 版本与设备环境。

## 开源与致谢

感谢 Clash Meta for Android、mihomo 及其贡献者。项目许可证见 [LICENSE](LICENSE)，第三方软件声明见 [NOTICE](NOTICE)。

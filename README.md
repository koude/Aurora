# Aurora

Aurora 是基于 [Clash Meta for Android](https://github.com/MetaCubeX/ClashMetaForAndroid) 与 [mihomo](https://github.com/MetaCubeX/mihomo) 的原生 Android 代理客户端。项目保留成熟的代理核心和 Android 服务能力，使用 Jetpack Compose 重做界面与导航。

## 下载与安装

从 [GitHub Releases](https://github.com/koude/Aurora/releases) 下载官方签名的 `arm64-v8a` APK。正式版为 [v1.0](https://github.com/koude/Aurora/releases/tag/v1.0)；后续日历版本默认标记为预发布。安装更新时须使用相同签名的 APK；不同签名的本地构建不能直接覆盖官方版本。

官方 APK 仅提供 `arm64-v8a`，适用于 Android 5.0 及以上的对应设备。应用包名为 `com.koude.aurora`。使用前需自行导入本地配置或远程订阅；项目不提供代理节点或订阅服务。

## 主要功能

- Android VPN 与本地代理服务两种接入方式
- 配置导入、订阅更新与配置切换
- 策略组和节点选择、延迟测试、连接查看与关闭
- 分应用代理、网络选项及 mihomo 内核功能设置
- 网站连通性检测与路由预判

部分功能的可用性取决于配置文件、Android 版本与设备环境。

## 从源码构建

需要 JDK 21、Go 1.24、Android SDK Platform 35、Build-Tools 35.0.0、NDK 29.0.14206865 和 CMake 3.22.1。先初始化子模块，并在项目根目录创建 `local.properties`：

```properties
sdk.dir=/path/to/android-sdk
custom.application.id=com.koude.aurora
remove.suffix=true
```

然后运行：

```bash
git submodule update --init --recursive
./gradlew app:assembleAlphaDebug
```

本地 Debug 构建使用开发签名，仅建议安装在测试设备或模拟器上。官方 Release APK 由 [GitHub Actions](https://github.com/koude/Aurora/actions) 使用固定签名构建；如需自行构建可更新的 Release 包，必须持有自己的签名密钥，不能与官方 APK 混装。

## 开源与致谢

Aurora 延续 Clash Meta for Android 的 Android 工程并集成 mihomo 核心。感谢上述项目及其贡献者。项目许可证见 [LICENSE](LICENSE)，第三方软件声明见 [NOTICE](NOTICE)。

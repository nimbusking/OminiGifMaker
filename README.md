# OminiGifMaker

跨平台桌面端 GIF 处理工具。所有图像处理均在本地完成，内置五个引擎、开箱即用，无需网络上传。

A cross-platform desktop GIF tool — all processing happens locally, with five engines bundled and ready to use.

## 特性

- **本地处理**：全部计算通过内置引擎子进程在本机完成，无任何网络上传。
- **开箱即用**：编译期静态内置 FFmpeg、Gifsicle、ImageMagick、gifski、libvips 五个引擎，首次运行自动解压。
- **跨平台**：Kotlin + Compose Multiplatform，可打包为 `.exe` / `.dmg` / `.deb`。
- **中英文切换**：内置国际化，设置页可一键切换语言并持久化。
- **记住设置**：各模块表单参数可跨会话持久化（基于系统 Preferences）。

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| Gradle Wrapper | 9.4.1 |
| JDK | 21（toolchain） |
| kotlinx-coroutines | 1.10.2 |
| kotlinx-serialization | 1.8.1 |
| 设置持久化 | `java.util.prefs.Preferences` |

## 架构

### 目录结构

```
src/
├── commonMain/kotlin/                 # 跨平台通用逻辑
│   ├── model/                         # 数据模型（GifMetaData、各模块 Config/枚举）
│   └── state/                         # 状态层（AppState、各模块 StateHolder、AppStrings、TaskStatus）
└── desktopMain/
    ├── kotlin/
    │   ├── Main.kt                    # 桌面入口（引擎解压 → Window 1280×820 → 主题/语言提供）
    │   ├── ui/
    │   │   ├── layout/                # 主体布局（AppLayout / AppNavigationRail / MainContentArea）
    │   │   ├── components/            # 复用组件（FileMetaDataPanel / ResultPreview / NumberField / DropdownSelector / PlaceholderContent）
    │   │   ├── tabs/                  # 各功能模块面板（7 个一期 + 6 个二期占位）
    │   │   ├── pages/                 # 设置页 / 关于页
    │   │   └── LocalAppStrings.kt     # 国际化 CompositionLocal
    │   └── core/
    │       ├── engine/                # EngineType / EngineExtractor / ProcessRunner
    │       ├── command/               # 命令生成器（各模块 Builder）+ CommandRunner
    │       ├── metadata/              # GifMetaDataReader（基于 ffprobe）
    │       ├── settings/              # SettingsStore（Preferences 封装）
    │       └── utils/                 # OsUtils（OS/架构检测）
    └── resources/
        ├── binaries/{windows,macos,linux}/   # 引擎二进制（目前仅 windows 已内嵌）
        └── icons/                             # 应用图标（.ico/.icns/.png）
```

### 分层设计

- **模型层（`commonMain/model`）**：纯数据类与枚举，如 `GifMetaData`、`ResizeConfig`、`ResizeMethod`、`TaskStatus`。
- **状态层（`commonMain/state`）**：视作 ViewModel 层。
  - `AppState` 持有全局状态：当前源 GIF 路径、基础元数据、当前页面（`AppPage`）、跨模块任务状态（`TaskStatus`）、当前语言（`Language`）。
  - 各模块 `XxxState`（如 `ResizeState`、`CropState`）持有表单参数，切换 Tab 时状态不丢失。
- **命令层（`desktopMain/core/command`）**：`XxxCommandBuilder` 将表单状态映射为引擎 CLI 参数（`EngineCommand`），并负责参数校验；`CommandRunner` 统一执行。
- **引擎层（`desktopMain/core/engine`）**：`EngineExtractor` 负责运行时按 `manifest.txt` 整体释放二进制；`ProcessRunner` 封装 `ProcessBuilder`。
- **UI 层（`desktopMain/ui`）**：Compose 界面，通过 `LocalAppStrings` 取文案，实现中英文切换。

### 状态管理

- 全程使用 `StateFlow` 暴露状态，Compose 通过 `collectAsState()` 订阅。
- 任务状态用密封接口 `TaskStatus` 表达三态：`Idle` / `Running` / `Success(outputPath)` / `Failed(message)`，UI 据此渲染加载态 / 结果 / 错误。
- 模块级状态存于上层 `AppState`（而非 Tab 内部），避免切换 Tab 时表单数据丢失。

### 国际化（i18n）

- `Language` 枚举（`ZH` / `EN`），`AppStrings(lang)` 集中提供所有用户可见文案与枚举中文标签。
- `AppState.language` 为 `StateFlow<Language>`，切换即触发全界面重组合并持久化到 Preferences。
- 首次运行按系统语言初始化，之后以设置页选择为准。

### 引擎调用与进程调度

1. 启动时 `EngineExtractor` 检测宿主 OS，按 `binaries/<os>/manifest.txt` 从 classpath 流式释放整个 bundle（可执行文件 + 依赖 DLL/配置文件）到 `java.io.tmpdir/gif_app_engines/`，非 Windows 平台赋予可执行权限。
2. `ProcessRunner` 在 `Dispatchers.IO` 中执行 `ProcessBuilder`，以 `List<String>` 传参防注入，并**并发**读取 stdout/stderr 防止缓冲区满导致死锁。
3. 元数据统一由 `GifMetaDataReader` 调用 ffprobe（`-print_format json`）解析。

### 设置持久化

- `commonMain` 定义 `KeyValueStore` 抽象接口；`desktopMain` 的 `SettingsStore`（封装 `java.util.prefs.Preferences`）实现它。
- 「Remember settings」勾选后，模块表单参数在变更时写入，下次启动回填；引擎解压目录、语言等全局配置同样持久化。

## 内置引擎

| 引擎 | 版本 | 用途 | 许可证 |
| --- | --- | --- | --- |
| FFmpeg（含 ffprobe） | 9.0.1-essentials | 复杂变换、元数据读取、计时器叠加 | GPL v3 |
| Gifsicle | 1.95 | GIF 优化、裁剪、90°/翻转、延迟、倒放 | GPL-2.0 |
| ImageMagick | 7.1.2-29 | 任意角度旋转、画布扩展、帧合成 | ImageMagick License |
| gifski | 1.7.1（CLI） | 高质量 GIF 重编码 | AGPL-3.0 |
| libvips | 8.18.5 | 高性能图像处理（Frames 转换） | LGPL-2.1 |

各引擎均以独立子进程调用，与主程序仅通过命令行与标准流交互，属 GPL 语境下的「聚合分发」，不传染主程序代码。详见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) 与 `third_party/`。

## 功能模块

### 一期（已实现）

| 模块 | 功能 | 引擎 |
| --- | --- | --- |
| Resize（调整大小） | 宽高/百分比缩放、四种处理方法（Gifsicle / IM / IM+coalesce / 改画布） | Gifsicle / ImageMagick |
| Crop（裁剪） | 可拖拽裁剪预览、四角手柄、长宽比锁定、自动裁剪 | Gifsicle / ImageMagick |
| Rotate（旋转与翻转） | 90°/180°/翻转 + 自定义角度 | Gifsicle / ImageMagick |
| Reverse（倒放与播放） | 倒放、回旋镖、循环次数、计时器叠加、翻转 | Gifsicle / FFmpeg |
| Speed（速度调整） | 百分比变速 / 绝对帧延迟 | FFmpeg / Gifsicle |
| Optimize（优化压缩） | 有损、gifski 重编码、减少颜色、去重复、透明度、coalesce | Gifsicle / gifski |
| Frames（帧管理与合成） | 多帧序列 → GIF、全局延迟/循环、三种转换引擎 | ImageMagick / libvips |

### 二期（占位）

`Effects` / `Add text` / `Censor` / `Add image` / `Cut` / `Split` 六个模块已建立空视图占位（按设计文档一期暂不实现业务逻辑）。

### 应用级页面

- **Settings（设置）**：语言切换、引擎解压目录、清除已保存设置。
- **About（关于）**：应用信息、版本、许可证。

## 构建与运行

```bash
# 编译
./gradlew build

# 运行（开发）
./gradlew run

# 打包当前平台安装包（.exe / .dmg / .deb）
./gradlew package
```

> 首次编译会下载依赖，耗时数分钟。

## 打包分发

`compose.desktop.application` 的 `nativeDistributions` 配置了 `targetFormats(TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)`，并为各平台绑定了 `icons/` 下的 `.ico` / `.icns` / `.png` 图标。

## 许可证与第三方组件

本项目以 **GPL-3.0** 协议开源（见 [`LICENSE`](LICENSE)）。内置五个引擎的版本、许可证与来源见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)，各引擎的 LICENSE / COPYING 随附于 `third_party/` 目录。

## 已知限制 / 说明

- 目前仅 **Windows** 平台已内嵌引擎二进制（`binaries/windows/` 约 250MB），`macos/`、`linux/` 目录为空占位，需按平台补齐二进制并更新 `manifest.txt`。
- gifski 的 Windows CLI 取自 npm `gifski` 包（官方仅发布 GUI），版本为 1.7.1。
- libvips 转换要求帧为 RGBA/灰度（RGB 需先加 alpha 通道）。
- 两个原生文件选择对话框标题暂未随语言切换。
- 设计文档见 [`doc/详细设计终稿.md`](doc/详细设计终稿.md)。

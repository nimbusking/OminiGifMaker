# 第三方组件声明（THIRD_PARTY_NOTICES）

本项目 **OminiGifMaker** 以 GPL-3.0 协议开源。下列引擎均以**独立子进程**（`ProcessBuilder`）方式调用，与 Kotlin 主程序之间仅通过命令行与标准流交互，不构成链接或衍生作品，属于 GPL 语境下的「聚合分发 (mere aggregation)」。因此各二进制各自保留原许可证，不传染到主程序代码。

## 内置引擎清单

| 引擎 | 版本 | 用途 | 许可证 | 来源 |
| --- | --- | --- | --- | --- |
| FFmpeg（含 ffprobe） | 9.0.1-essentials（gyan.dev 静态构建） | 复杂变换、元数据读取 | GPL v3（`--enable-gpl` 静态构建） | https://www.gyan.dev/ffmpeg/builds/ · 源码 https://github.com/FFmpeg/FFmpeg/commit/bf1b838f2a |
| Gifsicle | 1.95 (win64) | GIF 优化、裁剪、90° 旋转、翻转、延迟调整 | GPL-2.0 | http://eternallybored.org/misc/gifsicle/ |
| ImageMagick | 7.1.2-29 (portable Q16 x64) | 任意角度旋转、画布扩展、色彩处理 | ImageMagick License（基于 Apache-2.0，兼容 GPL v3） | https://imagemagick.org/ |
| gifski | 1.7.1（CLI，取自 npm `gifski` 包） | 高质量 GIF 重编码 | AGPL-3.0 | https://github.com/ImageOptim/gifski |
| libvips | 8.18.5 (build-win64-mxe, web 变体) | 高性能图像处理（Frames Converter） | LGPL-2.1 | https://github.com/libvips/build-win64-mxe |

## 许可证文本位置

各引擎的 LICENSE / 版权声明随附于本仓库 `third_party/` 目录：

- `third_party/ffmpeg-LICENSE.txt`（GPL v3）与 `third_party/ffmpeg-README.txt`（构建配置）
- `third_party/gifsicle-GPLv2.txt`（GPL-2.0）
- `third_party/ImageMagick-LICENSE.txt` 与 `third_party/ImageMagick-NOTICE.txt`（ImageMagick License）
- `third_party/gifski-AGPLv3.txt`（AGPL-3.0）与 `third_party/gifski-README.md`
- `third_party/libvips-LICENSE.txt`（LGPL-2.1）

## 合规说明

1. **FFmpeg**：所分发构建为 gyan.dev 的 `release-essentials` GPL 静态构建，完整编译配置（含 `--enable-gpl` 等开关）见 `third_party/ffmpeg-README.txt`。
2. **gifski（AGPL-3.0）**：五个引擎中要求最严格者。独立子进程调用不构成衍生作品，但分发时必须提供对应源码。源码可在 https://github.com/ImageOptim/gifski 获取（本仓库所分发二进制为 v1.7.1 CLI，对应源码见该版本 tag）。
3. **ImageMagick**：使用其自有许可（基于 Apache-2.0），分发时需随附 LICENSE / NOTICE 并保留对 ImageMagick Studio LLC 的署名。
4. 各引擎二进制在运行时解压到临时目录后以独立子进程调用，不链接进主程序。

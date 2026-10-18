#!/usr/bin/env bash
set -euo pipefail
# 在 macOS 上安装五个引擎，连同其动态依赖平铺到 <DEST> 并生成 manifest.txt。
# 用法: fetch-engines-macos.sh <ARCH> <DEST>    ARCH ∈ {x86_64, aarch64}
#
# 用 Homebrew 安装引擎，再把可执行文件与其 dylib 依赖递归平铺到目标目录，运行时靠
# ProcessRunner 注入的 DYLD_LIBRARY_PATH 定位。macOS 的动态库打包对 Homebrew 版本/路径
# 较敏感，首次 CI 若失败，优先用 `otool -L` 检查依赖路径是否被正确复制。

ARCH="${1:?usage: fetch-engines-macos.sh <ARCH> <DEST>}"
DEST="${2:?usage: fetch-engines-macos.sh <ARCH> <DEST>}"

mkdir -p "$DEST"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

# Homebrew 统一安装（ffmpeg 自带 drawtext/libfreetype，满足计时器叠加）
brew install ffmpeg gifsicle imagemagick gifski vips
PREFIX="$(brew --prefix)"

# 递归复制二进制及其非系统 dylib 依赖到 DEST
copy_with_dylibs() {
  local bin="$1"
  cp "$bin" "$DEST/$(basename "$bin")"
  local seen="$STAGE/seen.dylibs"; : > "$seen"
  walk() {
    local lib="$1"
    case "$lib" in
      /usr/lib/*|/System/*) return ;;
    esac
    [ -f "$lib" ] || return
    grep -qxF "$lib" "$seen" && return
    echo "$lib" >> "$seen"
    cp "$lib" "$DEST/$(basename "$lib")"
    otool -L "$lib" 2>/dev/null | awk '/^\t/ {print $1}' | while read -r dep; do walk "$dep"; done
  }
  otool -L "$bin" 2>/dev/null | awk '/^\t/ {print $1}' | while read -r dep; do walk "$dep"; done
}

copy_with_dylibs "$PREFIX/bin/ffmpeg"
copy_with_dylibs "$PREFIX/bin/ffprobe"
copy_with_dylibs "$PREFIX/bin/gifsicle"
copy_with_dylibs "$PREFIX/bin/magick"
copy_with_dylibs "$PREFIX/bin/gifski"
copy_with_dylibs "$PREFIX/bin/vips"

# ImageMagick 配置（*.xml + sRGB.icc）
IM_CONF="$(find "$PREFIX" -type d -name 'ImageMagick-*' 2>/dev/null | head -1)"
if [ -n "$IM_CONF" ]; then
  find "$IM_CONF" -type f \( -name '*.xml' -o -name '*.icc' \) -exec cp {} "$DEST/" \;
fi

# 生成 manifest.txt
( cd "$DEST" && ls -1 | grep -v '^manifest.txt$' | sort > manifest.txt )
echo "wrote $(wc -l < "$DEST/manifest.txt") entries -> $DEST"

#!/usr/bin/env bash
set -euo pipefail
# 在 macOS 上安装五个引擎，连同其动态依赖平铺到 <DEST> 并生成 manifest.txt。
# 用法: fetch-engines-macos.sh <ARCH> <DEST>    ARCH ∈ {x86_64, aarch64}
#
# 用 Homebrew 安装引擎，再把可执行文件与其 dylib 依赖递归平铺到目标目录，运行时靠
# ProcessRunner 注入的 DYLD_LIBRARY_PATH 定位。macOS 的动态库打包对 Homebrew 版本/路径
# 较敏感，首次 CI 若失败，优先用 `otool -L` 检查依赖路径是否被正确复制。
#
# 注：ffmpeg/ffprobe 等共享同一批 dylib，且 Homebrew 的 dylib 为只读(0444)。普通 `cp`
# 会把只读权限带到目标，导致下一次覆盖同名的只读文件时报 Permission denied，故统一走
# cp_overwrite（先 rm 再 cp），并在全局层面去重。

ARCH="${1:?usage: fetch-engines-macos.sh <ARCH> <DEST>}"
DEST="${2:?usage: fetch-engines-macos.sh <ARCH> <DEST>}"

mkdir -p "$DEST"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

# Homebrew 统一安装（ffmpeg 自带 drawtext/libfreetype，满足计时器叠加）
brew install ffmpeg gifsicle imagemagick gifski vips
PREFIX="$(brew --prefix)"

SEEN="$STAGE/seen.dylibs"
: > "$SEEN"
EXEDIR=""

# 覆盖式复制：目标可能是上次已复制的只读 dylib，先 rm 再 cp，并保证可写
cp_overwrite() {
  local src="$1" dst="$2"
  rm -f "$dst"
  cp "$src" "$dst"
  chmod u+w "$dst" 2>/dev/null || true
}

# 递归复制一条依赖（$1=依赖名，$2=引用它的文件），全局去重
walk() {
  local dep="$1" ref="$2" lib
  case "$dep" in
    /usr/lib/*|/System/*) return ;;
  esac
  lib="$dep"
  # 把 @rpath/@loader_path/@executable_path/裸相对名 解析为真实绝对路径
  if [ ! -f "$lib" ]; then
    case "$dep" in
      @loader_path/*)      lib="${ref%/*}/${dep#@loader_path/}" ;;
      @executable_path/*)  lib="$EXEDIR/${dep#@executable_path/}" ;;
      @rpath/*)            lib="$(find -L "$PREFIX/Cellar" -name "$(basename "$dep")" 2>/dev/null | head -1)" ;;
      *)                   lib="${ref%/*}/$dep" ;;
    esac
  fi
  [ -f "$lib" ] || return
  grep -qxF "$lib" "$SEEN" && return
  echo "$lib" >> "$SEEN"
  cp_overwrite "$lib" "$DEST/$(basename "$lib")"
  otool -L "$lib" 2>/dev/null | awk '/^\t/ {print $1}' | while read -r d; do walk "$d" "$lib"; done
}

copy_with_dylibs() {
  local bin="$1"
  EXEDIR="$(dirname "$bin")"
  cp_overwrite "$bin" "$DEST/$(basename "$bin")"
  otool -L "$bin" 2>/dev/null | awk '/^\t/ {print $1}' | while read -r d; do walk "$d" "$bin"; done
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

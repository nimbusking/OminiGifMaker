#!/usr/bin/env bash
set -euo pipefail
# 在 Linux 上拉取五个引擎，连同依赖平铺到 <DEST> 并生成 manifest.txt。
# 用法: fetch-engines-linux.sh <ARCH> <DEST>    ARCH ∈ {x86_64, aarch64}
#
# 运行时依赖：curl / tar / npm / apt-get（脚本内部用 sudo 安装 gifsicle、libvips-tools）。

ARCH="${1:?usage: fetch-engines-linux.sh <ARCH> <DEST>}"
DEST="${2:?usage: fetch-engines-linux.sh <ARCH> <DEST>}"

mkdir -p "$DEST"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

# 1) FFmpeg + ffprobe：johnvansickle 静态构建（GPL，含 drawtext/libfreetype/fontconfig）
case "$ARCH" in
  x86_64)  FF_TAR="ffmpeg-release-amd64-static.tar.xz" ;;
  aarch64) FF_TAR="ffmpeg-release-arm64-static.tar.xz" ;;
  *) echo "unsupported arch: $ARCH" >&2; exit 1 ;;
esac
curl -fsSL "https://johnvansickle.com/ffmpeg/releases/${FF_TAR}" -o "$STAGE/ffmpeg.tar.xz"
tar -xf "$STAGE/ffmpeg.tar.xz" -C "$STAGE"
FF_DIR="$(find "$STAGE" -mindepth 1 -maxdepth 1 -type d -name 'ffmpeg-*' | head -1)"
cp "$FF_DIR/ffmpeg"  "$DEST/ffmpeg"
cp "$FF_DIR/ffprobe" "$DEST/ffprobe"

# 2) Gifsicle：单文件、无外部依赖
sudo apt-get update -qq && sudo apt-get install -y -qq gifsicle
cp "$(command -v gifsicle)" "$DEST/gifsicle"

# 3) ImageMagick 7（magick）：官方 Linux 仅提供 AppImage（GitHub release），解包出 magick + 依赖 + 配置
if [ "$ARCH" != "x86_64" ]; then
  echo "ImageMagick AppImage 仅提供 x86_64；${ARCH} 需源码静态编译" >&2
  exit 1
fi
IM_VER="7.1.2-34"
curl -fsSL "https://github.com/ImageMagick/ImageMagick/releases/download/${IM_VER}/ImageMagick-${IM_VER}-gcc-x86_64.AppImage" \
  -o "$STAGE/magick.AppImage"
chmod +x "$STAGE/magick.AppImage"
( cd "$STAGE" && ./magick.AppImage --appimage-extract >/dev/null )
SR="$(find "$STAGE" -maxdepth 1 -type d -name 'squashfs-root*' | head -1)"
MAGICK_BIN="$(find "$SR" -type f -name magick | head -1)"
[ -n "$MAGICK_BIN" ] || { echo "magick binary not found in AppImage" >&2; exit 1; }
cp "$MAGICK_BIN" "$DEST/magick"
# AppImage 自带全部委托库（除 libc），全部平铺；跳过关键系统库，避免 LD_LIBRARY_PATH 误覆盖
find "$SR" -type f -name '*.so*' | while read -r so; do
  case "$(basename "$so")" in
    libc.so*|libm.so*|libpthread.so*|libdl.so*|ld-linux*.so*) ;;
    *) cp "$so" "$DEST/" ;;
  esac
done
# 配置 *.xml + sRGB.icc
find "$SR" -type f \( -name '*.xml' -o -name '*.icc' \) -exec cp {} "$DEST/" \;

# 4) gifski：npm gifski 包（与 Windows 同源，v1.7.1）
npm pack gifski@1.7.1 --pack-destination "$STAGE" >/dev/null
tar -xzf "$STAGE"/gifski-*.tgz -C "$STAGE"
cp "$(find "$STAGE/package" -type f -name gifski | head -1)" "$DEST/gifski"

# 5) libvips（vips）：libvips-tools + 平铺其全部动态依赖（LD_LIBRARY_PATH 运行时定位）
sudo apt-get install -y -qq libvips-tools
cp "$(command -v vips)" "$DEST/vips"
ldd "$DEST/vips" | awk '/=> \// {print $3}' | sort -u | while read -r so; do
  [ -f "$so" ] && cp "$so" "$DEST/"
done

# 生成 manifest.txt
( cd "$DEST" && ls -1 | grep -v '^manifest.txt$' | sort > manifest.txt )
echo "wrote $(wc -l < "$DEST/manifest.txt") entries -> $DEST"

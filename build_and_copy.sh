#!/usr/bin/env bash
# ============================================================
#  AutoAngler — Build & Copy (Linux)
#  Pengganti build_and_copy.cmd era Windows.
#  Build APK debug lalu salin ke apk/ sesuai konvensi
#  repo (lihat ANDROID/CLAUDE.md): <namapaket>_<yyyymmdd>.apk,
#  simpan hanya APK terbaru per app.
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/Android/jdk-17}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "========================================"
echo " AutoAngler - Build & Copy"
echo "========================================"

echo
echo "[1/2] Build APK debug..."
./gradlew assembleDebug

SRC="app/build/outputs/apk/debug/app-debug.apk"
DST_DIR="apk"
DST="$DST_DIR/autoangler_$(date +%Y%m%d).apk"

if [ ! -f "$SRC" ]; then
    echo "BUILD GAGAL / APK tidak ditemukan: $SRC" >&2
    exit 1
fi

echo
echo "[2/2] Salin APK ke $DST ..."
mkdir -p "$DST_DIR"
rm -f "$DST_DIR"/autoangler_*.apk "$DST_DIR"/fishing_*.apk   # buang APK lama (termasuk nama lama)
cp "$SRC" "$DST"

echo
echo "========================================"
echo " SELESAI: $DST"
echo "========================================"

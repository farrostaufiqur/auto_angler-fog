# CLAUDE.md — AutoAngler

Bot overlay Android (`tf.fog.autoangler`, label "AutoAngler") untuk auto-mancing mini-game
di sebuah game. Screen capture (MediaProjection) + vision HSV → tap tombol panah lewat
**Root** (`su` / `input tap`) atau **Non-Root** (AccessibilityService). Dulu bernama
`nte-fishing-bot` / "Fishing Bot" — semua referensi "NTE" sudah dihapus. Detail arsitektur
& tuning: `CONTEXT_FOR_CLAUDE.md`. Rilis & Play Store: `PLAY_STORE.md`.

## Aturan build

Aturan rilis global (AAB wajib, salin APK+AAB ke `ANDROID/apk/` dgn nama sama, build
ulang tiap perubahan) ada di `/home/albayan/CLAUDE.md` bagian ANDROID/. Khusus app ini:

- **Dev / uji cepat:** `./build_and_copy.sh` → APK debug ke `ANDROID/apk/`.
- **Rilis:** `./build_release.sh` → `autoangler_<yyyymmdd>.apk` + `autoangler_<yyyymmdd>.aab`
  (skrip sudah menyalin keduanya & menyapu versi lama). Varian rooted set
  `ARTIFACT_BASE=autoangler-rooted`.
- Signing: `keystore.properties` + `autoangler-release.keystore` di root proyek
  (di-gitignore). Backup wajib.
- Toolchain: `JAVA_HOME=~/Android/jdk-17`, `ANDROID_SDK_ROOT=~/Android/Sdk`
  (di-default di skrip). AGP 8.5.2 / Kotlin 1.9.24, minSdk 26, target/compile 35.

## Varian

Kedua varian **`applicationId` sama** (`tf.fog.autoangler`) + **keystore sama**, jadi APK
satu bisa dipasang menimpa yang lain — pola "kalau satu gagal, coba yang root-only".
Keduanya TIDAK bisa terpasang bersamaan; `versionCode` sama-sama 1 (reinstall).

- **`autoangler/`** (ini) — dua mode. Mode dipilih **hanya di layar utama APK**
  (RadioGroup Root / Non-Root), bukan di HUD. Pilih Root + START → cek root; kalau gagal
  muncul dialog tawaran pindah ke Non-Root. Label "AutoAngler".
- **`autoangler-root-only/`** — salinan **root-only**: tak ada AccessibilityService sama
  sekali, tak ada pemilih mode. Cek root langsung saat buka app; kalau gagal, tombol START
  di-grayout. Label "AutoAngler (Rooted)", artefak `autoangler-rooted_<yyyymmdd>.{apk,aab}`.

## Catatan

- **UI text aplikasi ini = BAHASA INGGRIS saja** (permintaan user, meng-override konvensi
  repo). Komentar/log kode: campuran (belum diterjemahkan seluruhnya); tulisan baru → EN.
- Nama internal (`fishing_bot_prefs`, `fishing_bot_channel`, class `FishingAccessibilityService`)
  sengaja TIDAK diubah — ganti = data lisensi/config di perangkat hilang.
- **Registrasi = verifikasi web** (`License.kt`): GET `https://fog.tf/api/autoangler/<kode>?device_id=<ANDROID_ID>`,
  simpan kode lokal kalau `valid`. `isRegistered()` cek lokal saja (offline aman);
  `refresh()` di MainActivity hapus kode lokal hanya kalau server balas `valid:false`.
  Butuh permission `INTERNET`. API + admin kode/device: `WEBSITE/fog/autoangler/`
  (`fog.tf/api/autoangler`, login bersama fog.tf). Dev bagi kode via Telegram `@fog2x`.

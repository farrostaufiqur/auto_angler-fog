# AutoAngler — Rilis & Play Store

## Status build rilis (sudah disiapkan)

| Item | Nilai |
|---|---|
| `applicationId` | `tf.fog.autoangler` |
| `versionCode` / `versionName` | `1` / `1.0` |
| `minSdk` / `targetSdk` / `compileSdk` | 26 / 35 / 35 |
| Signing | keystore RSA-4096, alias `autoangler`, valid ~27 th (`autoangler-release.keystore`) |
| R8 / shrink resources | aktif (`isMinifyEnabled` + `isShrinkResources = true`) |
| Output AAB | `app/build/outputs/bundle/release/app-release.aab` (~2,2 MB) — **ini yang di-upload** |
| Output APK | `app/build/outputs/apk/release/app-release.apk` (~1,5 MB) — sideload/uji, disalin ke `apk/autoangler_<tgl>.apk` |

Build ulang: `./build_release.sh`

### Kredensial keystore

Ada di `keystore.properties` (root proyek, **di-gitignore**) + file `autoangler-release.keystore`.
**Wajib di-backup di tempat aman** — kalau hilang, tidak bisa update app yang sama di Play
(kecuali pakai Play App Signing + minta reset upload key ke Google).

- SHA-256 sertifikat: `3D:AE:02:22:78:55:6A:4D:77:41:B0:97:D6:77:C1:75:AC:A7:8D:8A:20:E8:09:70:04:E6:FE:F2:AC:0B:36:3E`

---

## ⚠️ Peringatan kelayakan Play Store

Secara teknis AAB-nya **siap di-upload**, tapi **kemungkinan lolos review sangat kecil**
dalam bentuk sekarang. Alasan (berurut dari yang paling mematikan):

1. **Penyalahgunaan Accessibility API.** Kebijakan Play: `AccessibilityService` hanya untuk
   membantu pengguna disabilitas. Dipakai untuk auto-tap / otomasi game = pelanggaran
   langsung. Saat submit ada form deklarasi "Accessibility API" wajib; app yang fungsinya
   bot game hampir pasti ditolak. Seluruh mode Non-Root bergantung pada ini.
2. **Device and Network Abuse** — otomasi interaksi / bot gameplay termasuk yang dilarang.
3. **Memfasilitasi cheating** — mengotomasi mini-game untuk farming reward umumnya melanggar
   ToS game target; makin berat kalau game-nya online/kompetitif.
4. **`SYSTEM_ALERT_WINDOW` + `MediaProjection`** — diizinkan, tapi diperiksa ketat dan wajib
   ada privacy policy, Data safety form, serta justifikasi `foregroundServiceType`
   (`dataSync` sekarang dibatasi). Digabung poin 1–3 = banyak bendera merah.
5. **Unlock berbayar via kode registrasi di luar Google Play Billing** — kalau dianggap
   "membuka fitur", bisa kena kebijakan pembayaran.

**Mode Root-only pun tidak menyelamatkan** — poin 2–3 (bot game) berlaku apa pun cara
pengirimannya. Distribusi realistis = sideload APK dari situs sendiri / store pihak ketiga.

---

## Kalau tetap mau coba submit ke Play Console

### Sebelum upload
- [ ] Buat akun Play Console (biaya pendaftaran sekali $25).
- [ ] Aktifkan **Play App Signing** (default untuk app baru) — upload pakai
      `autoangler-release.keystore` sebagai *upload key*.
- [ ] Siapkan **Privacy Policy** publik (URL) — wajib karena `MediaProjection` (screen
      capture) + Accessibility. Jelaskan: data layar diproses **lokal di perangkat**, tidak
      dikirim ke server, tidak ada analytics.
- [ ] Isi **Data safety form**: tidak ada data dikumpulkan/dibagikan (kalau memang begitu).
- [ ] **Content rating questionnaire** (IARC).
- [ ] **Target audience**: dewasa (bukan anak-anak).

### Deklarasi permission (Play Console → App content)
- [ ] **Accessibility API** — jelaskan fungsi. (Ini titik gagal paling mungkin.)
- [ ] **All files / sensitive** — tidak dipakai.
- [ ] **Foreground service** (`mediaProjection`, `dataSync`) — jelaskan tiap tipe + kenapa
      butuh berjalan di foreground.
- [ ] **Overlay (`SYSTEM_ALERT_WINDOW`)** — jelaskan HUD melayang di atas game.

### Teknis / kepatuhan
- [ ] Target API level: Play untuk submission baru bisa menuntut **API 35/36**. Sekarang 35.
      Kalau ditolak "target API too low", naikkan `compileSdk`/`targetSdk` (butuh
      `platforms;android-36` + `build-tools` 36 + upgrade AGP ke ≥ 8.6).
- [ ] Uji di perangkat fisik (Android 12–15), Root & Non-Root.
- [ ] Screenshot + ikon 512×512 + feature graphic 1024×500 untuk listing.
- [ ] Isi rilis di jalur **Internal testing** dulu, jangan langsung Production.

### Kalau ditolak
Jangan banding berulang dengan konten sama. Opsi:
- Reposisikan app (bukan "bot game") — sulit karena fungsinya memang itu.
- Distribusi di luar Play (situs sendiri, APKPure, F-Droid tak cocok karena bukan FOSS murni).

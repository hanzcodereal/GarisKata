# Garis Kata

Game tebak kata Android berbahasa Indonesia. Setiap stage menampilkan satu kalimat dengan satu kata yang hilang. Tebak hurufnya satu per satu sebelum enam kesempatan habis.

Pengembang: Hanz Dev Official
Kontak: hanzzcodee@gmail.com
Dukung: https://saweria.co/hanzreally

## Cara main

- Tekan huruf di papan. Huruf yang ada di kata dilingkari biru, yang tidak ada dicoret merah.
- Enam salah berarti stage diulang.
- Bantuan:
  - Kaca (30 koin): membuka satu huruf.
  - Bom (50 koin): menyingkirkan lima huruf yang tidak ada di kata.
  - Lampu (gratis sekali per stage): menampilkan petunjuk.
- Selesai pertama kali memberi 20 koin, ditambah 10 koin bila tanpa salah. Mengulang stage lama tidak memberi koin.
- Keyboard fisik juga bisa dipakai untuk mengetik huruf.

## Build APK di GitHub (manual)

1. Unggah proyek ini ke repositori GitHub.
2. Buka tab Actions, pilih **Build APK Release**, tekan **Run workflow**.
3. Setelah selesai, unduh artifact `garis-kata-release` (berisi APK release yang sudah ditandatangani).

Workflow tidak jalan otomatis, hanya saat ditekan. Tipe build-nya `assembleRelease`.

### Tanda tangan

Tanpa secrets, workflow membuat keystore baru di setiap build. APK-nya sah dan bisa dipasang, tetapi tidak bisa menimpa versi dari build sebelumnya (Android menolak karena tanda tangannya beda). Untuk tanda tangan tetap, buat keystore sekali lalu isi secrets repositori:

| Secret | Isi |
|--------|-----|
| `KEYSTORE_BASE64` | Hasil `base64 -w0 garis-kata.jks` |
| `KEYSTORE_PASSWORD` | Kata sandi keystore |
| `KEY_ALIAS` | Alias kunci |
| `KEY_PASSWORD` | Kata sandi kunci |

Jangan pernah mengunggah berkas `.jks` ke repositori.

## Struktur

- `app/src/main/java/com/hanzdev/gariskata/game`: logika game murni, tanpa Android.
- `.../data`: soal, penyimpanan progres.
- `.../ui`: layar dan widget yang digambar dengan kode.
- `app/src/main/res/raw/stages.txt`: soal, format `kalimat|JAWABAN|petunjuk`.
- `tools/stages_sumber.py`: sumber soal, jalankan lalu `tools/cek_stage.py`.
- `tools/buat_suara.py`: pembuat efek suara.
- `DESIGN.md` dan `docs/ASSETS.md`: arah desain dan daftar aset.

## Menambah soal

Tambahkan baris di `tools/stages_sumber.py`, jalankan `python3 tools/stages_sumber.py`, lalu `python3 tools/cek_stage.py`. Jawaban harus 3 sampai 14 huruf A sampai Z tanpa spasi atau tanda hubung.

## Build lokal

Butuh JDK 17, Android SDK 35, dan Gradle 8.9. Atur `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, lalu `gradle :app:assembleRelease`.

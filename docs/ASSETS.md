# Daftar aset

Semua aset dibuat khusus untuk proyek ini. Tidak ada gambar hasil generator AI, tidak ada aset pihak ketiga.

| Aset | Berkas | Cara dibuat | Lisensi |
|------|--------|-------------|---------|
| Ikon aplikasi | `res/drawable/ic_launcher_*.xml` | Vector digambar tangan lewat kode | Milik pengembang |
| Ikon kaca, bom, lampu, koin | `ui/Widgets.kt` | Digambar dengan Canvas | Milik pengembang |
| Lingkaran, coretan, garis kertas | `ui/Ui.kt` | Dihitung dengan kode, goyangan dari seed | Milik pengembang |
| Efek suara (9 berkas WAV) | `res/raw/sfx_*.wav` | `tools/buat_suara.py`, sintesis gelombang sendiri | Milik pengembang |
| Soal | `res/raw/stages.txt` | Ditulis tangan, dibuat ulang lewat `tools/stages_sumber.py` | Milik pengembang |
| Font | Bawaan Android (`casual`, `sans-serif-medium`) | Tidak dibundel | Lisensi font sistem |

Pemeriksaan teknis:

- Level suara: puncak antara -3,1 dan -6,0 dBFS, RMS sekitar -13 sampai -17 dBFS (hasil `tools/buat_suara.py`).
- Soal: `python3 tools/cek_stage.py` memeriksa satu `___` per kalimat, jawaban hanya A sampai Z, tidak ada jawaban ganda, dan petunjuk tidak membocorkan jawaban.
- Pemeriksaan di perangkat nyata (skala ikon, keterbacaan font `casual` di berbagai merek) belum dilakukan.

Tidak ada konten yang dibuat saat game berjalan, jadi bagian "live-generated" pada Steam Content Survey tidak berlaku.

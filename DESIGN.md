# Arah desain Garis Kata

Dial: ENERGY 2 / RHYTHM 2 / MOTION 2

Dibaca sebagai: game puzzle kata Android untuk pemain kasual berbahasa Indonesia, bergaya buku tulis bergaris dengan coretan pensil.

## Alasan tiap keputusan

- Garis buku tulis di latar: nama game ini "Garis Kata", jadi garis adalah identitasnya, bukan hiasan.
- Font tulisan tangan bawaan Android (`casual`): cocok dengan buku tulis dan tidak butuh berkas font tambahan.
- Teks kecil memakai sans-serif: tulisan tangan di ukuran kecil sulit dibaca.
- Warna biru pensil: dipakai untuk huruf benar, lingkaran, dan jawaban. Rasio kontras 6,76:1 di atas kertas.
- Merah spidol (aksen tunggal): dipakai hanya untuk salah, kesempatan habis, dan garis judul. Kontras 5,47:1.
- Kuning stabilo: hanya untuk koin dan tombol utama, sebagai tanda "bagian yang bisa ditekan atau bernilai".
- Benar dan salah tidak hanya dibedakan warna: benar dilingkari, salah dicoret, dan ada teks "Salah 2 dari 6".
- Lingkaran dan coretan digambar bergoyang dengan seed tetap per huruf: bentuknya terasa digambar tangan tetapi stabil tiap dibuka.
- Satu radius sudut kecil (10 sampai 12 dp) di semua tombol dan kartu, tanpa bentuk pil.
- Tanpa bayangan, tanpa glow, tanpa gradasi: tombol hanya turun 2 dp saat ditekan agar terasa seperti tombol.
- Tanpa ikon pustaka: kaca, bom, dan lampu digambar langsung dengan kode karena tiga ikon itu langsung menunjukkan fungsinya.
- Gerak hanya untuk umpan balik: coretan lingkaran (260 ms), huruf membal (320 ms), guncangan salah (380 ms), hitung koin (600 ms). Semua berhenti bila "Kurangi gerakan" aktif atau skala animasi sistem nol.
- Tidak ada musik latar: belum ada musik yang layak, dan lebih baik kosong daripada asal.

## Palet

Netral: kertas `#F7F1E3`, tinta `#1E2430`, tinta lembut `#4A5160`.
Inti: biru pensil `#28558A`, kuning stabilo `#F4C542`.
Aksen: merah spidol `#B5301F`.
Garis kertas `#D8E3EF` dan garis margin `#E0A39A` hanya dekorasi. Garis kertas sengaja terang: merah spidol di atasnya 4,74:1, biru pensil 5,86:1, tinta lembut 6,13:1 (semua lolos AA, diukur dengan `contrast-check.py`).

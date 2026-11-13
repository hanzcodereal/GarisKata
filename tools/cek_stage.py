# Memeriksa stages.txt: satu "___", jawaban A-Z saja, tidak ada duplikat, petunjuk tidak membocorkan jawaban.
import pathlib, re, sys

path = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res/raw/stages.txt"
masalah, jawaban_terlihat = [], set()
baris = [b for b in path.read_text(encoding="utf-8").splitlines() if b.strip()]
for no, b in enumerate(baris, 1):
    bagian = b.split("|")
    if len(bagian) != 3:
        masalah.append(f"stage {no}: harus 3 kolom"); continue
    kalimat, jawab, petunjuk = bagian
    if kalimat.count("___") != 1: masalah.append(f"stage {no}: harus ada tepat satu ___")
    if not re.fullmatch(r"[A-Z]{3,14}", jawab): masalah.append(f"stage {no}: jawaban {jawab!r} tidak valid")
    if jawab in jawaban_terlihat: masalah.append(f"stage {no}: jawaban ganda {jawab}")
    jawaban_terlihat.add(jawab)
    if jawab.lower() in petunjuk.lower(): masalah.append(f"stage {no}: petunjuk membocorkan jawaban")
    if jawab.lower() in kalimat.lower(): masalah.append(f"stage {no}: kalimat memuat jawaban")
    if "\u2014" in b: masalah.append(f"stage {no}: ada tanda pisah panjang")
print(f"{len(baris)} stage diperiksa, {len(masalah)} masalah")
for m in masalah: print(" -", m)
sys.exit(1 if masalah else 0)

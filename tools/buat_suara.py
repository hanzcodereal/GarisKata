# Membuat efek suara game sebagai WAV mono 22,05 kHz. Tidak memakai sampel orang lain.
# Jalankan: python3 tools/buat_suara.py
import pathlib, wave
import numpy as np

SR = 22050
OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res/raw"
rng = np.random.default_rng(7)


def t(dur):
    return np.arange(int(SR * dur)) / SR


def amplop(n, serang=0.004, lepas=0.08):
    e = np.ones(n)
    a, r = max(1, int(SR * serang)), max(1, int(SR * lepas))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] = np.minimum(e[-r:], np.linspace(1, 0, r))
    return e


def nada(f, dur, lepas=0.08, harmonik=0.35):
    x = t(dur)
    s = np.sin(2 * np.pi * f * x) + harmonik * np.sin(2 * np.pi * 2 * f * x)
    return s * amplop(len(x), lepas=lepas) * np.exp(-x * 5)


def gesek(dur, kasar=1.0):
    # derau yang disaring sederhana, mirip goresan pensil
    n = int(SR * dur)
    d = rng.standard_normal(n)
    d = np.convolve(d, np.ones(int(2 + 4 * kasar)) / (2 + 4 * kasar), mode="same")
    return d * amplop(n, 0.002, 0.04) * np.exp(-t(dur) * 18)


def susun(*bagian):
    panjang = max(int(SR * (mulai)) + len(x) for mulai, x in bagian)
    y = np.zeros(panjang)
    for mulai, x in bagian:
        i = int(SR * mulai)
        y[i:i + len(x)] += x
    return y


def simpan(nama, y, puncak=0.7):
    y = y / (np.max(np.abs(y)) + 1e-9) * puncak
    data = (y * 32767).astype("<i2")
    with wave.open(str(OUT / f"{nama}.wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(data.tobytes())
    rms = 20 * np.log10(np.sqrt(np.mean(y ** 2)) + 1e-9)
    print(f"{nama}: {len(y) / SR:.2f} dtk, puncak {20 * np.log10(puncak):.1f} dBFS, rms {rms:.1f} dBFS")


simpan("sfx_tap", susun((0, gesek(0.05, 0.6) * 0.8), (0, nada(900, 0.05, 0.03) * 0.5)), 0.5)
simpan("sfx_benar", susun((0, nada(659, 0.18)), (0.09, nada(880, 0.26, 0.15))))
simpan("sfx_salah", susun((0, gesek(0.16, 2.5) * 0.9), (0, nada(160, 0.24, 0.12, 0.6))))
simpan("sfx_menang", susun(*[(i * 0.11, nada(f, 0.32, 0.2)) for i, f in enumerate([523, 659, 784, 1047])]))
simpan("sfx_kalah", susun(*[(i * 0.2, nada(f, 0.4, 0.2, 0.2)) for i, f in enumerate([392, 330, 262])]))
simpan("sfx_koin", susun((0, nada(1319, 0.1, 0.05, 0.2)), (0.07, nada(1760, 0.22, 0.14, 0.2))), 0.55)
simpan("sfx_bom", susun((0, gesek(0.35, 6) * 1.2), (0, nada(90, 0.35, 0.2, 0.8))))
simpan("sfx_ungkap", susun((0, nada(740, 0.12, 0.06)), (0.06, nada(988, 0.12, 0.06)), (0.12, nada(1245, 0.26, 0.16))), 0.6)
simpan("sfx_petunjuk", susun((0, nada(523, 0.3, 0.2, 0.2)), (0.1, nada(784, 0.4, 0.28, 0.2))), 0.55)

"""Synthesize a short gauss-rifle-like Φ-turret shot as OGG."""
from __future__ import annotations

import math
import struct
import subprocess
import wave
from pathlib import Path

import imageio_ffmpeg
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "sounds"
ART = ROOT / "art" / "turrets" / "audio"
OUT_DIR.mkdir(parents=True, exist_ok=True)
ART.mkdir(parents=True, exist_ok=True)

SR = 44100
DUR = 0.55


def synth() -> np.ndarray:
    n = int(SR * DUR)
    t = np.linspace(0.0, DUR, n, endpoint=False)
    # Rising coil charge (0–80ms)
    charge = np.zeros(n)
    charge_n = int(0.08 * SR)
    tc = t[:charge_n]
    charge[:charge_n] = 0.22 * np.sin(2 * math.pi * (900 + 4200 * (tc / 0.08)) * tc) * (tc / 0.08)
    # Hypervelocity crack (impulse + noise burst)
    crack = np.zeros(n)
    crack_start = charge_n
    crack_len = int(0.045 * SR)
    noise = np.random.default_rng(7).normal(0, 1, crack_len)
    env = np.exp(-np.linspace(0, 18, crack_len))
    crack[crack_start : crack_start + crack_len] = 0.85 * noise * env
    # Low thump body
    thump = 0.55 * np.sin(2 * math.pi * 55 * t) * np.exp(-t * 14)
    thump += 0.25 * np.sin(2 * math.pi * 110 * t) * np.exp(-t * 18)
    # Metallic ring tail
    ring = 0.12 * np.sin(2 * math.pi * 1850 * t) * np.exp(-t * 9)
    ring += 0.08 * np.sin(2 * math.pi * 3200 * t) * np.exp(-t * 16)
    # Soft whoosh
    whoosh = 0.1 * noise_like(n, 3) * np.exp(-np.maximum(0, t - 0.07) * 8)
    whoosh[:crack_start] *= 0.2

    mix = charge + crack + thump + ring + whoosh
    # Soft clip / normalize
    peak = np.max(np.abs(mix)) + 1e-9
    mix = 0.92 * mix / peak
    return mix.astype(np.float32)


def noise_like(n: int, seed: int) -> np.ndarray:
    return np.random.default_rng(seed).normal(0, 1, n).astype(np.float32)


def write_wav(path: Path, samples: np.ndarray) -> None:
    pcm = np.clip(samples, -1, 1)
    ints = (pcm * 32767.0).astype(np.int16)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(ints.tobytes())


def main() -> None:
    samples = synth()
    wav = ART / "kinetic_gauss_shot.wav"
    ogg = OUT_DIR / "kinetic_gauss_shot.ogg"
    write_wav(wav, samples)
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    subprocess.run(
        [ffmpeg, "-y", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "6", str(ogg)],
        check=True,
        capture_output=True,
    )
    print("wrote", ogg)


if __name__ == "__main__":
    main()

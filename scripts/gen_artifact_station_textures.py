"""Shim — run bake_artifact_station_textures.py (concept sheet -> game PNGs)."""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

if __name__ == "__main__":
    bake = Path(__file__).resolve().parent / "bake_artifact_station_textures.py"
    raise SystemExit(subprocess.call([sys.executable, str(bake)]))

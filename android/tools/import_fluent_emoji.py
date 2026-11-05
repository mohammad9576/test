#!/usr/bin/env python3
"""Bundles Microsoft Fluent Emoji (3D style, MIT licence, https://github.com/microsoft/fluentui-emoji) for every emoji
used in the app, so pictures look the same and much richer on every phone than the system emoji font.

Usage: python3 tools/import_fluent_emoji.py <fluentui-emoji checkout> <app/src/main/assets> <emoji→folder map.json>
Output: assets/emoji3d/<codepoints>.webp (e.g. 1f43c.webp for 🐼), 144 px.
"""
import json
import pathlib
import sys

from PIL import Image


def key(e: str) -> str:
    return "_".join(f"{ord(c):x}" for c in e if c not in "️‍")


def main(fluent: str, assets: str, mapping: str) -> None:
    base = pathlib.Path(fluent) / "assets"
    out = pathlib.Path(assets) / "emoji3d"
    out.mkdir(parents=True, exist_ok=True)
    m = json.load(open(mapping, encoding="utf-8"))
    n = 0
    for emoji, folder in m.items():
        pngs = sorted((base / folder).glob("3D/*.png")) or sorted((base / folder).glob("Default/3D/*.png"))
        if not pngs:
            continue
        im = Image.open(pngs[0]).convert("RGBA")
        im.thumbnail((144, 144))
        im.save(out / f"{key(emoji)}.webp", "WEBP", quality=82, method=6)
        n += 1
    (out / "LICENSE.txt").write_text("Fluent Emoji by Microsoft – MIT License – https://github.com/microsoft/fluentui-emoji\n")
    print(f"{n} emoji images")


if __name__ == "__main__":
    main(*sys.argv[1:4])

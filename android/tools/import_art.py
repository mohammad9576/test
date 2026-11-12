#!/usr/bin/env python3
"""
Imports the artist / Gemini asset pack into android/app/src/main/assets/art/.

    python3 -I android/tools/import_art.py <folder-or-zip> [--manifest docs/design/handoff_v5/asset_manifest.csv]

For every file listed in the manifest it:
  * finds it in the pack by its path (or by its bare file name),
  * removes a flat chroma background (#00FF00 or #FF00FF) when the image has no transparency,
  * fits it to the manifest size (keeps proportions, centres on a transparent canvas),
  * saves it as an optimised PNG at assets/art/<path>.
Prints what is missing, what is extra and what had problems, so a pack can be checked before the build.
"""
import csv
import io
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "art")
DEFAULT_MANIFEST = os.path.join(os.path.dirname(ROOT), "docs", "design", "handoff_v5", "asset_manifest.csv")
KEYS = [(0, 255, 0), (255, 0, 255)]


def read_pack(src):
    files = {}
    if zipfile.is_zipfile(src):
        with zipfile.ZipFile(src) as z:
            for n in z.namelist():
                if n.lower().endswith((".png", ".webp", ".jpg", ".jpeg")) and not n.startswith("__MACOSX"):
                    files[n.replace("\\", "/")] = z.read(n)
    else:
        for d, _, fs in os.walk(src):
            for f in fs:
                if f.lower().endswith((".png", ".webp", ".jpg", ".jpeg")):
                    p = os.path.join(d, f)
                    files[os.path.relpath(p, src).replace("\\", "/")] = open(p, "rb").read()
    return files


def find(files, path):
    stem = os.path.splitext(path)[0]
    for name in files:
        if os.path.splitext(name)[0].endswith(stem):
            return name
    base = os.path.basename(stem)
    folder = path.split("/")[0]
    for name in files:
        if os.path.splitext(os.path.basename(name))[0] == base and folder in name:
            return name
    return None


def has_alpha(img):
    return img.mode == "RGBA" and img.getchannel("A").getextrema()[0] < 250


def remove_key(img):
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    corners = [px[0, 0][:3], px[w - 1, 0][:3], px[0, h - 1][:3], px[w - 1, h - 1][:3]]
    key = min(KEYS, key=lambda k: sum(sum(abs(a - b) for a, b in zip(c, k)) for c in corners))
    if sum(sum(abs(a - b) for a, b in zip(c, key)) for c in corners) > 4 * 120:
        return img, False
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            d = abs(r - key[0]) + abs(g - key[1]) + abs(b - key[2])
            if d < 90:
                px[x, y] = (r, g, b, 0)
            elif d < 200:
                # soft edge, and pull the key colour out of the fringe
                alpha = int(255 * (d - 90) / 110)
                if key == (0, 255, 0):
                    g = min(g, max(r, b))
                else:
                    r, b = min(r, g), min(b, g)
                px[x, y] = (r, g, b, min(a, alpha))
    return img, True


def fit(img, size, transparent):
    tw, th = size
    img = img.convert("RGBA")
    if not transparent:
        # full-bleed backgrounds: cover and crop
        s = max(tw / img.width, th / img.height)
        img = img.resize((round(img.width * s), round(img.height * s)), Image.LANCZOS)
        l, t = (img.width - tw) // 2, (img.height - th) // 2
        return img.crop((l, t, l + tw, t + th))
    s = min(tw / img.width, th / img.height)
    img = img.resize((max(1, round(img.width * s)), max(1, round(img.height * s))), Image.LANCZOS)
    canvas = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    canvas.paste(img, ((tw - img.width) // 2, (th - img.height) // 2), img)
    return canvas


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    manifest = DEFAULT_MANIFEST
    if "--manifest" in sys.argv:
        manifest = sys.argv[sys.argv.index("--manifest") + 1]
    files = read_pack(sys.argv[1])
    used, missing, problems, done = set(), [], [], 0
    for row in csv.DictReader(open(manifest, encoding="utf-8")):
        path, (w, h), bg = row["path"], map(int, row["size"].split("x")), row["background"]
        name = find(files, path)
        if not name:
            missing.append(path)
            continue
        used.add(name)
        img = Image.open(io.BytesIO(files[name]))
        transparent = bg == "alpha"
        if transparent and not has_alpha(img):
            img, ok = remove_key(img)
            if not ok:
                problems.append(f"{path}: no transparency and no flat #00FF00/#FF00FF background")
        out = os.path.join(OUT, path)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        fit(img, (w, h), transparent).save(out, optimize=True)
        done += 1
    print(f"imported {done}")
    if missing:
        print(f"missing {len(missing)}:\n  " + "\n  ".join(missing))
    extra = sorted(set(files) - used)
    if extra:
        print(f"not in the manifest (ignored) {len(extra)}:\n  " + "\n  ".join(extra))
    if problems:
        print("problems:\n  " + "\n  ".join(problems))


if __name__ == "__main__":
    main()

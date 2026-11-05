#!/usr/bin/env python3
"""Imports the Persian (fa) stories of the Global Storybooks project into Kodomo's bundled book catalog.

Source texts: https://github.com/global-asp/global-asp (folder fa/), illustrations: https://github.com/global-asp/gsn-imagebank
Only stories under CC-BY (not CC-BY-NC) are imported; author, illustrator and translator are credited in the book.

Usage: python3 tools/import_global_storybooks.py <global-asp/fa dir> <gsn-imagebank dir> <android app/src/main/assets>
"""
import pathlib
import re
import sys

from PIL import Image

KEYWORDS = [
    ("ANIMALS", ["حیوان", "گربه", "سگ", "گاو", "بز", "مرغ", "خروس", "کروکودیل", "کرگدن", "پرنده", "فیل", "شیر", "میمون", "خرگوش", "کره خر", "کره-خر", "هزارپا", "ماهی", "عنکبوت", "آنانسی"]),
    ("SCIENCE", ["آب", "گیاه", "بدن", "رنگین", "دانه", "باران", "خورشید", "ماه", "آتش"]),
    ("SPORT", ["بازی", "دو", "توپ", "فوتبال"]),
    ("ART", ["رنگ", "نقاشی", "آشپزی"]),
]


def interests(title: str, text: str) -> str:
    found = ["BOOKS"]
    blob = title + " " + text
    for name, words in KEYWORDS:
        if any(w in blob for w in words):
            found.append(name)
    return ",".join(found)


def parse(md: str):
    title = re.search(r"^# (.+)$", md, re.M).group(1).strip()
    sections = [s.strip() for s in re.split(r"^##\s*$", md, flags=re.M)[1:]]
    meta = sections[-1]
    pages = [re.sub(r"\s+", " ", s) for s in sections[:-1] if s]
    info = dict(re.findall(r"^\* (\w+): (.+)$", meta, re.M))
    return title, pages, info


def main(fa_dir: str, img_dir: str, assets: str) -> None:
    fa = pathlib.Path(fa_dir)
    imgs = pathlib.Path(img_dir)
    out_img = pathlib.Path(assets) / "catalog" / "img"
    lines = ["# Global Storybooks (https://global-asp.github.io) – کتاب‌های واقعی با مجوز CC-BY؛ نام نویسنده، تصویرگر و مترجم در هر کتاب آمده است."]
    count = 0
    for md_file in sorted(fa.glob("[0-9]*.md")):
        sid = md_file.name[:4]
        title, pages, info = parse(md_file.read_text(encoding="utf-8"))
        if info.get("License", "").strip("[] ") != "CC-BY" or not pages:
            continue
        story_imgs = sorted((imgs / sid).glob("*.jpg"))
        if len(story_imgs) < 2:
            continue
        target = out_img / sid
        target.mkdir(parents=True, exist_ok=True)
        rendered = []
        for i, text in enumerate(pages):
            src = imgs / sid / f"{i + 2:02d}.jpg"  # 01 is the cover, page n uses image n+1
            if not src.exists():
                src = story_imgs[min(i + 1, len(story_imgs) - 1)]
            dst = target / f"{i + 1:02d}.jpg"
            if not dst.exists():
                im = Image.open(src).convert("RGB")
                im.thumbnail((420, 420))
                im.save(dst, "JPEG", quality=58, optimize=True)
            rendered.append(f"img:catalog/img/{sid}/{i + 1:02d}.jpg> {text.replace('>', '›')}")
        cover = target / "00.jpg"
        if not cover.exists():
            im = Image.open(story_imgs[0]).convert("RGB")
            im.thumbnail((360, 360))
            im.save(cover, "JPEG", quality=70, optimize=True)
        words = sum(len(p.split()) for p in pages)
        min_age, max_age = (3, 7) if words < 120 else (5, 9) if words < 350 else (7, 11)
        credit = f"نویسنده: {info.get('Text', '-')} • تصویرگر: {info.get('Illustration', '-')} • مترجم: {info.get('Translation', '-')}"
        desc = (pages[0][:110] + "…") if len(pages[0]) > 110 else pages[0]
        lines.append(f"@gs{sid} | {title} | کتاب‌های مصور جهان | 📗 | {min_age}-{max_age} | {interests(title, ' '.join(pages))} | {desc}")
        lines.append(f"credit:{credit} • مجوز CC-BY از Global Storybooks • cover:catalog/img/{sid}/00.jpg")
        lines.extend(rendered)
        count += 1
    (pathlib.Path(assets) / "catalog" / "books_global.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"imported {count} stories")


if __name__ == "__main__":
    main(*sys.argv[1:4])

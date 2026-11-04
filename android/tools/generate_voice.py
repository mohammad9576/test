#!/usr/bin/env python3
"""Records Persian narration clips for every phrase exported by VoicePhraseExportTest.

Usage: python3 tools/generate_voice.py app/build/voice/phrases.tsv app/src/main/assets/voice
Clips that already exist are kept, so the step is incremental (CI caches the output folder).
Voice: Microsoft Edge neural Persian voice (fa-IR-DilaraNeural) via the `edge-tts` package. Replace the generated files
with studio recordings using the same file names for a production release.
"""
import asyncio
import pathlib
import re
import sys

import edge_tts

VOICE = "fa-IR-DilaraNeural"
RATE = "-8%"
PITCH = "+12Hz"
CONCURRENCY = 6

EMOJI = re.compile(
    "[\U0001F000-\U0001FAFF\U00002600-\U000027BF\U0001F1E6-\U0001F1FF\U0000FE0F\U0000200D\U00002B50\U00002B55\U000023E9-\U000023FA]+"
)


def speakable(text: str) -> str:
    return re.sub(r"\s+", " ", EMOJI.sub(" ", text)).strip(" .،")


async def record(sem, key, text, out_dir, stats):
    target = out_dir / f"{key}.mp3"
    if target.exists() and target.stat().st_size > 0:
        stats["kept"] += 1
        return
    clean = speakable(text)
    if not clean:
        return
    async with sem:
        for attempt in range(3):
            try:
                await edge_tts.Communicate(clean, VOICE, rate=RATE, pitch=PITCH).save(str(target))
                stats["new"] += 1
                return
            except Exception as exc:  # network hiccups – retry with backoff
                if attempt == 2:
                    print(f"failed: {clean[:40]!r}: {exc}", file=sys.stderr)
                    stats["failed"] += 1
                    target.unlink(missing_ok=True)
                await asyncio.sleep(2 ** attempt)


async def main(tsv: str, out: str) -> None:
    out_dir = pathlib.Path(out)
    out_dir.mkdir(parents=True, exist_ok=True)
    rows = [line.split("\t", 1) for line in pathlib.Path(tsv).read_text(encoding="utf-8").splitlines() if "\t" in line]
    wanted = {key for key, _ in rows}
    for stale in out_dir.glob("*.mp3"):
        if stale.stem not in wanted:
            stale.unlink()
    stats = {"new": 0, "kept": 0, "failed": 0}
    sem = asyncio.Semaphore(CONCURRENCY)
    await asyncio.gather(*(record(sem, key, text, out_dir, stats) for key, text in rows))
    print(f"phrases={len(rows)} recorded={stats['new']} cached={stats['kept']} failed={stats['failed']}")


if __name__ == "__main__":
    asyncio.run(main(sys.argv[1], sys.argv[2]))

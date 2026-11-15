import sys
from PIL import Image
src, out = sys.argv[1], sys.argv[2]
spec = {"boy": ("boy_wave", 0.42), "girl": ("girl_wave", 0.42), "mother": ("mother_wave", 0.30), "father": ("father", 0.28),
        "teen_girl": ("teen_girl", 0.27), "teen_boy": ("teen_boy", 0.27), "grandma": ("grandma", 0.30), "grandpa": ("grandpa", 0.28)}
for name, (f, frac) in spec.items():
    im = Image.open(f"{src}/{f}.png")
    bb = im.getchannel("A").getbbox(); x0, y0, x1, y1 = bb
    h = y1 - y0; side = int(h * frac)
    a = im.getchannel("A").crop((x0, y0, x1, y0 + int(side * 0.6))).load()
    w = x1 - x0; xs = [x for x in range(w) for y in range(0, int(side * 0.6), 4) if a[x, y] > 128]
    cx = x0 + (sum(xs) / len(xs) if xs else w / 2)
    box = (int(cx - side / 2), y0 - int(side * 0.04), int(cx + side / 2), y0 + side - int(side * 0.04))
    face = im.crop(box).resize((384, 384), Image.LANCZOS)
    face.save(f"{out}/{name}_face.png", optimize=True)

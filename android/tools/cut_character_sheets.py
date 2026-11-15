import sys, os
from PIL import Image, ImageFilter
src, out = sys.argv[1], sys.argv[2]
sheets = {
 "BOY_EIGHT_POSES_RGBA": ["boy_wave","boy_read","boy_play","boy_point","boy_cheer","boy_fly","boy_stretch","boy_sleep"],
 "GIRL_EIGHT_POSES_RGBA": ["girl_wave","girl_read","girl_play","girl_paint","girl_cheer","girl_fly","girl_stretch","girl_sleep"],
 "ROBOT_EIGHT_POSES_RGBA": ["robot_wave","robot_shield","robot_read","robot_trophy","robot_search","robot_point","robot_alarm","robot_sleep"],
 "FAMILY_AND_TEENS_RGBA": ["mother_wave","father","grandma","grandpa","mother_phone","father_read","teen_girl","teen_boy"],
}
for sheet, names in sheets.items():
    im = Image.open(f"{src}/{sheet}.png").convert("RGBA")
    W, H = im.size
    a = im.getchannel("A")
    print(sheet, "max alpha", a.getextrema())
    lo, hi = 110, max(200, a.getextrema()[1] - 10)
    a2 = a.point(lambda v: 0 if v < lo else 255 if v > hi else int((v - lo) * 255 / (hi - lo)))
    # remove thin halo: erode alpha by 1px
    a2 = a2.filter(ImageFilter.MinFilter(3))
    im.putalpha(a2)
    cw, ch = W // 4, H // 2
    # connected parts at 1/4 scale; each part belongs to the cell holding its centre (drops neighbours' slivers)
    k = 4; sw, sh = W // k, H // k
    small = a2.resize((sw, sh)).load()
    lab = [[-1] * sw for _ in range(sh)]; owner = {}
    comp = 0
    for y0 in range(sh):
        for x0 in range(sw):
            if lab[y0][x0] != -1 or small[x0, y0] < 128: continue
            stack = [(x0, y0)]; lab[y0][x0] = comp; sx = sy = cnt = 0
            while stack:
                x, y = stack.pop(); sx += x; sy += y; cnt += 1
                for nx, ny in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):
                    if 0 <= nx < sw and 0 <= ny < sh and lab[ny][nx] == -1 and small[nx, ny] >= 128:
                        lab[ny][nx] = comp; stack.append((nx, ny))
            cx, cy = sx / cnt * k, sy / cnt * k
            owner[comp] = (int(cy // ch), int(cx // cw), cnt)
            comp += 1
    for i, n in enumerate(names):
        r, c = divmod(i, 4)
        keep = {cid for cid, (rr, cc, cnt) in owner.items() if rr == r and cc == c and cnt > 30}
        m = Image.new("L", (sw, sh), 0); mp = m.load()
        for y in range(sh):
            row = lab[y]
            for x in range(sw):
                if row[x] in keep: mp[x, y] = 255
        m = m.resize((W, H)).filter(ImageFilter.MaxFilter(9))
        part = im.copy(); pa = Image.composite(im.getchannel("A"), Image.new("L", (W, H), 0), m); part.putalpha(pa)
        bb0 = pa.getbbox()
        cell = part.crop(bb0)
        bb = bb0
        s = max(cell.size); pad = int(s * 0.06)
        canvas = Image.new("RGBA", (s + 2 * pad, s + 2 * pad), (0, 0, 0, 0))
        canvas.paste(cell, ((canvas.width - cell.width) // 2, canvas.height - pad - cell.height), cell)
        canvas = canvas.resize((768, 768), Image.LANCZOS)
        os.makedirs(out, exist_ok=True)
        canvas.save(f"{out}/{n}.png", optimize=True)
        print(n, bb)

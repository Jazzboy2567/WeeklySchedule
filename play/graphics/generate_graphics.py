#!/usr/bin/env python
"""Generate Play Store graphics (512 icon, 1024x500 feature) with Pillow."""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r"C:\Users\mjnie\AndroidStudioProjects\WeeklySchedule\play\graphics"
os.makedirs(OUT, exist_ok=True)

SILVER = (194, 200, 208, 255)
SILVER_EDGE = (124, 131, 140, 255)
BLACK_PEN = (23, 23, 27, 255)
CREAM = (252, 251, 247, 255)


def load_font(names, size):
    for n in names:
        p = os.path.join(r"C:\Windows\Fonts", n)
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def make_pen(k):
    """Pen drawn horizontally (tip left) on a transparent image; returns image + center."""
    W, H = int(320 * k), int(130 * k)
    cx, cy = W / 2, H / 2
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def P(pts):
        return [(cx + x * k, cy + y * k) for (x, y) in pts]

    # barrel (tapered)
    top = [(-95, -7), (-55, -11), (-10, -11), (45, -10), (85, -10), (112, -10), (118, -6), (122, -3), (122, 0)]
    bot = [(122, 3), (118, 6), (112, 10), (85, 10), (45, 10), (-10, 11), (-55, 11), (-95, 7)]
    d.polygon(P(top + bot), fill=BLACK_PEN, outline=(0, 0, 0, 255))
    # chrome tip
    d.polygon(P([(-114, 0), (-95, -7), (-95, 7)]), fill=SILVER, outline=SILVER_EDGE)
    # collar
    d.rectangle(P([(-98, -8), (-92, 8)]), fill=SILVER, outline=SILVER_EDGE)
    # centre band
    d.rectangle(P([(-24, -11), (-9, 11)]), fill=SILVER, outline=SILVER_EDGE)
    d.line(P([(-20, -11), (-20, 11)]), fill=SILVER_EDGE, width=max(1, int(1 * k)))
    d.line(P([(-13, -11), (-13, 11)]), fill=SILVER_EDGE, width=max(1, int(1 * k)))
    # specular highlight
    d.polygon(P([(4, -6), (104, -4), (104, -1), (4, -3)]), fill=(255, 255, 255, 130))
    # cap trim ring
    d.rectangle(P([(72, -10), (79, 10)]), fill=SILVER, outline=SILVER_EDGE)
    # clip
    r = max(1, int(2.5 * k))
    d.rounded_rectangle(P([(68, -17), (110, -10)]), radius=r, fill=SILVER, outline=SILVER_EDGE)

    rot = img.rotate(40, expand=True, resample=Image.BICUBIC)
    return rot


def make_art(S):
    """Notebook + pen on transparent SxS image (reference coords are 512)."""
    k = S / 512.0
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def R(x0, y0, x1, y1):
        return [x0 * k, y0 * k, x1 * k, y1 * k]

    # page shadow + page
    d.rectangle(R(150, 124, 386, 410), fill=(0, 0, 0, 34))
    d.rounded_rectangle(R(146, 116, 382, 402), radius=int(16 * k), fill=CREAM)
    # ruling lines
    lw = max(2, int(5 * k))
    for y in (168, 206, 244, 282, 320, 358):
        d.line(R(188, y, 360, y), fill=(0, 0, 0, 41), width=lw)
    # spiral strip
    d.rectangle(R(140, 132, 154, 386), fill=(0, 0, 0, 15))
    # spiral rings + holes
    for y in (146, 192, 238, 284, 330):
        d.rounded_rectangle(R(120, y, 180, y + 18), radius=int(9 * k), fill=SILVER, outline=SILVER_EDGE, width=max(1, int(2 * k)))
    for cy in (155, 201, 247, 293, 339):
        d.ellipse(R(146, cy - 4, 154, cy + 4), fill=(0, 0, 0, 34))

    # pen
    pen = make_pen(k)
    pcx, pcy = int(268 * k), int(262 * k)
    img.alpha_composite(pen, (pcx - pen.width // 2, pcy - pen.height // 2))
    return img


def vgrad(w, h, top, bottom):
    img = Image.new("RGB", (w, h), top)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


# ---- 512 icon ----
icon = vgrad(512, 512, (243, 171, 166), (230, 139, 139)).convert("RGBA")
icon.alpha_composite(make_art(512))
icon.convert("RGB").save(os.path.join(OUT, "icon-512.png"))
print("wrote icon-512.png")

# ---- 1024x500 feature graphic ----
feat = vgrad(1024, 500, (244, 176, 170), (228, 134, 134)).convert("RGBA")
art = make_art(470)
feat.alpha_composite(art, (600, 15))
d = ImageDraw.Draw(feat)
title_font = load_font(["segoeuib.ttf", "arialbd.ttf"], 86)
sub_font = load_font(["segoeui.ttf", "arial.ttf"], 38)
d.text((60, 175), "Weekly", font=title_font, fill=(38, 24, 26))
d.text((60, 265), "Scheduler", font=title_font, fill=(38, 24, 26))
d.text((62, 380), "Plan your week. Stay on track.", font=sub_font, fill=(74, 40, 42))
feat.convert("RGB").save(os.path.join(OUT, "feature-1024x500.png"))
print("wrote feature-1024x500.png")
print("done:", OUT)

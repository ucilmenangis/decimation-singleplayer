#!/usr/bin/env python3
"""Real scenes of our world in Decimation's own main menu (docs/main_menu.md; user, 17 Oktober 2026: "replace with real
scene", "just use component from decimation gui menu its self").

- Decimation's menu backgrounds (textures/gui/menu/background0..2.png, one picked at random each launch, drawn by
  RenderUtil.drawMenuBackground with its slow sway, mouse parallax and zoom; PatchMenuBackground removes the black
  layer and the cube planet over them) become six real scenes of our world (background0..5), dimmed for the buttons
  in front; fixes/MenuBackground shows them one after another, 7 seconds each.
- The pictures of our four news panels (textures/gui/menu/news_<id>.png, fixes/MenuNews), each a real picture of its
  topic (user: "replace the picture of news panel to real topic image"): our GitHub repository page, its newest
  release, the website's top and the website's coming soon cards, taken with agent-browser at 2.32 : 1 (the panel's
  picture box is 116 x 50) into docs/shots/main_menu_v0.62.9/news_src/ (docs/main_menu.md).

- Decimation's DECIMATION lettering (textures/gui/menu/banner.png and banner_intro.png) becomes our official DeciRemnant
  logo (user, 17 Oktober 2026: "replace old logo on decimation with our new logo"; logos()), from the user's picture in
  docs/shots/main_menu_v0.64.0/logo_src; the soldier and the infected of banner_intro stay.
- The FURY advert in the bottom right corner (textures/gui/menu/donor_rank_0.png, and donor_rank_1.png that pulses over
  it; BoehMod's closed store) becomes ours: since 21 Oktober 2026 a FURY style call to our Discord (corner()): an elite
  soldier of our game on a red ink splash, our logo, "JOIN THE REMNANT", our Discord, the words pulsing as FURY's did
  (the Minecraft font the website uses: tools/menu/MinecraftStandard.otf, public domain). Until then Decimation's torn
  black panel with "DeciRemnant", "Download and guides" and the site's address.
- The pictures of the news panels' detail popups (fixes/MenuDetail, MenuDetails; user, 21 Oktober 2026: "several
  image with carousel design"): detail_<popup><n>.png, 640 x 360, from the hosting shots, news_src and the site shots.
- The footer's BoehMod logo bottom left (textures/gui/menu/boehmod.png, drawn 90 x 17 at 15 % by GuiMenuBase) becomes
  our logo (user, 21 Oktober 2026: "replace boehmod logo and web link in the bottom with ours"; BoehMod is credited
  on the website); its copyright line is PatchMenuTexts'.
- The welcome box line in Decimation's lang file: "Welcome to Decimation v%s!" becomes "Welcome to DeciRemnant v%s!"
  (the version is ours: PatchMenuTexts); "Players Online: Unknown" on the official server list becomes "Players Online:
  Coming soon" (user, 17 Oktober 2026).
  The update progress screen's two lines (fixes/UpdateProgress, v0.64.7): no "your game may freeze" and no "your game
  will Shut down" (our download runs beside the menu; Quit game or Later puts the jar in place).
- The Chernarus row of the official server list (fixes/MenuServers) fades its map's pictures in behind the page while
  hovered, as Decimation's Bukovka and NewYork rows do (textures/gui/menu/serverbackgrounds/<map>0..7.png, one of the
  eight at random per hover): the user's five Chernarus pictures (docs/shots/main_menu_v0.64.0/chernarus_src, 720 x
  405), the first three twice to fill the eight slots, at Decimation's menu background size 960 x 540.

Sources: the website's real screenshots (docs/shots/site_v0.62.9, dev test siteshots) and news_src. Written with `zip` into the
three jars that carry Decimation (Decimation.jar.patched, dist/Decimation.jar, dev/libs/Decimation-base.jar); nothing
here is committed (Decimation art). Run it again after new site shots and before a release (tools/onejar.py takes
dist/Decimation.jar).

    python3 tools/menu_assets.py [--shots DIR] [--only-dir OUT]   # --only-dir: write the PNGs there, touch no jar
"""
import argparse
import io
import os
import shutil
import subprocess
import sys
import zipfile

from PIL import Image, ImageDraw, ImageEnhance, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JARS = ["Decimation.jar.patched", "dist/Decimation.jar", "dev/libs/Decimation-base.jar"]
MENU = "assets/deci/textures/gui/menu/"
# background slot -> site shot: cities first (user: "one of our generated cities"), then a base at sunset and the airport
BACKGROUNDS = {"background0": "site_hero", "background1": "site_refuge", "background2": "site_gate",
               "background3": "site_bridge", "background4": "site_base", "background5": "site_airport"}
DIM, COLOUR = 0.62, 0.8  # brightness and saturation of the backgrounds: the menu's text stays readable on them
# news panel -> its topic picture in news_src (taken at 2.32 : 1; cropped around the centre if not)
NEWS = {"news_github": "github", "news_update": "update", "news_website": "website", "news_coming": "coming"}
NEWS_SRC = os.path.join(ROOT, "docs", "shots", "main_menu_v0.62.9", "news_src")
NEWS_SIZE = (464, 200)
# a news picture from elsewhere than news_src (relative to docs/shots): the Multiplayer panel shows the game, not its screens
NEWS_PATHS = {"news_multiplayer": "main_menu_v0.65.0/mp_src/together.png"}
# official server list map -> its eight picture slots, each a picture number in MAP_SRC/<map>_<n>.jpg
MAPS = {"chernarus": [1, 2, 3, 4, 5, 1, 2, 3]}
MAP_SRC = os.path.join(ROOT, "docs", "shots", "main_menu_v0.64.0", "chernarus_src")
MAP_SIZE = (960, 540)
# detail popup slide -> its source (relative to docs/shots) and the box to cut from it first (None: the whole picture;
# "fit": a picture wider than 16:9 kept whole on a dark band, as the Community row is)
DETAILS = {
    # Multiplayer: scenes of the game with two or three people in them, not the new screens (user, 21 Oktober 2026:
    # "show 2-3 mob in-game as preview so you dont expose the new feat at all"; tools/site/mpshots.tsv)
    "detail_multiplayer0": ("main_menu_v0.65.0/mp_src/fight.png", None),               # side by side against infected
    "detail_multiplayer1": ("main_menu_v0.65.0/mp_src/together.png", None),            # friends together in a city
    "detail_multiplayer2": ("main_menu_v0.65.0/mp_src/explore.png", None),             # heading out together
    "detail_coming0": ("main_menu_v0.65.0/mp_src/together.png", None),
    "detail_coming1": ("site_v0.62.9/site_city.png", None),
}
DETAIL_SIZE = (640, 360)
# our official logo (the user's picture on white paper, cleared like the website's: tools/site/export_assets.py)
LOGO_SRC = os.path.join(ROOT, "docs", "shots", "main_menu_v0.64.0", "logo_src", "deciremnant_logo.png")
# the corner's soldier (dev test cornerart, cut out) and the Discord address it shows
CORNER_SRC = os.path.join(ROOT, "docs", "shots", "main_menu_v0.65.0", "corner_src")
DISCORD_SHOWN = "discord.gg/ZJ47zfXn8H"


ORIGINAL = os.path.join(ROOT, "Decimation.jar.original.bak")
FONT = os.path.join(ROOT, "tools", "menu", "MinecraftStandard.otf")
LANG = "assets/deci/lang/en_US.lang"


def corner():
    """Our corner art in place of BoehMod's FURY store advert (drawn 150 x 150 GUI units at the bottom right; donor_rank_1
    pulses over donor_rank_0). Learned from FURY detail by detail (docs/main_menu.md 2h; user, 21 Oktober 2026: "learn
    again from old design detail by detail deeply"): a landscape banner over the bottom half of the square only (FURY's
    rows 397 .. 800, solid from x 232), dark and opaque, fading out at its top and left; a red smoke band and black
    splatter behind; a close up soldier (waist up, cut by the bottom edge) on the left with a red rim glow and his
    rifle toward the words; one tight centred stack of words in FURY's band (x 441 .. 724, y 486 .. 706): the title (our
    logo, as FURY's brush title), "JOIN THE REMNANT" in white, a small line, the address in red and white; the pulse
    layer lights only the words. Our soldier: dev test cornerart (corner_src)."""
    import random
    from PIL import ImageChops, ImageFilter
    size, top, left = 800, 400, 60
    rnd = random.Random(21)

    def fade_mask():
        m = Image.new("L", (size, size), 0)
        px = m.load()
        for y in range(top, size):
            ty = min(1.0, (y - top) / 80.0)                       # the soft top edge
            for x in range(left, size):
                tx = min(1.0, (x - left) / 220.0)                 # the soft left edge
                px[x, y] = int(255 * ty * tx)
        return m

    banner = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(banner)
    for y in range(top, size):                                  # dark, a touch lighter at the top left like FURY's
        k = (y - top) / float(size - top)
        c = (int(36 - 28 * k), int(34 - 28 * k), int(38 - 30 * k))
        d.line([(left, y), (size, y)], fill=c + (255,))
    smoke = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(smoke)
    for i in range(26):                                         # the red smoke band behind the soldier and the words
        x, y = rnd.randint(330, 800), rnd.randint(470, 760)
        rx, ry = rnd.randint(50, 130), rnd.randint(25, 70)
        sd.ellipse([x - rx, y - ry, x + rx, y + ry], fill=(200 + rnd.randint(0, 50), 18, 18, rnd.randint(130, 210)))
    smoke = smoke.filter(ImageFilter.GaussianBlur(18))
    banner.alpha_composite(smoke)
    ink = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    idr = ImageDraw.Draw(ink)
    for i in range(60):                                         # black splatter and drips along the right edge
        x, y = rnd.randint(640, 800), rnd.randint(430, 800)
        r = rnd.randint(2, 14) if rnd.random() < 0.8 else rnd.randint(16, 30)
        idr.ellipse([x - r, y - r, x + r, y + r], fill=(4, 2, 2, 235))
    for i in range(8):
        x = rnd.randint(660, 800)
        y0 = rnd.randint(440, 640)
        idr.rectangle([x, y0, x + rnd.randint(2, 4), y0 + rnd.randint(30, 110)], fill=(4, 2, 2, 220))
    for i in range(3):                                          # FURY's thin diagonal streaks
        x0 = rnd.randint(520, 640)
        idr.line([(x0, 410), (x0 - 90, 640)], fill=(0, 0, 0, 120), width=2)
    banner.alpha_composite(ink.filter(ImageFilter.GaussianBlur(0.8)))
    shade = Image.new("RGBA", (size, size), (0, 0, 0, 0))     # the bottom goes to black
    shd = ImageDraw.Draw(shade)
    for y in range(size - 90, size):
        shd.line([(left, y), (size, y)], fill=(0, 0, 0, int(200 * (y - size + 90) / 90.0)))
    banner.alpha_composite(shade)
    # the soldier: a close up, waist up, cut by the banner's bottom edge, a red rim glow
    soldier = Image.open(os.path.join(CORNER_SRC, "soldier.png")).convert("RGBA")
    soldier = soldier.crop((0, 0, soldier.width, int(soldier.height * 0.70)))  # down to the rifle across his body
    sh = 352
    soldier = soldier.resize((round(soldier.width * sh / soldier.height), sh), Image.LANCZOS)
    sx, sy = 196, size - sh + 6                                  # in the banner's solid part, as FURY's (x 232 on)
    lit = Image.new("RGBA", soldier.size, (200, 20, 20, 0))      # lit red from the smoke, as FURY's soldier is
    lit.putalpha(soldier.split()[3].point(lambda v: int(v * 0.12)))
    soldier = Image.alpha_composite(soldier, lit)
    glow = Image.new("RGBA", soldier.size, (235, 30, 30, 0))
    glow.putalpha(soldier.split()[3].filter(ImageFilter.GaussianBlur(11)).point(lambda v: min(255, int(v * 2.4))))
    banner.alpha_composite(glow, (sx, sy))
    banner.alpha_composite(soldier, (sx, sy))
    rim = Image.new("RGBA", soldier.size, (255, 60, 60, 0))  # light on his right edge, from the red behind him
    a = soldier.split()[3]
    edge = ImageChops.subtract(a, a.filter(ImageFilter.MinFilter(5)))
    rim.putalpha(edge.point(lambda v: int(v * 0.55)))
    banner.alpha_composite(rim, (sx, sy))
    banner.putalpha(ImageChops.multiply(banner.split()[3], fade_mask()))
    base = banner
    pulse = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # the words: one tight stack centred in FURY's band, each line placed by its measured height
    sys.path.insert(0, os.path.join(ROOT, "tools", "site"))
    from export_assets import clear_paper
    logo = clear_paper(Image.open(LOGO_SRC))
    logo = logo.crop(logo.getbbox())
    cx, y, band = 628, 452, 330
    logo = logo.resize((band, round(logo.height * band / logo.width)), Image.LANCZOS)
    halo = Image.new("RGBA", logo.size, (0, 0, 0, 0))
    halo.putalpha(logo.split()[3].filter(ImageFilter.GaussianBlur(6)))
    base.alpha_composite(halo, (cx - band // 2 + 3, y + 4))
    base.alpha_composite(logo, (cx - band // 2, y))
    y += logo.height + 10

    def line(text, colour, width, gap, pulses=True, colours=None):
        nonlocal y
        fs = 60
        font = ImageFont.truetype(FONT, fs)
        while font.getbbox(text)[2] - font.getbbox(text)[0] > width:
            fs -= 1
            font = ImageFont.truetype(FONT, fs)
        b = font.getbbox(text)
        x0 = cx - (b[2] - b[0]) // 2 - b[0]
        y0 = y - b[1]
        sh = max(2, fs // 9)
        dd = ImageDraw.Draw(base)
        dd.text((x0 + sh, y0 + sh), text, font=font, fill=(20, 4, 4, 240))
        if colours:                                             # address: "discord.gg/" red, the code white
            dd.text((x0, y0), colours[0][0], font=font, fill=colours[0][1] + (255,))
            w0 = font.getbbox(colours[0][0])[2]
            dd.text((x0 + w0, y0), colours[1][0], font=font, fill=colours[1][1] + (255,))
        else:
            dd.text((x0, y0), text, font=font, fill=colour + (255,))
        if pulses:
            ImageDraw.Draw(pulse).text((x0, y0), text, font=font, fill=(255, 226, 214, 255))
        y += (b[3] - b[1]) + gap

    line("JOIN THE REMNANT", (255, 255, 255), band - 6, 16)
    line("NEWS, HELP AND PLAYERS ON OUR DISCORD", (236, 232, 226), band - 60, 12, pulses=False)
    code = DISCORD_SHOWN.split("/")[-1]
    line(DISCORD_SHOWN, None, band - 130, 0, colours=[(DISCORD_SHOWN[:-len(code)], (232, 60, 60)), (code, (245, 245, 245))])
    return {"donor_rank_0": base, "donor_rank_1": pulse}


def logos():
    """Our logo in place of Decimation's DECIMATION lettering (user, 17 Oktober 2026: "replace old logo on decimation with
    our new logo"), both at twice the old size (drawn by UV 0..1, so only sharper):
    - banner.png (461 x 101; the top left of every menu page at 160 x 35, in game at 186 x 40 and 93 x 20): ours fitted
      into the old lettering's box (1, 3, 459, 99);
    - banner_intro.png (512 x 240; the home page and the update screen at 247 x 116): Decimation's soldier and infected
      kept, everything from row 127 down (its lettering and barbed wire) cleared and ours set across the old width
      (x 20 .. 490), its foot on row 222."""
    sys.path.insert(0, os.path.join(ROOT, "tools", "site"))
    from export_assets import clear_paper
    ours = clear_paper(Image.open(LOGO_SRC))
    out = {}
    with zipfile.ZipFile(ORIGINAL) as z:
        old = Image.open(io.BytesIO(z.read(MENU + "banner_intro.png"))).convert("RGBA")
    k = 2
    box = (1 * k, 3 * k, 459 * k, 99 * k)
    scale = min((box[2] - box[0]) / ours.width, (box[3] - box[1]) / ours.height)
    mark = ours.resize((round(ours.width * scale), round(ours.height * scale)), Image.LANCZOS)
    banner = Image.new("RGBA", (461 * k, 101 * k), (0, 0, 0, 0))
    banner.alpha_composite(mark, ((banner.width - mark.width) // 2, box[1] + (box[3] - box[1] - mark.height) // 2))
    out["banner"] = banner
    intro = old.resize((old.width * k, old.height * k), Image.LANCZOS)
    cut = Image.new("RGBA", (intro.width, intro.height - 127 * k), (0, 0, 0, 0))
    intro.paste(cut, (0, 127 * k))
    w = (490 - 20) * k
    mark = ours.resize((w, round(ours.height * w / ours.width)), Image.LANCZOS)
    intro.alpha_composite(mark, (20 * k, 222 * k - mark.height))
    out["banner_intro"] = intro
    return out


def footer():
    """Our logo in the footer's BoehMod logo slot (boehmod.png, 180 x 35, drawn 90 x 17 at 15 % from x 3): ours at twice
    the size, as high as the slot, from its left edge."""
    sys.path.insert(0, os.path.join(ROOT, "tools", "site"))
    from export_assets import clear_paper
    ours = clear_paper(Image.open(LOGO_SRC))
    ours = ours.crop(ours.getbbox())
    k = 2
    slot = Image.new("RGBA", (180 * k, 35 * k), (0, 0, 0, 0))
    scale = min(slot.width / ours.width, slot.height / ours.height)
    mark = ours.resize((round(ours.width * scale), round(ours.height * scale)), Image.LANCZOS)
    slot.alpha_composite(mark, (0, (slot.height - mark.height) // 2))
    return {"boehmod": slot}


def lang(out):
    """Decimation's lang file with our name in the welcome box (the version there is ours through PatchMenuTexts) and
    the official server list's player count coming soon."""
    with zipfile.ZipFile(ORIGINAL) as z:
        text = z.read(LANG).decode("utf-8")
    for old, new in [("menu.notification.welcome2=Welcome to Decimation v%s! Have fun!",
                      "menu.notification.welcome2=Welcome to DeciRemnant v%s! Have fun!"),
                     ("menu.stats.playercount.unknown=Unknown", "menu.stats.playercount.unknown=Coming soon"),
                     # the update progress screen (fixes/UpdateProgress): our download runs beside the menu and the
                     # game does not shut itself down
                     ("menu.updating.paragraph1=This may take a while! Your game may freeze! (That's normal!)",
                      "menu.updating.paragraph1=This may take a while. The menu keeps working while it downloads."),
                     ("menu.updating.paragraph2=After download, your game will Shut down! Restart the game to finish updating!",
                      "menu.updating.paragraph2=When it is done: Quit game puts the new version in place, Later does it when you close the game.")]:
        if old not in text:
            sys.exit("changed in Decimation's lang file: " + old)
        text = text.replace(old, new)
    path = os.path.join(out, LANG)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)
    return LANG


def crop_ratio(im, centre, ratio):
    """The largest crop of `ratio` (w / h) around `centre` that fits the picture."""
    w, h = im.size
    cw, ch = (w, round(w / ratio)) if w / h < ratio else (round(h * ratio), h)
    cx = min(max(round(centre[0] * w - cw / 2), 0), w - cw)
    cy = min(max(round(centre[1] * h - ch / 2), 0), h - ch)
    return im.crop((cx, cy, cx + cw, cy + ch))


def build(shots, out):
    os.makedirs(out, exist_ok=True)
    made = []
    for slot, shot in BACKGROUNDS.items():
        im = Image.open(os.path.join(shots, shot + ".png")).convert("RGB")
        im = ImageEnhance.Color(im).enhance(COLOUR)
        im = ImageEnhance.Brightness(im).enhance(DIM)
        im.save(os.path.join(out, slot + ".png"), optimize=True)
        made.append((slot, shot, im.size))
    for name, im in logos().items():
        im.save(os.path.join(out, name + ".png"), optimize=True)
        made.append((name, "logo_src/deciremnant_logo", im.size))
    for name, im in footer().items():
        im.save(os.path.join(out, name + ".png"), optimize=True)
        made.append((name, "logo_src/deciremnant_logo", im.size))
    for name, im in corner().items():
        im.save(os.path.join(out, name + ".png"), optimize=True)
        made.append((name, "corner_src + logo", im.size))
    for name, topic in NEWS.items():
        source = os.path.join(ROOT, "docs", "shots", NEWS_PATHS[name]) if name in NEWS_PATHS else os.path.join(NEWS_SRC, topic + ".png")
        im = Image.open(source).convert("RGB")
        im = crop_ratio(im, (0.5, 0.5), NEWS_SIZE[0] / NEWS_SIZE[1]).resize(NEWS_SIZE, Image.LANCZOS)
        im.save(os.path.join(out, name + ".png"), optimize=True)
        made.append((name, "news_src/" + topic, im.size))
    for name, (source, cut) in DETAILS.items():
        im = Image.open(os.path.join(ROOT, "docs", "shots", source)).convert("RGB")
        if cut == "fit":
            band = Image.new("RGB", DETAIL_SIZE, (16, 16, 16))
            wide = im.resize((DETAIL_SIZE[0], round(im.height * DETAIL_SIZE[0] / im.width)), Image.LANCZOS)
            band.paste(wide, (0, (DETAIL_SIZE[1] - wide.height) // 2))
            im = band
        else:
            im = im.crop(cut) if cut else im
            im = crop_ratio(im, (0.5, 0.5), DETAIL_SIZE[0] / DETAIL_SIZE[1]).resize(DETAIL_SIZE, Image.LANCZOS)
        im.save(os.path.join(out, name + ".png"), optimize=True)
        made.append((name, source.split("/")[-1][:13], im.size))
    os.makedirs(os.path.join(out, "serverbackgrounds"), exist_ok=True)
    for name, pictures in MAPS.items():
        for slot, n in enumerate(pictures):
            im = Image.open(os.path.join(MAP_SRC, "%s_%d.jpg" % (name, n))).convert("RGB")
            im = crop_ratio(im, (0.5, 0.5), MAP_SIZE[0] / MAP_SIZE[1]).resize(MAP_SIZE, Image.LANCZOS)
            im.save(os.path.join(out, "serverbackgrounds", "%s%d.png" % (name, slot)), optimize=True)
            made.append(("serverbackgrounds/%s%d" % (name, slot), "%s_%d" % (name, n), im.size))
    for name, shot, size in made:
        print("%-30s from %-13s %4d x %d" % (name, shot, size[0], size[1]))
    return [n for n, _, _ in made]


def entries(jar):
    out = subprocess.run(["unzip", "-l", jar], capture_output=True, text=True, check=True).stdout.strip().splitlines()
    return int(out[-1].split()[1])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--shots", default=os.path.join(ROOT, "docs", "shots", "site_v0.62.9"))
    ap.add_argument("--only-dir", help="write the pictures here and touch no jar")
    a = ap.parse_args()
    if a.only_dir:
        build(a.shots, a.only_dir)
        return
    stage = os.path.join(ROOT, "build", "menu_assets")
    shutil.rmtree(stage, ignore_errors=True)
    names = build(a.shots, os.path.join(stage, MENU))
    files = [MENU + n + ".png" for n in names] + [lang(stage)]
    for jar in JARS:
        path = os.path.join(ROOT, jar)
        before = entries(path)
        subprocess.run(["zip", "-q", path] + files, cwd=stage, check=True)
        test = subprocess.run(["unzip", "-tq", path], capture_output=True, text=True).stdout.strip()
        print("%-30s entries %d -> %d | %s" % (jar, before, entries(path), test))
        if not test.startswith("No errors"):
            sys.exit(1)


if __name__ == "__main__":
    main()

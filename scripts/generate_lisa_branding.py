"""Generate LISA launcher and splash branding assets for Android."""
from pathlib import Path

from PIL import Image

SRC = Path(
    r"C:\Users\Richa\.cursor\projects\c-Users-Richa-AndroidStudioProjects-LISA2\assets"
    r"\c__Users_Richa_AppData_Roaming_Cursor_User_workspaceStorage_2e03a06b9e3c23b42871b1db6311951c_images"
    r"_ChatGPT_Image_Jul_1__2026__04_01_24_PM-22b16cb4-6e64-4a86-8624-4081c9af97bf.png"
)
RES = Path(r"C:\Users\Richa\AndroidStudioProjects\LISA2\app\src\main\res")

# Logo occupies ~62.5% of canvas — safe zone for adaptive launcher icons.
LAUNCHER_LOGO_SCALE = 0.625

# Splash logo box is 280dp (~35% of an 800dp screen); bitmap fills the box.
SPLASH_LOGO_DP = 280

# Splash animated-icon canvas is 288dp; logo scaled inside to avoid circular crop.
SPLASH_ICON_CANVAS_DP = 288
SPLASH_ICON_LOGO_SCALE = 0.42

LAUNCHER_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

FOREGROUND_SIZES = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}

DENSITY_SCALE = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def scaled_logo(source: Image.Image, size: int) -> Image.Image:
    return source.resize((size, size), Image.Resampling.LANCZOS)


def paste_centered(canvas: Image.Image, logo: Image.Image) -> None:
    offset = ((canvas.width - logo.width) // 2, (canvas.height - logo.height) // 2)
    if logo.mode == "RGBA":
        canvas.paste(logo, offset, logo)
    else:
        canvas.paste(logo, offset)


def compose_on_white(source: Image.Image, canvas_size: int, logo_scale: float) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (255, 255, 255, 255))
    logo_size = max(1, int(canvas_size * logo_scale))
    logo = scaled_logo(source, logo_size)
    paste_centered(canvas, logo)
    return canvas


def compose_transparent(source: Image.Image, canvas_size: int, logo_scale: float) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    logo_size = max(1, int(canvas_size * logo_scale))
    logo = scaled_logo(source, logo_size)
    paste_centered(canvas, logo)
    return canvas


def dp_to_px(dp: float, density: str) -> int:
    return max(1, int(dp * DENSITY_SCALE[density]))


def main() -> None:
    source = Image.open(SRC).convert("RGBA")

    for folder, size in LAUNCHER_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = compose_on_white(source, size, LAUNCHER_LOGO_SCALE)
        icon.save(out_dir / "ic_launcher.png", optimize=True)
        icon.save(out_dir / "ic_launcher_round.png", optimize=True)

    for folder, size in FOREGROUND_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        fg = compose_transparent(source, size, LAUNCHER_LOGO_SCALE)
        fg.save(out_dir / "ic_launcher_foreground.png", optimize=True)

    for density in DENSITY_SCALE:
        out_dir = RES / f"drawable-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)

        splash_logo_px = dp_to_px(SPLASH_LOGO_DP, density)
        splash_logo = scaled_logo(source, splash_logo_px)
        splash_logo.save(out_dir / "splash_logo.png", optimize=True)

        splash_icon_px = dp_to_px(SPLASH_ICON_CANVAS_DP, density)
        splash_icon = compose_on_white(source, splash_icon_px, SPLASH_ICON_LOGO_SCALE)
        splash_icon.save(out_dir / "splash_icon.png", optimize=True)

    print("Generated launcher and splash branding assets.")


if __name__ == "__main__":
    main()

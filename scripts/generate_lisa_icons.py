"""Generate LISA launcher icons from source PNG for all Android densities."""
from pathlib import Path
from PIL import Image

SRC = Path(
    r"C:\Users\Richa\.cursor\projects\c-Users-Richa-AndroidStudioProjects-LISA2\assets"
    r"\c__Users_Richa_AppData_Roaming_Cursor_User_workspaceStorage_2e03a06b9e3c23b42871b1db6311951c_images"
    r"_ChatGPT_Image_Jul_1__2026__04_01_24_PM-22b16cb4-6e64-4a86-8624-4081c9af97bf.png"
)
RES = Path(r"C:\Users\Richa\AndroidStudioProjects\LISA2\app\src\main\res")

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


def resize_square(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    source = Image.open(SRC).convert("RGBA")
    for folder, size in LAUNCHER_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = resize_square(source, size)
        icon.save(out_dir / "ic_launcher.png", optimize=True)
        icon.save(out_dir / "ic_launcher_round.png", optimize=True)

    for folder, size in FOREGROUND_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        fg = resize_square(source, size)
        fg.save(out_dir / "ic_launcher_foreground.png", optimize=True)

    print("Generated launcher icons for all densities.")


if __name__ == "__main__":
    main()

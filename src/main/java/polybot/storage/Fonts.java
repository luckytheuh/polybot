package polybot.storage;

import polybot.PolyBot;
import polybot.util.BotUtil;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public enum Fonts { //BARCODE("barcode.ttf", 40),
    ANGRY_BIRDS("angrybirds.ttf"),
    ARIAL("arial.ttf"),
    CALCULATOR("calcscreen.ttf", 34),
    CALCULATOR_ALTERNATIVE("calcscreenmono.ttf", 32),
    COMIC_SANS("comic.ttf"),
    DIARY_OF_A_WIMPY_KID("doawk.ttf", "doawk"),
    ESMBOT("caption.otf"),
    GMOD("gmod.otf", "garrys mod"),
    GOOGLE_DCIG("pixelplus.ttf", "doodle champion island games"),
    HIGHWAY_SIGNAGE("signage.ttf", "highway", "signage"),
    IMPACT("impact.ttf"),
    INDIANA_JONES("indianajones.otf"),
    JETBRAINS("jetbrains.ttf"),
    LICENSE_PLATE("licenseplate.otf", 34),
    MINECRAFT("minecraft.otf"),
    MODERNICA("modernica.otf", 36),
    PAPYRUS("papyrus.ttf"),
    PIKMIN("pikmin.otf"),
    ROBLOX("GillSansUltraBold.ttf", 26) //PRIDE("pride.otf"),
    ,
    ROBOTO("Roboto-Regular.ttf"),
    RODONDO("rodondo.otf", 34),
    SM64("sm64.ttf", 28),
    SPLATOON1("splatoon1.otf"),
    SPLATOON2("splatoon2.otf"),
    SPONGEBOB("spongebob.ttf", 28),
    TAHOMA("tahoma.ttf"),
    TERRARIA("terraria.ttf", 34),
    TF2("tf2.ttf"),
    TF2_ALTERNATIVE("tf2-alt.ttf"),
    TIMES_NEW_ROMAN("times.ttf"),
    TRANSFORMERS("transformers.ttf"),
    ULTRAKILL("ultrakill.ttf"),
    UNDERTALE("undertale.otf"),
    UPSIDE_DOWN("upsidedown.ttf");

    final String[] aliases;
    final Font font;

    Fonts(String fontName) {
        this(fontName, "");
    }

    Fonts(String fontName, String... aliases) {
        this(fontName, 30, aliases);
    }

    Fonts(String fontFile, int size) {
        this(fontFile, size, "");
    }

    Fonts(String fontFile, int size, String... aliases) {
        if (Arrays.stream(aliases).anyMatch(String::isBlank)) aliases = null;
        this.aliases = aliases;

        Font f = null;
        try {
            f = Font.createFont(Font.TRUETYPE_FONT, Files.newInputStream(Paths.get("./fonts/" + fontFile))).deriveFont((float) size);
        } catch (IOException | FontFormatException e) {
            PolyBot.getLogger().warn("Failed to load font " + fontFile + "!");
        }
        this.font = f;
    }

    public String friendlyName() {
        return name().toLowerCase().replace('_', ' ');
    }

    public Font getFont() {
        return font;
    }

    public static Fonts getFontFromString(String str) {
        for (Fonts f : Fonts.values()) {
            if (BotUtil.settingNameMatch(str, f.friendlyName(), f.name().toLowerCase())) return f;
            if (BotUtil.settingNameMatch(str, f.aliases)) return f;
        }

        return null;
    }
}

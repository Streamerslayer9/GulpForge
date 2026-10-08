package com.example.gulp;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Client-side settings, saved to config/gulp-client.properties. */
public class GulpConfig {
    public enum BellyStyle {
        /** The original look: the whole torso (chest to waist) swells forward. */
        CLASSIC("Classic (whole torso)"),
        /** Only the lower stomach area swells; the chest stays flat. */
        STOMACH("Stomach only");

        public final String label;
        BellyStyle(String label) { this.label = label; }

        public BellyStyle next() {
            BellyStyle[] v = values();
            return v[(ordinal() + 1) % v.length];
        }
    }

    public static BellyStyle bellyStyle = BellyStyle.STOMACH;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("gulp-client.properties");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(f)) {
                props.load(in);
            }
            bellyStyle = BellyStyle.valueOf(props.getProperty("bellyStyle", BellyStyle.STOMACH.name()));
        } catch (Exception ignored) {
            // Bad or missing file: keep defaults.
        }
    }

    public static void save() {
        try {
            Properties props = new Properties();
            props.setProperty("bellyStyle", bellyStyle.name());
            try (OutputStream out = Files.newOutputStream(file())) {
                props.store(out, "Gulp client settings");
            }
        } catch (Exception ignored) {
        }
    }
}

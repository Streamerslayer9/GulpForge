package com.example.gulp;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

/** Every sound the mod makes. Each has a custom version (your .ogg files) and a vanilla Minecraft stand-in. */
public enum GulpSound {
    SWALLOW("Swallow", () -> Gulp.SWALLOW.get(), () -> SoundEvents.GENERIC_EAT),
    SPIT("Spit out", () -> Gulp.RELEASE.get(), () -> SoundEvents.SLIME_SQUISH),
    DIGEST("Digestion", () -> Gulp.DIGEST.get(), () -> SoundEvents.PLAYER_BURP),
    DIGEST_BUTTON("Digest button", () -> Gulp.DIGEST_BUTTON.get(), () -> SoundEvents.PLAYER_BURP),
    /** Only exists as a custom sound; the Minecraft sound set has no screen music. */
    SCREEN_LOOP("Screen music", () -> Gulp.SCREEN_LOOP.get(), () -> null);

    public final String label;
    private final Supplier<SoundEvent> custom;
    private final Supplier<SoundEvent> vanilla;

    GulpSound(String label, Supplier<SoundEvent> custom, Supplier<SoundEvent> vanilla) {
        this.label = label;
        this.custom = custom;
        this.vanilla = vanilla;
    }

    public SoundEvent custom() { return custom.get(); }

    /** @return the vanilla stand-in, or null if there isn't one */
    public SoundEvent vanilla() { return vanilla.get(); }

    public static GulpSound byIndex(int i) {
        GulpSound[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}

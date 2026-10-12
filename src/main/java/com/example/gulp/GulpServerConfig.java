package com.example.gulp;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server settings for multiplayer admins (config file: serverconfig/gulp-server.toml inside each world,
 * or the world's serverconfig folder on a dedicated server). Only the server reads these.
 */
public final class GulpServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue HARD_MODE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STRUGGLE_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ITEM_STORAGE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PLAYER_SWALLOWING_ENABLED;
    public static final ForgeConfigSpec.IntValue PLAYER_MAX_HOLD_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        HARD_MODE_ENABLED = b
                .comment("If false, nobody can digest mobs: hard mode and the Digest button are turned off (soft mode only).")
                .define("hardModeEnabled", true);
        STRUGGLE_DAMAGE_MULTIPLIER = b
                .comment("Multiplies the damage swallowed mobs deal to you in hard mode. 0 = they never hurt you, 1 = normal.")
                .defineInRange("struggleDamageMultiplier", 1.0, 0.0, 10.0);
        XP_MULTIPLIER = b
                .comment("Multiplies all stomach XP gained. 0.5 = half as fast, 2 = twice as fast.")
                .defineInRange("xpMultiplier", 1.0, 0.0, 10.0);
        ITEM_STORAGE_ENABLED = b
                .comment("If false, the stomach can't store items (no item swallowing and no Storage screen).")
                .define("itemStorageEnabled", true);
        PLAYER_SWALLOWING_ENABLED = b
                .comment("If false, players can't swallow each other at all.",
                        "Even when true, a player can only be swallowed if they turned on 'Let players swallow me' themselves,",
                        "swallowed players are only held (never harmed or digested), and they can leave any time.")
                .define("playerSwallowingEnabled", true);
        PLAYER_MAX_HOLD_SECONDS = b
                .comment("Longest a swallowed player is held before they are let out automatically.")
                .defineInRange("playerMaxHoldSeconds", 300, 10, 3600);
        SPEC = b.build();
    }

    private GulpServerConfig() { }
}

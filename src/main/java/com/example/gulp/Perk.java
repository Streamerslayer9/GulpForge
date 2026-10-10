package com.example.gulp;

/**
 * The perks. Add a new one at the END of this list, give it an effect in StomachLogic and an icon in PerksScreen.
 * There are 98 perk points by level 99 (one per level-up) and 110 ranks in total, so a max-level player
 * can't have everything and has to choose.
 */
public enum Perk {
    ROOMY("Roomy Stomach", "+6% stomach capacity per rank.", 30),
    HEALING("Healing Stomach", "Soft mode: held mobs heal 1% max HP/rank every 5s.", 15),
    RICH("Better Loot", "Looting on digested mobs: +1 level per 2 ranks (max V).", 10),
    QUICK("Quick Gulp", "Swallow cooldown -0.075s/rank, -0.15s looking up.", 10),
    IRON("Iron Stomach", "Struggling mobs hurt 5% less per rank.", 15),
    GOURMET("Gourmet", "+5% XP from digesting mobs per rank.", 20),
    HEARTY("Hearty Meal", "Heal 0.5 HP per rank whenever you digest a mob.", 10);

    public final String title;
    public final String description;
    public final int maxRank;

    Perk(String title, String description, int maxRank) {
        this.title = title;
        this.description = description;
        this.maxRank = maxRank;
    }

    public static Perk byIndex(int i) {
        Perk[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}

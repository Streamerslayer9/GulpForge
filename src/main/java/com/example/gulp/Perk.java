package com.example.gulp;

/** The perks. Add a new one at the END of this list, give it an effect in StomachLogic and an icon in PerksScreen. */
public enum Perk {
    ROOMY("Roomy Stomach", "+1 stomach capacity per rank.", 5),
    HEALING("Healing Stomach", "Soft mode: held mobs heal 1 HP/rank every 5s.", 5),
    RICH("Better Loot", "+1 Looting level per rank on digested mobs.", 5),
    QUICK("Quick Gulp", "Looking up: swallow cooldown -0.3s per rank.", 5),
    IRON("Iron Stomach", "Struggling mobs hurt 15% less per rank.", 5);

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

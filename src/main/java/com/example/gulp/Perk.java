package com.example.gulp;

/** The perks. Add a new one at the END of this list, give it an effect in StomachLogic and an icon in PerksScreen. */
public enum Perk {
    ROOMY("Roomy Stomach", "+1 stomach capacity per rank.", 5),
    HEALING("Healing Stomach", "Soft mode: held mobs heal 1 HP/rank every 5s.", 3),
    RICH("Rich Digestion", "+1 Looting level per rank on digested mobs.", 3),
    QUICK("Quick Gulp", "Looking up: swallow cooldown -0.5s per rank.", 3),
    IRON("Iron Stomach", "Struggling mobs hurt 25% less per rank.", 3);

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

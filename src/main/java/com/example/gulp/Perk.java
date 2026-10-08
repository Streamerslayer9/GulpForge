package com.example.gulp;

/** The demo perks. Add a new one here, then give it an effect in Stomach/StomachLogic and an icon in PerksScreen. */
public enum Perk {
    ROOMY("Roomy Stomach", "+1 stomach capacity per rank.", 5),
    HEALING("Healing Stomach", "Soft mode: held mobs heal 1 HP per rank every 5 seconds.", 3),
    RICH("Rich Digestion", "+15% chance per rank of a bonus loot roll when you digest something.", 3);

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

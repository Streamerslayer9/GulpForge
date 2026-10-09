package com.example.gulp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** One player's stomach. Each entry in `contents` is an NBT compound:
 *  Entity (full saved mob), Size, MaxHealth, Digest, DigestTime, Name, Uid */
public class Stomach {
    public static final int MAX_LEVEL = 20;

    /** Soft mode: XP per second for every 1.0 of mob volume you're holding. */
    public static final float SOFT_XP_PER_VOLUME_SECOND = 0.5f;

    public boolean hard = false;
    public int level = 1;
    public int xp = 0;
    public int points = 0;                                  // unspent perk points
    public final int[] ranks = new int[Perk.values().length];
    public final List<CompoundTag> contents = new ArrayList<>();
    public transient int cooldown = 0;

    /** XP needed to go from `level` to the next. Grows faster than linear: 100, 280, 520, 800, 1120, 1470 ... */
    public static int xpForNext(int level) {
        return (int) (Math.round(100 * Math.pow(level, 1.5) / 10.0) * 10);
    }

    /** XP for digesting a mob. Tougher mobs are worth much more. */
    public static int digestXp(float maxHealth) {
        return 15 + (int) (3 * maxHealth);
    }

    public int rank(Perk perk) { return ranks[perk.ordinal()]; }

    /** How much "mob volume" fits. Chicken ~0.1, villager ~0.7, cow ~1.1, horse ~3, iron golem ~5. */
    public double capacity() { return 1.0 + level * 0.75 + rank(Perk.ROOMY) * 1.0; }

    public double used() {
        double total = 0;
        for (CompoundTag e : contents) total += e.getFloat("Size");
        return total;
    }

    /** @return how many levels were gained (each level gives 1 perk point) */
    public int addXp(int amount) {
        if (level >= MAX_LEVEL) return 0;
        xp += amount;
        int gained = 0;
        while (level < MAX_LEVEL && xp >= xpForNext(level)) {
            xp -= xpForNext(level);
            level++;
            points++;
            gained++;
        }
        return gained;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("Hard", hard);
        nbt.putInt("Level", level);
        nbt.putInt("Xp", xp);
        nbt.putInt("Points", points);
        CompoundTag perks = new CompoundTag();
        for (Perk p : Perk.values()) perks.putInt(p.name(), ranks[p.ordinal()]);
        nbt.put("Perks", perks);
        ListTag list = new ListTag();
        list.addAll(contents);
        nbt.put("Contents", list);
        return nbt;
    }

    public static Stomach fromNbt(CompoundTag nbt) {
        Stomach s = new Stomach();
        s.hard = nbt.getBoolean("Hard");
        s.level = Math.max(1, nbt.getInt("Level"));
        s.xp = nbt.getInt("Xp");
        s.points = nbt.contains("Points") ? nbt.getInt("Points") : s.level - 1;
        CompoundTag perks = nbt.getCompound("Perks");
        for (Perk p : Perk.values()) s.ranks[p.ordinal()] = Math.min(p.maxRank, perks.getInt(p.name()));
        ListTag list = nbt.getList("Contents", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) s.contents.add(list.getCompound(i));
        return s;
    }
}

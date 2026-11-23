package com.example.gulp;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Townstead (an MCA Reborn expansion) gives every villager Hunger, Thirst and Energy (fatigue) needs, and when the night is skipped it
 * restores the energy of the villagers who were sleeping. A villager inside someone's stomach is not in the world, so it never sees that:
 * sleeping in a bed has to count for them too. Townstead does not publish where it keeps the numbers, so this reads them from the
 * villager's saved data (the same data a swallowed villager is stored as), looking for numbers named fatigue / energy / hunger / thirst.
 * It does not need Townstead to be installed: no such numbers means nothing changes. /gulp townstead shows what it finds.
 *
 * What it does for a villager inside you:
 *  - Hunger and thirst are never changed: a stored villager is not ticking, so nothing drains while they are in there.
 *  - When you sleep through a night, fatigue goes to 0 (rested) and energy to full (20), like a villager who slept through it.
 */
final class Townstead {
    private Townstead() { }

    /** Is a mod with "townstead" in its id loaded? */
    static boolean installed() {
        for (net.minecraftforge.forgespi.language.IModInfo m : ModList.get().getMods()) {
            if (m.getModId().toLowerCase(Locale.ROOT).contains("townstead")) return true;
        }
        return false;
    }

    /** 0 = not a need, 1 = fatigue (lower is more rested), 2 = energy (higher is better), 3 = hunger or thirst. */
    private static int kind(String keyLower, String pathLower) {
        if (keyLower.contains("fatigue")) return 1;
        boolean needsPath = pathLower.contains("townstead") || pathLower.contains("need");
        if (needsPath && (keyLower.equals("energy") || keyLower.endsWith("energy"))) return 2;
        if (needsPath && (keyLower.contains("hunger") || keyLower.contains("thirst"))) return 3;
        return 0;
    }

    /**
     * Rests a stored villager (their saved data): fatigue to 0 and energy to full.
     * @return how many numbers were changed
     */
    static int rest(CompoundTag villager) {
        return walk(villager, "", true, null);
    }

    /** Every need-looking number in a villager's saved data, as "path = value", for /gulp townstead. */
    static List<String> find(CompoundTag villager) {
        List<String> out = new ArrayList<>();
        walk(villager, "", false, out);
        return out;
    }

    private static int walk(CompoundTag tag, String path, boolean change, List<String> out) {
        int changed = 0;
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            Tag v = tag.get(key);
            String here = path.isEmpty() ? key : path + "." + key;
            if (v instanceof CompoundTag c) {
                changed += walk(c, here, change, out);
            } else if (v instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof CompoundTag c) changed += walk(c, here + "[" + i + "]", change, out);
                }
            } else if (v instanceof NumericTag n) {
                int kind = kind(key.toLowerCase(Locale.ROOT), path.toLowerCase(Locale.ROOT));
                if (kind == 0) continue;
                if (out != null) out.add(here + " = " + n.getAsDouble());
                if (!change) continue;
                if (kind == 1 && n.getAsDouble() > 0.0) {
                    tag.put(key, sameType(n, 0.0));
                    changed++;
                } else if (kind == 2 && n.getAsDouble() < 20.0) {
                    tag.put(key, sameType(n, 20.0));
                    changed++;
                }
            }
        }
        return changed;
    }

    /** A number of the same kind as `like` (int stays int, float stays float...) with a new value. */
    private static Tag sameType(NumericTag like, double value) {
        if (like instanceof IntTag) return IntTag.valueOf((int) value);
        if (like instanceof FloatTag) return FloatTag.valueOf((float) value);
        if (like instanceof DoubleTag) return DoubleTag.valueOf(value);
        if (like instanceof ShortTag) return ShortTag.valueOf((short) value);
        if (like instanceof ByteTag) return ByteTag.valueOf((byte) value);
        if (like instanceof LongTag) return LongTag.valueOf((long) value);
        return DoubleTag.valueOf(value);
    }
}

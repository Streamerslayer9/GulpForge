package com.example.gulp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** What the client currently knows. Plain Java only (no client-only classes), so it's safe to load anywhere. */
public final class ClientState {
    private ClientState() { }

    /** One swallowed mob as the server describes it. */
    public record EntryInfo(int uid, String name, String typeId, float size, float hp, float maxHp, float digest) { }

    private static boolean hard;
    private static int level = 1, xp, xpNeeded = 100, points;
    private static float used, cap = 1.75f;
    private static int[] ranks = new int[Perk.values().length];
    private static List<EntryInfo> entries = new ArrayList<>();

    public static void apply(SyncPacket m) {
        hard = m.hard;
        level = m.level;
        xp = m.xp;
        xpNeeded = m.xpNeeded;
        used = m.used;
        cap = m.cap;
        points = m.points;
        ranks = m.ranks;
        entries = m.entries;
    }

    /** Set by client setup; shows the level-up chat message if the player has it turned on. */
    public static java.util.function.BiConsumer<Integer, Integer> levelUpListener;

    public static void fireLevelUp(int level, int points) {
        if (levelUpListener != null) levelUpListener.accept(level, points);
    }

    /** Plays a Gulp sound on this client. Set by client setup (see ClientSounds). */
    public interface SoundListener { void play(int type, double x, double y, double z); }

    public static SoundListener soundListener;

    public static void firePlaySound(int type, double x, double y, double z) {
        if (soundListener != null) soundListener.play(type, x, y, z);
    }

    public static boolean isHard() { return hard; }
    public static int level() { return level; }
    public static int xp() { return xp; }
    public static int xpNeeded() { return xpNeeded; }
    public static int points() { return points; }
    public static float used() { return used; }
    public static float cap() { return cap; }
    public static int rank(Perk p) { return ranks[p.ordinal()]; }
    public static List<EntryInfo> entries() { return Collections.unmodifiableList(entries); }

    /** Changes whenever the screens need to rebuild their buttons. */
    public static String signature() {
        StringBuilder sb = new StringBuilder();
        sb.append(hard).append('|').append(points).append('|').append(Arrays.toString(ranks)).append('|');
        for (EntryInfo e : entries) sb.append(e.uid()).append(',');
        return sb.toString();
    }

    // Belly fullness (0..1) for every player we can see, keyed by UUID.
    // Custom player models can read this too: ClientState.bellyAmount(uuid)
    private static final Map<UUID, Float> BELLY_TARGET = new HashMap<>();
    private static final Map<UUID, Float> BELLY_SHOWN = new HashMap<>();

    public static void setBelly(UUID id, float ratio) { BELLY_TARGET.put(id, ratio); }

    public static float bellyAmount(UUID id) { return BELLY_TARGET.getOrDefault(id, 0f); }

    /** Eases the displayed belly toward the real value so it grows/shrinks smoothly. */
    public static float smoothedBelly(UUID id) {
        float target = bellyAmount(id);
        float cur = BELLY_SHOWN.getOrDefault(id, 0f);
        cur += (target - cur) * 0.04f;
        if (Math.abs(target - cur) < 0.002f) cur = target;
        BELLY_SHOWN.put(id, cur);
        return cur;
    }
}

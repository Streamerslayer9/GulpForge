package com.example.gulp;

import java.util.HashMap;
import java.util.Map;

/**
 * Drives the belly "sliders" of your Custom Player Model (CPM), from this player's own stomach.
 *
 * The model's author adds animations to the model in the CPM editor (value layers, so they can sit anywhere
 * between "empty" and "full") and names them:
 *   belly        how full you are overall (creatures, plus items counting half as much)
 *   belly_mobs   how full of creatures you are (their volume compared to your capacity)
 *   belly_items  how full your item storage is
 * Each is set to a value between 0 and the maximum the model allows for that animation (we read the maximum from the
 * model, so any range works). An animation the model doesn't have is simply skipped. The names can be changed in
 * config/gulp-client.properties. Values ease toward their target so the belly grows and shrinks smoothly.
 * CPM then shows it to everyone, the same way it shares any other animation.
 */
public final class CpmDriver {
    private static float shownMobs;
    private static float shownItems;
    private static boolean localModelHasBelly;
    private static int ticks;
    private static final Map<String, Integer> LAST_SENT = new HashMap<>();

    private CpmDriver() { }

    /** Does the local player's CPM model have its own belly animation? (Then our built-in bulge steps aside.) */
    public static boolean localModelHandlesBelly() { return localModelHasBelly; }

    public static void reset() {
        shownMobs = 0f;
        shownItems = 0f;
        localModelHasBelly = false;
        ticks = 0;
        LAST_SENT.clear();
    }

    /** Called every client tick. */
    public static void tick() {
        if (!GulpConfig.cpmSliders || !CpmBridge.available()) {
            localModelHasBelly = false;
            return;
        }
        if (++ticks % 2 != 0) return; // every other tick is plenty

        float cap = Math.max(0.0001f, ClientState.cap());
        float mobTarget = clamp01(ClientState.used() / cap);
        float itemTarget = clamp01(ClientState.itemFill());
        shownMobs = ease(shownMobs, mobTarget);
        shownItems = ease(shownItems, itemTarget);
        float total = clamp01(shownMobs + 0.5f * shownItems);

        boolean resend = ticks % 200 == 0; // now and then, in case the player switched models
        boolean bellyExists = push(GulpConfig.cpmBellyName, total, resend);
        boolean mobsExists = push(GulpConfig.cpmMobsName, shownMobs, resend);
        boolean itemsExists = push(GulpConfig.cpmItemsName, shownItems, resend);
        // A model with ANY of the three belly animations drives its own belly, so our built-in box steps aside.
        localModelHasBelly = bellyExists || mobsExists || itemsExists;
    }

    /** @return true if the model has an animation with this name */
    private static boolean push(String name, float ratio, boolean force) {
        if (name == null || name.isBlank()) return false;
        int max = CpmBridge.maxValue(name);
        if (max < 0) {
            LAST_SENT.remove(name);
            return false; // this model has no such animation
        }
        int value = Math.max(0, Math.min(max, Math.round(ratio * max)));
        Integer last = LAST_SENT.get(name);
        if (force || last == null || last != value) {
            CpmBridge.play(name, value);
            LAST_SENT.put(name, value);
        }
        return true;
    }

    private static float ease(float shown, float target) {
        float next = shown + (target - shown) * 0.2f;
        return Math.abs(target - next) < 0.004f ? target : next;
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}

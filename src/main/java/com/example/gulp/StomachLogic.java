package com.example.gulp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.UUID;

/** All the server-side stomach rules. */
public final class StomachLogic {
    /** Hard mode only: each swallowed mob has this chance every second to struggle and hurt you. */
    private static final float STRUGGLE_CHANCE_PER_SECOND = 0.08f;

    /**
     * Once the digestion bar is full, the mob doesn't die at once: for about this long it takes steady damage
     * (you can watch its health bar drain on the stomach screen), and it dies and drops its loot when it runs out.
     * Damage per second = the mob's max health / this, so a mob at full health lasts this many seconds.
     */
    private static final float DISSOLVE_SECONDS = 8f;
    /** Struggle damage = base + per-volume x the mob's size, capped (2.0 = one heart), before armor and Iron Stomach. */
    private static final float STRUGGLE_BASE_DAMAGE = 2.0f;
    private static final float STRUGGLE_PER_VOLUME = 0.75f;
    private static final float STRUGGLE_MAX_DAMAGE = 5.0f;

    /** Extra Looting levels applied to the mob being digested right now (see ServerEvents.onLooting). */
    private static int pendingLooting = 0;

    public static int pendingLooting() { return pendingLooting; }

    private StomachLogic() { }

    // 0 swallow | 1 toggle mode | 2 release last | 3 release all
    // 4 release entry (arg = uid) | 5 digest entry now (arg = uid) | 6 buy perk (arg = perk index)
    public static void handleAction(ServerPlayer p, int action, int arg) {
        if (p == null || !p.isAlive() || p.isSpectator()) return;
        StomachManager mgr = StomachManager.get(p.getServer());
        Stomach s = mgr.of(p.getUUID());
        switch (action) {
            case 0 -> swallow(p, s, arg);
            case 1 -> {
                if (!s.hard && !hardModeAllowed()) {
                    p.displayClientMessage(Component.literal("Hard mode is turned off on this server."), true);
                } else {
                    s.hard = !s.hard;
                    p.displayClientMessage(Component.literal(s.hard
                            ? "Hard mode: contents will be digested for loot."
                            : "Soft mode: contents are held safely."), true);
                }
            }
            case 2 -> {
                if (!s.contents.isEmpty() && release(p, s.contents.get(s.contents.size() - 1))) {
                    s.contents.remove(s.contents.size() - 1);
                    sound(p, GulpSound.SPIT);
                }
            }
            case 3 -> {
                boolean any = false;
                Iterator<CompoundTag> it = s.contents.iterator();
                while (it.hasNext()) {
                    if (release(p, it.next())) {
                        it.remove();
                        any = true;
                    }
                }
                if (any) sound(p, GulpSound.SPIT);
            }
            case 4 -> {
                int i = indexOf(s, arg);
                if (i >= 0 && release(p, s.contents.get(i))) {
                    s.contents.remove(i);
                    sound(p, GulpSound.SPIT);
                }
            }
            case 5 -> {
                int i = indexOf(s, arg);
                if (i >= 0) {
                    if (hardModeAllowed()) digest(p, s, s.contents.remove(i), true);
                    else p.displayClientMessage(Component.literal("Digesting is turned off on this server."), true);
                }
            }
            case 6 -> {
                // arg = perk index in the low 8 bits, how many ranks to buy above that (0 or 1 means one)
                Perk perk = Perk.byIndex(arg & 0xFF);
                int count = Math.max(1, Math.min(100, arg >> 8));
                if (perk != null) {
                    int bought = Math.min(count, Math.min(s.points, perk.maxRank - s.rank(perk)));
                    if (bought > 0) {
                        s.ranks[perk.ordinal()] += bought;
                        s.points -= bought;
                        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                    }
                }
            }
            default -> { }
        }
        mgr.setDirty();
        sync(p, s);
    }

    private static int indexOf(Stomach s, int uid) {
        for (int i = 0; i < s.contents.size(); i++) {
            if (s.contents.get(i).getInt("Uid") == uid) return i;
        }
        return -1;
    }

    private static void swallow(ServerPlayer p, Stomach s, int targetId) {
        if (s.cooldown > 0) return;
        // Prefer what the player's client says they were looking at; fall back to our own forgiving search.
        Entity target = null;
        if (targetId >= 0) {
            Entity candidate = p.serverLevel().getEntity(targetId);
            if (candidate != null && TargetFinder.valid(candidate) && TargetFinder.withinReach(p, candidate, 2.0)) {
                target = candidate;
            }
        }
        if (target == null) target = TargetFinder.find(p, TargetFinder.REACH);
        if (!(target instanceof LivingEntity le)) {
            p.displayClientMessage(Component.literal("Nothing swallowable in reach."), true);
            return;
        }
        // Multiplayer: don't let anyone swallow (or digest) another player's tamed pet or horse.
        if (le instanceof TamableAnimal pet && pet.isTame() && pet.getOwnerUUID() != null
                && !pet.getOwnerUUID().equals(p.getUUID())) {
            p.displayClientMessage(Component.literal("That pet belongs to someone else."), true);
            return;
        }
        if (le instanceof AbstractHorse horse && horse.isTamed() && horse.getOwnerUUID() != null
                && !horse.getOwnerUUID().equals(p.getUUID())) {
            p.displayClientMessage(Component.literal("That animal belongs to someone else."), true);
            return;
        }
        float size = Math.max(0.05f, le.getBbWidth() * le.getBbWidth() * le.getBbHeight());
        if (size > s.capacity()) {
            p.displayClientMessage(Component.literal("Too big for your stomach! (needs " + Stomach.fmtVolume(size) + ", capacity " + Stomach.fmtVolume(s.capacity()) + ")"), true);
            return;
        }
        if (s.used() + size > s.capacity()) {
            p.displayClientMessage(Component.literal("Your stomach is too full."), true);
            return;
        }
        if (s.contents.size() >= Stomach.MAX_MOBS) {
            p.displayClientMessage(Component.literal("You can't hold more than " + Stomach.MAX_MOBS + " creatures at once."), true);
            return;
        }
        le.stopRiding();
        CompoundTag tag = new CompoundTag();
        if (!le.save(tag)) return;

        CompoundTag entry = new CompoundTag();
        entry.put("Entity", tag);
        entry.putFloat("Size", size);
        entry.putFloat("MaxHealth", le.getMaxHealth());
        entry.putInt("Digest", 0);
        // Bigger = slower, but gently: 13s for a cow, 22s for an iron golem, about 3 minutes for a size-500 monster.
        entry.putInt("DigestTime", 100 + (int) (150 * Math.sqrt(size)));
        // Keep the name as a translatable component, so every player sees it in their own language, and
        // modded mobs and name-tagged mobs show properly. "Name" is a plain-text backup for older saves.
        Component displayName = le.getName();
        entry.putString("Name", displayName.getString());
        entry.putString("NameJson", Component.Serializer.toJson(displayName));
        entry.putInt("Uid", p.getRandom().nextInt());

        le.discard();
        s.contents.add(entry);
        // Quick Gulp perk: looking up makes swallowing faster.
        s.cooldown = p.getXRot() < -30 ? Math.max(10, 40 - 3 * s.rank(Perk.QUICK)) : 40;
        sound(p, GulpSound.SWALLOW);
        p.displayClientMessage(Component.literal("Gulp! Swallowed ").append(displayName), true);
    }

    // ------------------------------------------------------------ per-tick

    public static void tickPlayer(StomachManager mgr, ServerPlayer p, Stomach s) {
        if (s.cooldown > 0) s.cooldown--;
        if (s.contents.isEmpty()) return;

        boolean changed = false;
        boolean dissolving = false;
        boolean hardNow = s.hard && hardModeAllowed();
        int healAmount = s.rank(Perk.HEALING);
        Iterator<CompoundTag> it = s.contents.iterator();
        while (it.hasNext()) {
            CompoundTag e = it.next();
            if (hardNow) {
                // Phase 1: the digestion bar fills up.
                int d = e.getInt("Digest");
                int digestTime = e.getInt("DigestTime");
                if (d < digestTime) {
                    d++;
                    e.putInt("Digest", d);
                }
                // Phase 2 (grace period): the bar is full, so the mob now takes damage until it runs out of health.
                if (d >= digestTime) {
                    CompoundTag ent = e.getCompound("Entity");
                    float maxHealth = Math.max(1f, e.getFloat("MaxHealth"));
                    if (!e.getBoolean("Dissolving")) {
                        e.putBoolean("Dissolving", true);
                        if (ent.getFloat("Health") <= 0f) ent.putFloat("Health", maxHealth); // no health recorded: start full
                    }
                    float health = ent.getFloat("Health") - maxHealth / (DISSOLVE_SECONDS * 20f);
                    if (health <= 0f) {
                        it.remove();
                        digest(p, s, e, false); // now it dies and drops its loot
                        changed = true;
                        continue;
                    }
                    ent.putFloat("Health", health);
                    dissolving = true;
                }
                if (p.tickCount % 20 == 0 && p.getHealth() > 4f
                        && p.getRandom().nextFloat() < STRUGGLE_CHANCE_PER_SECOND) {
                    // Hard mode only, and only now and then: the mob fights back.
                    // Bigger mobs hit harder. Armor reduces it, Iron Stomach reduces it by 5% per rank,
                    // and it can never take you below 2 hearts.
                    float strength = Math.min(STRUGGLE_MAX_DAMAGE,
                            STRUGGLE_BASE_DAMAGE + STRUGGLE_PER_VOLUME * e.getFloat("Size"));
                    float damage = strength * Math.max(0f, 1f - 0.05f * s.rank(Perk.IRON))
                            * GulpServerConfig.STRUGGLE_DAMAGE_MULTIPLIER.get().floatValue();
                    damage = Math.min(damage, p.getHealth() - 4f);
                    if (damage > 0f) {
                        p.hurt(p.level().damageSources().generic(), damage);
                        p.displayClientMessage(Component.empty().append(entryName(e)).append(" struggles inside you!"), true);
                    }
                }
            } else {
                // Healing Stomach perk
                if (healAmount > 0 && p.tickCount % 100 == 0) {
                    CompoundTag ent = e.getCompound("Entity");
                    float maxHealth = e.getFloat("MaxHealth");
                    float heal = Math.max(1f, maxHealth * 0.01f * healAmount); // 1% of its max HP per rank, at least 1
                    ent.putFloat("Health", Math.min(maxHealth, ent.getFloat("Health") + heal));
                }
            }
        }

        // Soft mode still earns XP: a completely full stomach earns a level's worth every few minutes,
        // so the fuller you are, the faster you level (and a bigger stomach means more to carry).
        if (!hardNow && p.tickCount % 20 == 0) {
            float fill = (float) Math.min(1.0, s.used() / s.capacity());
            gainXp(p, s, fill * Stomach.xpForNext(s.level) / Stomach.SOFT_SECONDS_PER_LEVEL);
        }

        if (p.tickCount % 20 == 0) mgr.setDirty();
        // Big stomachs send a lot of data, so they sync a bit less often.
        int syncEvery = s.contents.size() > 20 ? 20 : (dissolving ? 5 : 10);
        if (changed || p.tickCount % syncEvery == 0) sync(p, s);
    }

    /** @param manual true when the player used the Digest button on the stomach screen */
    private static void digest(ServerPlayer p, Stomach s, CompoundTag entry, boolean manual) {
        ServerLevel w = p.serverLevel();
        // Better Loot perk: 1 Looting level per 2 ranks (an odd rank is a coin flip for the extra level), up to Looting V.
        int lootRank = s.rank(Perk.RICH);
        int looting = lootRank / 2 + ((lootRank % 2 == 1 && p.getRandom().nextFloat() < 0.5f) ? 1 : 0);
        EntityType.create(entry.getCompound("Entity"), w).ifPresent(e -> {
            e.setUUID(UUID.randomUUID());
            e.moveTo(p.getX(), p.getY(), p.getZ(), 0f, 0f);
            w.addFreshEntity(e);
            // Credit the kill to the player so loot tables apply.
            if (e instanceof LivingEntity le) {
                pendingLooting = looting;
                try {
                    // Scaled to the mob's health so even a giant modded mob dies from it.
                    le.hurt(w.damageSources().playerAttack(p), Math.max(10000f, le.getMaxHealth() * 2f));
                    if (le.isAlive()) le.kill(); // something blocked the hit: make sure it can't walk away
                } finally {
                    pendingLooting = 0;
                }
            } else {
                e.discard();
            }
        });
        sound(p, manual ? GulpSound.DIGEST_BUTTON : GulpSound.DIGEST);
        // Gourmet perk: +5% XP per rank, digesting only.
        gainXp(p, s, Stomach.digestXp(entry.getFloat("MaxHealth")) * (1f + 0.05f * s.rank(Perk.GOURMET)));
    }

    /** @return false if the mob couldn't be recreated (so the caller keeps it instead of losing it) */
    public static boolean release(ServerPlayer p, CompoundTag entry) {
        ServerLevel w = p.serverLevel();
        Entity e = EntityType.create(entry.getCompound("Entity"), w).orElse(null);
        if (e == null) return false;
        Vec3 fwd = p.getViewVector(1.0f).scale(1.5);
        float yaw = p.getYRot() + 180f;
        e.moveTo(p.getX() + fwd.x, p.getY() + 0.5, p.getZ() + fwd.z, yaw, 0f);
        if (!w.noCollision(e)) {
            // Not enough room in front of you (a wall, say): put it where you're standing instead of inside a block.
            e.moveTo(p.getX(), p.getY() + 0.1, p.getZ(), yaw, 0f);
        }
        e.setDeltaMovement(Vec3.ZERO);
        e.fallDistance = 0f;
        if (!w.addFreshEntity(e)) {
            e.setUUID(UUID.randomUUID()); // an old copy with the same id is still around
            return w.addFreshEntity(e);
        }
        return true;
    }

    /** Called when a player dies: let everything out so nothing is lost. */
    public static void releaseAll(ServerPlayer p) {
        StomachManager mgr = StomachManager.get(p.getServer());
        Stomach s = mgr.of(p.getUUID());
        s.contents.removeIf(e -> release(p, e));
        mgr.setDirty();
        sync(p, s);
    }

    private static void levelUp(ServerPlayer p, Stomach s) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1f);
        // The message itself is shown by the client, so each player can turn it off in Settings.
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new LevelUpPacket(s.level, s.points));
    }

    // ------------------------------------------------------------ helpers

    /** Tells the player (and anyone nearby) to play a sound. Each client chooses what it actually plays. */
    private static void sound(ServerPlayer p, GulpSound type) {
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                new SoundPacket(type.ordinal(), p.getX(), p.getY(), p.getZ()));
    }

    /** The name of a swallowed mob as a Component (falls back to the plain text name for older saves). */
    public static Component entryName(CompoundTag entry) {
        if (entry.contains("NameJson")) {
            Component parsed = Component.Serializer.fromJson(entry.getString("NameJson"));
            if (parsed != null) return parsed;
        }
        return Component.literal(entry.getString("Name"));
    }

    private static boolean hardModeAllowed() {
        return GulpServerConfig.HARD_MODE_ENABLED.get();
    }

    /** Adds XP (scaled by the server's xpMultiplier, with fractions rounded randomly) and handles level-ups. */
    private static void gainXp(ServerPlayer p, Stomach s, float amount) {
        float scaled = amount * GulpServerConfig.XP_MULTIPLIER.get().floatValue();
        int whole = (int) scaled;
        if (p.getRandom().nextFloat() < scaled - whole) whole++;
        if (whole > 0 && s.addXp(whole) > 0) levelUp(p, s);
    }

    private static String fmt(double d) { return String.format("%.1f", d); }

    public static void sync(ServerPlayer p, Stomach s) {
        for (CompoundTag e : s.contents) {
            if (!e.contains("Uid")) e.putInt("Uid", p.getRandom().nextInt()); // older entries
        }
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), SyncPacket.from(s));

        // Tell this player and everyone who can see them how full the belly is.
        float ratio = (float) Math.min(1.0, s.used() / s.capacity());
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new BellyPacket(p.getUUID(), ratio));
    }
}

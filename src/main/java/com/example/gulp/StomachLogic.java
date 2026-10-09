package com.example.gulp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.UUID;

/** All the server-side stomach rules. */
public final class StomachLogic {
    /** Hard mode only: each swallowed mob has this chance every second to struggle and hurt you. */
    private static final float STRUGGLE_CHANCE_PER_SECOND = 0.08f;
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
        if (p == null) return;
        StomachManager mgr = StomachManager.get(p.getServer());
        Stomach s = mgr.of(p.getUUID());
        switch (action) {
            case 0 -> swallow(p, s, arg);
            case 1 -> {
                s.hard = !s.hard;
                p.displayClientMessage(Component.literal(s.hard
                        ? "Hard mode: contents will be digested for loot."
                        : "Soft mode: contents are held safely."), true);
            }
            case 2 -> {
                if (!s.contents.isEmpty()) {
                    release(p, s.contents.remove(s.contents.size() - 1));
                    playSfx(p, Gulp.RELEASE.get(), SoundEvents.SLIME_SQUISH);
                }
            }
            case 3 -> {
                if (!s.contents.isEmpty()) {
                    for (CompoundTag e : s.contents) release(p, e);
                    s.contents.clear();
                    playSfx(p, Gulp.RELEASE.get(), SoundEvents.SLIME_SQUISH);
                }
            }
            case 4 -> {
                int i = indexOf(s, arg);
                if (i >= 0) {
                    release(p, s.contents.remove(i));
                    playSfx(p, Gulp.RELEASE.get(), SoundEvents.SLIME_SQUISH);
                }
            }
            case 5 -> {
                int i = indexOf(s, arg);
                if (i >= 0) digest(p, s, s.contents.remove(i));
            }
            case 6 -> {
                Perk perk = Perk.byIndex(arg);
                if (perk != null && s.points > 0 && s.rank(perk) < perk.maxRank) {
                    s.ranks[perk.ordinal()]++;
                    s.points--;
                    p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
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
        float size = Math.max(0.05f, le.getBbWidth() * le.getBbWidth() * le.getBbHeight());
        if (size > s.capacity()) {
            p.displayClientMessage(Component.literal("Too big for your stomach! (needs " + fmt(size) + ", capacity " + fmt(s.capacity()) + ")"), true);
            return;
        }
        if (s.used() + size > s.capacity()) {
            p.displayClientMessage(Component.literal("Your stomach is too full."), true);
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
        entry.putInt("DigestTime", 100 + (int) (size * 100)); // bigger = slower
        entry.putString("Name", le.getName().getString());
        entry.putInt("Uid", p.getRandom().nextInt());

        le.discard();
        s.contents.add(entry);
        // Quick Gulp perk: looking up makes swallowing faster.
        s.cooldown = p.getXRot() < -30 ? Math.max(10, 40 - 6 * s.rank(Perk.QUICK)) : 40;
        playSfx(p, Gulp.SWALLOW.get(), SoundEvents.GENERIC_EAT);
        p.displayClientMessage(Component.literal("Gulp! Swallowed " + entry.getString("Name")), true);
    }

    // ------------------------------------------------------------ per-tick

    public static void tickPlayer(StomachManager mgr, ServerPlayer p, Stomach s) {
        if (s.cooldown > 0) s.cooldown--;
        if (s.contents.isEmpty()) return;

        boolean changed = false;
        int healAmount = s.rank(Perk.HEALING);
        Iterator<CompoundTag> it = s.contents.iterator();
        while (it.hasNext()) {
            CompoundTag e = it.next();
            if (s.hard) {
                int d = e.getInt("Digest") + 1;
                e.putInt("Digest", d);
                if (d >= e.getInt("DigestTime")) {
                    it.remove();
                    digest(p, s, e);
                    changed = true;
                } else if (p.tickCount % 20 == 0 && p.getHealth() > 4f
                        && p.getRandom().nextFloat() < STRUGGLE_CHANCE_PER_SECOND) {
                    // Hard mode only, and only now and then: the mob fights back.
                    // Bigger mobs hit harder. Armor reduces it, Iron Stomach reduces it by 15% per rank,
                    // and it can't kill you (it stops at 2 hearts).
                    float strength = Math.min(STRUGGLE_MAX_DAMAGE,
                            STRUGGLE_BASE_DAMAGE + STRUGGLE_PER_VOLUME * e.getFloat("Size"));
                    float damage = strength * Math.max(0f, 1f - 0.15f * s.rank(Perk.IRON));
                    if (damage > 0f) {
                        p.hurt(p.level().damageSources().generic(), damage);
                        p.displayClientMessage(Component.literal(e.getString("Name") + " struggles inside you!"), true);
                    }
                }
            } else {
                // Soft mode still earns XP (holding things is progress too).
                if (p.tickCount % 20 == 0) {
                    float amount = e.getFloat("Size") * Stomach.SOFT_XP_PER_VOLUME_SECOND;
                    int whole = (int) amount;
                    if (p.getRandom().nextFloat() < amount - whole) whole++;
                    if (whole > 0 && s.addXp(whole) > 0) levelUp(p, s);
                }
                // Healing Stomach perk
                if (healAmount > 0 && p.tickCount % 100 == 0) {
                    CompoundTag ent = e.getCompound("Entity");
                    ent.putFloat("Health", Math.min(e.getFloat("MaxHealth"), ent.getFloat("Health") + healAmount));
                }
            }
        }

        if (p.tickCount % 20 == 0) mgr.setDirty();
        if (changed || p.tickCount % 10 == 0) sync(p, s);
    }

    private static void digest(ServerPlayer p, Stomach s, CompoundTag entry) {
        ServerLevel w = p.serverLevel();
        int looting = s.rank(Perk.RICH); // Rich Digestion perk = extra Looting levels
        EntityType.create(entry.getCompound("Entity"), w).ifPresent(e -> {
            e.setUUID(UUID.randomUUID());
            e.moveTo(p.getX(), p.getY(), p.getZ(), 0f, 0f);
            w.addFreshEntity(e);
            // Credit the kill to the player so loot tables apply.
            if (e instanceof LivingEntity le) {
                pendingLooting = looting;
                try {
                    le.hurt(w.damageSources().playerAttack(p), 10000f);
                } finally {
                    pendingLooting = 0;
                }
            } else {
                e.discard();
            }
        });
        playSfx(p, Gulp.DIGEST.get(), SoundEvents.PLAYER_BURP);
        if (s.addXp(Stomach.digestXp(entry.getFloat("MaxHealth"))) > 0) levelUp(p, s);
    }

    public static void release(ServerPlayer p, CompoundTag entry) {
        ServerLevel w = p.serverLevel();
        EntityType.create(entry.getCompound("Entity"), w).ifPresent(e -> {
            Vec3 fwd = p.getViewVector(1.0f).scale(1.5);
            e.moveTo(p.getX() + fwd.x, p.getY() + 0.5, p.getZ() + fwd.z, p.getYRot() + 180f, 0f);
            w.addFreshEntity(e);
        });
    }

    /** Called when a player dies: let everything out so nothing is lost. */
    public static void releaseAll(ServerPlayer p) {
        StomachManager mgr = StomachManager.get(p.getServer());
        Stomach s = mgr.of(p.getUUID());
        for (CompoundTag e : s.contents) release(p, e);
        s.contents.clear();
        mgr.setDirty();
        sync(p, s);
    }

    private static void levelUp(ServerPlayer p, Stomach s) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1f);
        // The message itself is shown by the client, so each player can turn it off in Settings.
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new LevelUpPacket(s.level, s.points));
    }

    // ------------------------------------------------------------ helpers

    private static void playSfx(ServerPlayer p, SoundEvent custom, SoundEvent vanillaFallback) {
        p.level().playSound(null, p.blockPosition(), Gulp.CUSTOM_SFX ? custom : vanillaFallback, SoundSource.PLAYERS, 1f, 1f);
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

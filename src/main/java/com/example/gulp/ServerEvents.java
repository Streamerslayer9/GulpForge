package com.example.gulp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gulp.ID)
public final class ServerEvents {
    private ServerEvents() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) return;
        if (!(e.player instanceof ServerPlayer p)) return;
        StomachManager mgr = StomachManager.get(p.getServer());
        StomachLogic.tickPlayer(mgr, p, mgr.of(p.getUUID()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        StomachLogic.sync(p, StomachManager.get(p.getServer()).of(p.getUUID()));
    }

    // Don't lose swallowed mobs if the player dies.
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) StomachLogic.releaseAll(p);
    }

    // Rich Digestion perk: extra Looting levels, but only for mobs being digested right now.
    @SubscribeEvent
    public static void onLooting(LootingLevelEvent e) {
        int bonus = StomachLogic.pendingLooting();
        if (bonus > 0 && e.getDamageSource() != null && e.getDamageSource().getEntity() instanceof ServerPlayer) {
            e.setLootingLevel(e.getLootingLevel() + bonus);
        }
    }
}

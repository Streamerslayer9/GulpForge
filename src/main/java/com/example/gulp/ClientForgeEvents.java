package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only game events (Forge event bus): key presses. */
@Mod.EventBusSubscriber(modid = Gulp.ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (ClientModEvents.SWALLOW_KEY.consumeClick()) Net.send(0);
        while (ClientModEvents.MODE_KEY.consumeClick()) Net.send(1);
        while (ClientModEvents.RELEASE_KEY.consumeClick()) Net.send(mc.player.isShiftKeyDown() ? 3 : 2);
        while (ClientModEvents.SCREEN_KEY.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new StomachScreen());
        }
    }
}

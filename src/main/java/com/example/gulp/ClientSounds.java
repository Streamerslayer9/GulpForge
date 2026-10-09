package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Client-only: plays a Gulp sound using this player's Settings (custom/Minecraft set, per-sound on/off). */
public final class ClientSounds {
    private ClientSounds() { }

    public static void playWorld(int type, double x, double y, double z) {
        GulpSound sound = GulpSound.byIndex(type);
        if (sound == null || !GulpConfig.isOn(sound)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SoundEvent event = GulpConfig.useCustomSounds ? sound.custom() : sound.vanilla();
        if (event == null) return;
        mc.level.playLocalSound(x, y, z, event, SoundSource.PLAYERS, 1.0f, 1.0f, false);
    }
}

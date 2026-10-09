package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** The looping stomach-screen music. Stops by itself as soon as none of the three Gulp screens is open. */
public class ScreenLoopSound extends AbstractTickableSoundInstance {
    /** How loud the loop is (0.0 - 1.0). Kept low so it sits in the background as ambience. */
    private static final float VOLUME = 0.25f;

    public ScreenLoopSound() {
        super(Gulp.SCREEN_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = VOLUME;
        this.relative = true;                         // plays flat in your ears, not from a spot in the world
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Object screen = Minecraft.getInstance().screen;
        if (!(screen instanceof StomachScreen || screen instanceof PerksScreen || screen instanceof SettingsScreen)) {
            stop();
        }
    }
}

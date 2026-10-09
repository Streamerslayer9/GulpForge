package com.example.gulp;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-side settings. Saved to config/gulp-client.properties. */
public class SettingsScreen extends Screen {
    private static final int W = 250;
    private static final int H = 200;

    private Button bellyButton, levelUpButton, soundSetButton;
    private final Button[] soundButtons = new Button[GulpSound.values().length];

    public SettingsScreen() {
        super(Component.literal("Gulp Settings"));
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    private static String onOff(boolean on) { return on ? "ON" : "OFF"; }

    /** Updates every button's label (and greys out the ones that don't apply). */
    private void refresh() {
        bellyButton.setMessage(Component.literal("Belly style: " + GulpConfig.bellyStyle.label));
        levelUpButton.setMessage(Component.literal("Level-up messages: " + onOff(GulpConfig.levelUpMessages)));
        soundSetButton.setMessage(Component.literal("Sounds: " + (GulpConfig.useCustomSounds ? "Custom" : "Minecraft")));
        for (GulpSound sound : GulpSound.values()) {
            Button b = soundButtons[sound.ordinal()];
            b.setMessage(Component.literal(sound.label + ": " + onOff(GulpConfig.isOn(sound))));
            // The screen music only exists in the custom sound set.
            if (sound == GulpSound.SCREEN_LOOP) b.active = GulpConfig.useCustomSounds;
        }
    }

    @Override
    protected void init() {
        ScreenLoop.start();
        int x = left();
        int y = top();

        bellyButton = Button.builder(Component.empty(), btn -> {
            GulpConfig.bellyStyle = GulpConfig.bellyStyle.next();
            GulpConfig.save();
            refresh();
        }).bounds(x + 15, y + 22, W - 30, 20).build();

        levelUpButton = Button.builder(Component.empty(), btn -> {
            GulpConfig.levelUpMessages = !GulpConfig.levelUpMessages;
            GulpConfig.save();
            refresh();
        }).bounds(x + 15, y + 46, W - 30, 20).build();

        soundSetButton = Button.builder(Component.empty(), btn -> {
            GulpConfig.useCustomSounds = !GulpConfig.useCustomSounds;
            GulpConfig.save();
            ScreenLoop.refresh();
            refresh();
        }).bounds(x + 15, y + 70, W - 30, 20).build();

        addRenderableWidget(bellyButton);
        addRenderableWidget(levelUpButton);
        addRenderableWidget(soundSetButton);

        // One on/off button per sound: two per row, the last one full width.
        GulpSound[] all = GulpSound.values();
        int half = (W - 30 - 4) / 2;
        for (int i = 0; i < all.length; i++) {
            GulpSound sound = all[i];
            int row = i / 2;
            boolean alone = i == all.length - 1 && all.length % 2 == 1;
            int bx = alone ? x + 15 : x + 15 + (i % 2) * (half + 4);
            int bw = alone ? W - 30 : half;
            Button b = Button.builder(Component.empty(), btn -> {
                GulpConfig.setOn(sound, !GulpConfig.isOn(sound));
                GulpConfig.save();
                if (sound == GulpSound.SCREEN_LOOP) ScreenLoop.refresh();
                refresh();
            }).bounds(bx, y + 94 + row * 24, bw, 20).build();
            soundButtons[sound.ordinal()] = b;
            addRenderableWidget(b);
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), btn -> this.minecraft.setScreen(new StomachScreen()))
                .bounds(x + 8, y + H - 28, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(x + W - 68, y + H - 28, 60, 20).build());

        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int x = left();
        int y = top();

        g.fill(x, y, x + W, y + H, 0xE6141414);
        g.fill(x, y, x + W, y + 1, 0xFF666666);
        g.fill(x, y + H - 1, x + W, y + H, 0xFF666666);
        g.fill(x, y, x + 1, y + H, 0xFF666666);
        g.fill(x + W - 1, y, x + W, y + H, 0xFF666666);

        g.drawString(font, "Settings", x + 8, y + 8, 0xFFFFFF, true);

        super.render(g, mouseX, mouseY, partialTick);
    }
}

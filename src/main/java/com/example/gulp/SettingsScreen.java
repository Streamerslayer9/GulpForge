package com.example.gulp;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-side settings. Saved to config/gulp-client.properties. */
public class SettingsScreen extends Screen {
    private static final int W = 250;
    private static final int H = 120;

    public SettingsScreen() {
        super(Component.literal("Gulp Settings"));
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    @Override
    protected void init() {
        int x = left();
        int y = top();

        addRenderableWidget(Button.builder(
                        Component.literal("Belly style: " + GulpConfig.bellyStyle.label),
                        btn -> {
                            GulpConfig.bellyStyle = GulpConfig.bellyStyle.next();
                            GulpConfig.save();
                            btn.setMessage(Component.literal("Belly style: " + GulpConfig.bellyStyle.label));
                        })
                .bounds(x + 15, y + 32, W - 30, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), btn -> this.minecraft.setScreen(new StomachScreen()))
                .bounds(x + 8, y + H - 28, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(x + W - 68, y + H - 28, 60, 20).build());
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

        String hint = GulpConfig.bellyStyle == GulpConfig.BellyStyle.CLASSIC
                ? "The whole torso swells forward."
                : "Only the lower stomach swells; the chest stays flat.";
        g.drawString(font, hint, x + 15, y + 58, 0x999999, false);
        g.drawString(font, "This only changes how bellies look on your screen.", x + 15, y + 70, 0x777777, false);

        super.render(g, mouseX, mouseY, partialTick);
    }
}

package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Placeholder HUD built from plain rectangles. Swap these for your pixel art textures later. */
public final class GulpHud {
    private GulpHud() { }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        int w = 100;
        int x = screenWidth - w - 8;
        int y = screenHeight - 34;

        if (ClientState.points() > 0) {
            g.drawString(mc.font, "Perk points: " + ClientState.points()
                    + " [" + ClientModEvents.SCREEN_KEY.getTranslatedKeyMessage().getString() + "]", x, y - 24, 0xFFE64A);
        }
        g.drawString(mc.font, "Stomach Lv." + ClientState.level() + (ClientState.isHard() ? "  [HARD]" : "  [SOFT]"), x, y - 12, 0xFFFFFF);

        g.fill(x - 1, y - 1, x + w + 1, y + 9, 0xFF000000);
        g.fill(x, y, x + w, y + 8, 0xFF333333);
        int fill = (int) (w * Math.min(1f, ClientState.used() / ClientState.cap()));
        g.fill(x, y, x + fill, y + 8, ClientState.isHard() ? 0xFFC0502E : 0xFF5DB85D);
        g.drawString(mc.font, String.format("%.1f / %.1f  (%d held)", ClientState.used(), ClientState.cap(),
                ClientState.entries().size()), x, y + 11, 0xDDDDDD);

        int xpFill = (int) (w * (ClientState.xp() / (float) Math.max(1, ClientState.xpNeeded())));
        g.fill(x, y + 22, x + w, y + 25, 0xFF222222);
        g.fill(x, y + 22, x + xpFill, y + 25, 0xFFE6C84A);
    }
}

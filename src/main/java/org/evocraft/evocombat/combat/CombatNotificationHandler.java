package org.evocraft.evocombat.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = "evocombat", value = Dist.CLIENT)
public class CombatNotificationHandler {

    private static final float COMBAT_DURATION_MS = 30000.0f;

    private static boolean inCombat = false;
    private static long remainingMs = 0;
    private static long lastUpdateTime = 0;

    public static void updateCombatStatus(boolean active, long ms) {
        inCombat = active;
        remainingMs = ms;
        lastUpdateTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        inCombat = false;
        remainingMs = 0;
        lastUpdateTime = 0;
    }

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        if (!inCombat) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null || mc.options.hideGui || mc.options.renderDebug) return;
        if (mc.options.keyPlayerList.isDown()) return;

        long now = System.currentTimeMillis();
        long passed = now - lastUpdateTime;
        long currentRemaining = remainingMs - passed;

        if (currentRemaining <= 0) {
            inCombat = false;
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        double guiScale = mc.getWindow().getGuiScale();
        float hudScale = 1.0f;
        if (guiScale >= 4) hudScale = 0.70f;
        else if (guiScale >= 3) hudScale = 0.85f;

        float seconds = currentRemaining / 1000.0f;
        String title = "§c§l⚔ IN COMBAT";
        String timerLine = "§7Timer: §c" + String.format(Locale.ROOT, "%.1fs", seconds);

        int boxWidth = Math.max(128, Math.max(mc.font.width(title), mc.font.width(timerLine)) + 20);
        int boxHeight = 31;

        g.pose().pushPose();
        g.pose().scale(hudScale, hudScale, 1.0f);

        int x = Math.round((w / hudScale) - boxWidth - 8);
        int y = Math.round((h / hudScale) - boxHeight - 42);

        g.fill(x + 2, y + 2, x + boxWidth + 2, y + boxHeight + 2, 0x45000000);
        g.fillGradient(x, y, x + boxWidth, y + boxHeight, 0xD4190A0A, 0xCC0C0505);
        g.renderOutline(x, y, boxWidth, boxHeight, 0x886E2424);
        g.fill(x, y, x + 2, y + boxHeight, 0xFFFF4444);

        float progress = Math.max(0.0f, Math.min(1.0f, currentRemaining / COMBAT_DURATION_MS));
        int barWidth = Math.max(0, Math.round((boxWidth - 3) * progress));
        if (barWidth > 0) {
            g.fill(x + 2, y + boxHeight - 2, x + 2 + barWidth, y + boxHeight - 1, 0xFFFF4444);
        }

        g.drawString(mc.font, title, x + 9, y + 5, 0xFFFFFF, true);
        g.drawString(mc.font, timerLine, x + 9, y + 17, 0xFFFFFF, true);

        g.pose().popPose();
    }
}

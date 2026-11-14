package org.spectator164;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.EnumGameType;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.gui.GuiScreen;
import cpw.mods.fml.common.network.PacketDispatcher;
import org.lwjgl.input.Keyboard;

public final class ClientHooks {
    private ClientHooks() { }
    private static boolean debugKeyPending;
    private static boolean spectatorShortcutUsed;
    private static boolean pendingProfiler;
    public static boolean nextKeyboardEvent() {
        if (!Keyboard.next()) return false;
        Minecraft mc = Minecraft.getMinecraft();
        int key = Keyboard.getEventKey();
        boolean down = Keyboard.getEventKeyState();
        if (key == Keyboard.KEY_F3 && down && mc.currentScreen == null && !debugKeyPending) {
            debugKeyPending = true;
            spectatorShortcutUsed = false;
            pendingProfiler = GuiScreen.isShiftKeyDown();
        }
        if (debugKeyPending && down && mc.currentScreen == null && mc.thePlayer != null
                && ((key == Keyboard.KEY_N && Keyboard.isKeyDown(Keyboard.KEY_F3))
                    || (key == Keyboard.KEY_F3 && Keyboard.isKeyDown(Keyboard.KEY_N)))) {
            PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket("Spectator164", new byte[] { 7 }));
            spectatorShortcutUsed = true;
        }
        if (key == Keyboard.KEY_F3 && !down && debugKeyPending) {
            if (!spectatorShortcutUsed) {
                mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
                mc.gameSettings.showDebugProfilerChart = pendingProfiler;
            }
            debugKeyPending = false;
        }
        return true;
    }
    public static void setDebugInfo(GameSettings settings, boolean value) {
        if (!debugKeyPending) settings.showDebugInfo = value;
    }
    public static void setDebugProfiler(GameSettings settings, boolean value) {
        if (!debugKeyPending) settings.showDebugProfilerChart = value;
    }
    public static void setGameType(EnumGameType mode) { SpectatorMod.setClientMode(mode); }
    public static void selectHotbarSlot(InventoryPlayer inventory, int slot) {
        if (!isSpectating()) inventory.currentItem = slot;
    }
    public static boolean isSpectating() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && SpectatorMod.active(mc.thePlayer);
    }
}


package org.spectator164;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.EnumGameType;

public final class ClientHooks {
    private ClientHooks() { }
    public static void setGameType(EnumGameType mode) { SpectatorMod.setClientMode(mode); }
    public static void selectHotbarSlot(InventoryPlayer inventory, int slot) {
        if (!isSpectating()) inventory.currentItem = slot;
    }
    public static boolean isSpectating() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && SpectatorMod.active(mc.thePlayer);
    }
}


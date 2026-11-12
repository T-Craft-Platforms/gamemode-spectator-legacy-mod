package org.spectator164;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;

public final class ClientHooks {
    private ClientHooks() { }
    public static void selectHotbarSlot(InventoryPlayer inventory, int slot) {
        if (!isSpectating()) inventory.currentItem = slot;
    }
    public static boolean isSpectating() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && SpectatorMod.active(mc.thePlayer);
    }
}


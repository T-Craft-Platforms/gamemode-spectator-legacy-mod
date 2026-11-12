package org.spectator164;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;

/** The survival layout without GuiInventory's creative-mode redirects. */
public final class SpectatorInventory extends GuiInventory {
    public SpectatorInventory(EntityPlayer player) { super(player); }
    @Override public void initGui() {
        buttonList.clear();
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
    }
    @Override public void updateScreen() { }
}

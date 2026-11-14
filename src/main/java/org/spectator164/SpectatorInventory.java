package org.spectator164;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;

/** The survival layout without GuiInventory's creative-mode redirects. */
public final class SpectatorInventory extends GuiInventory {
  private static boolean drawingPlayer;

  static boolean isDrawingPlayer() {
    return drawingPlayer;
  }

  public SpectatorInventory(EntityPlayer player) {
    super(player);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    drawingPlayer = true;
    try {
      super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
    } finally {
      drawingPlayer = false;
    }
  }

  @Override
  public void initGui() {
    buttonList.clear();
    guiLeft = (width - xSize) / 2;
    guiTop = (height - ySize) / 2;
  }

  @Override
  public void updateScreen() {}
}

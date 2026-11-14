package org.spectator164;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiBeacon;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.EnumMovingObjectType;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.client.gui.Gui;
import cpw.mods.fml.common.network.PacketDispatcher;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy implements ITickHandler {
        private float speed = 0.05F;
    private final Map<Integer, Long> spectatorIds = new HashMap<Integer, Long>();
    private final ModelBiped spectatorHead = new ModelBiped();
    private Object currentWorld;
    private boolean wasActive;
    private boolean creativeMode;
    private boolean allowEdit;
    private Entity camera;
    private CameraAnchor cameraAnchor;
    @Override public void cameraTarget(int id) {
        Minecraft mc = Minecraft.getMinecraft();
        if (id == -1 || mc.theWorld == null) { stopCamera(mc); return; }
        Entity target = mc.theWorld.getEntityByID(id);
        if (target == null || target.isDead || mc.thePlayer == null || !SpectatorMod.active(mc.thePlayer)) {
            if (mc.thePlayer != null && SpectatorMod.active(mc.thePlayer)) requestCamera(-1);
            return;
        }
        camera = target;
        cameraAnchor = new CameraAnchor(mc.theWorld);
        updateCameraAnchor();
        mc.renderViewEntity = cameraAnchor;
    }
    private void stopCamera(Minecraft mc) {
        if (mc.thePlayer != null && cameraAnchor != null) {
            mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = cameraAnchor.rotationYaw;
            mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = cameraAnchor.rotationPitch;
        }
        if (mc.thePlayer != null) mc.renderViewEntity = mc.thePlayer;
        camera = null;
        cameraAnchor = null;
    }
    private void updateCameraAnchor() {
        if (camera == null || cameraAnchor == null) return;
        double x = SpectatorHooks.cameraX(camera), y = SpectatorHooks.cameraY(camera), z = SpectatorHooks.cameraZ(camera);
        cameraAnchor.setPosition(x, y, z);
        cameraAnchor.prevPosX = x + camera.prevPosX - camera.posX;
        cameraAnchor.prevPosY = y + camera.prevPosY - camera.posY;
        cameraAnchor.prevPosZ = z + camera.prevPosZ - camera.posZ;
        cameraAnchor.lastTickPosX = x + camera.lastTickPosX - camera.posX;
        cameraAnchor.lastTickPosY = y + camera.lastTickPosY - camera.posY;
        cameraAnchor.lastTickPosZ = z + camera.lastTickPosZ - camera.posZ;
        cameraAnchor.rotationYaw = SpectatorHooks.cameraYaw(camera);
        cameraAnchor.prevRotationYaw = camera instanceof EntityHanging ? cameraAnchor.rotationYaw : camera.prevRotationYaw;
        cameraAnchor.rotationPitch = camera.rotationPitch;
        cameraAnchor.prevRotationPitch = camera.prevRotationPitch;
    }
    private static final class CameraAnchor extends EntityLivingBase {
        CameraAnchor(World world) { super(world); yOffset = 1.62F; noClip = true; }
        @Override public float getEyeHeight() { return 0.0F; }
        @Override public ItemStack getHeldItem() { return null; }
        @Override public ItemStack getCurrentItemOrArmor(int slot) { return null; }
        @Override public void setCurrentItemOrArmor(int slot, ItemStack stack) { }
        @Override public ItemStack[] getLastActiveItems() { return new ItemStack[0]; }
    }
    private void requestCamera(int id) {
        PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket("Spectator164", ByteBuffer.allocate(5).put((byte) 5).putInt(id).array()));
    }
    @Override public void spectatorState(int id, boolean active) {
        if (active) spectatorIds.put(id, System.currentTimeMillis());
        else spectatorIds.remove(id);
    }
    @Override public boolean isClientSpectator(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (player == mc.thePlayer) return SpectatorMod.clientActive();
        Long seen = spectatorIds.get(player.entityId);
        return seen != null && System.currentTimeMillis() - seen.longValue() <= 5000L;
    }
    @Override public void showInventorySnapshot(byte[] data) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !SpectatorMod.active(mc.thePlayer)) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            int type = in.readUnsignedByte();
            if (type == 10) {
                int kind = in.readUnsignedByte();
                if (kind > 2) return;
                String title = Packet.readString(in, 64);
                boolean customName = in.readBoolean();
                int horseType = 0, variant = 0, age = 0;
                boolean tame = false;
                if (kind == 0) {
                    horseType = in.readUnsignedByte();
                    variant = in.readInt();
                    age = in.readInt();
                    tame = in.readBoolean();
                    if (horseType > 4) return;
                }
                int size = in.readUnsignedByte();
                if (kind == 0 ? size != 2 && size != 17 : size != (kind == 1 ? 27 : 5)) return;
                InventoryBasic snapshot = new InventoryBasic(title, customName, size);
                for (int i = 0; i < size; i++) snapshot.setInventorySlotContents(i, Packet.readItemStack(in));
                if (in.available() != 0) return;
                if (kind == 0) {
                    EntityHorse horse = new EntityHorse(mc.theWorld);
                    horse.setHorseType(horseType);
                    horse.setHorseVariant(variant);
                    horse.setGrowingAge(age);
                    horse.setHorseTamed(tame);
                    horse.setChested(size == 17);
                    horse.setHorseSaddled(snapshot.getStackInSlot(0) != null);
                    horse.func_110236_r(horse.getHorseArmorIndex(snapshot.getStackInSlot(1)));
                    mc.displayGuiScreen(new GuiScreenHorseInventory(mc.thePlayer.inventory, snapshot, horse));
                } else if (kind == 1) {
                    mc.displayGuiScreen(new GuiChest(mc.thePlayer.inventory, snapshot));
                } else {
                    mc.displayGuiScreen(new GuiHopper(mc.thePlayer.inventory, snapshot));
                }
                return;
            }
            if (type == 9) {
                int kind = in.readUnsignedByte();
                String title = Packet.readString(in, 64);
                boolean customName = in.readBoolean();
                IInventory snapshot;
                GuiScreen screen;
                if (kind == 0 || kind == 1) {
                    TileEntityDispenser dispenser = kind == 0 ? new TileEntityDropper() : new TileEntityDispenser();
                    if (customName) dispenser.setCustomName(title);
                    snapshot = dispenser;
                    screen = new GuiDispenser(mc.thePlayer.inventory, dispenser);
                } else if (kind == 2) {
                    TileEntityHopper hopper = new TileEntityHopper();
                    if (customName) hopper.setInventoryName(title);
                    snapshot = hopper;
                    screen = new GuiHopper(mc.thePlayer.inventory, hopper);
                } else if (kind == 3) {
                    TileEntityBrewingStand brewing = new TileEntityBrewingStand();
                    if (customName) brewing.func_94131_a(title);
                    brewing.setBrewTime(in.readInt());
                    snapshot = brewing;
                    screen = new GuiBrewingStand(mc.thePlayer.inventory, brewing);
                } else if (kind == 4) {
                    TileEntityBeacon beacon = new TileEntityBeacon();
                    if (customName) beacon.func_94047_a(title);
                    beacon.setLevels(in.readInt());
                    beacon.setPrimaryEffect(in.readInt());
                    beacon.setSecondaryEffect(in.readInt());
                    snapshot = beacon;
                    screen = new ReadOnlyBeacon(mc.thePlayer.inventory, beacon);
                } else return;
                for (int i = 0; i < snapshot.getSizeInventory(); i++) snapshot.setInventorySlotContents(i, Packet.readItemStack(in));
                if (in.available() == 0) mc.displayGuiScreen(screen);
                return;
            }
            String title = Packet.readString(in, 64);
            boolean customName = in.readBoolean();
            if (type == 8) {
                TileEntityFurnace furnace = new TileEntityFurnace();
                if (customName) furnace.setGuiDisplayName(title);
                furnace.furnaceBurnTime = in.readInt();
                furnace.currentItemBurnTime = in.readInt();
                furnace.furnaceCookTime = in.readInt();
                for (int i = 0; i < 3; i++) furnace.setInventorySlotContents(i, Packet.readItemStack(in));
                if (in.available() == 0) mc.displayGuiScreen(new GuiFurnace(mc.thePlayer.inventory, furnace));
                return;
            }
            if (type != 4) return;
            int size = in.readUnsignedByte();
            if (size < 9 || size > 54 || size % 9 != 0) return;
            InventoryBasic snapshot = new InventoryBasic(title, customName, size);
            for (int i = 0; i < size; i++) snapshot.setInventorySlotContents(i, Packet.readItemStack(in));
            if (in.available() == 0) mc.displayGuiScreen(new GuiChest(mc.thePlayer.inventory, snapshot));
        } catch (IOException ignored) { }
    }
    private static final class ReadOnlyBeacon extends GuiBeacon {
        ReadOnlyBeacon(net.minecraft.entity.player.InventoryPlayer inventory, TileEntityBeacon beacon) { super(inventory, beacon); }
        @Override protected void actionPerformed(GuiButton button) {
            if (button.id == -2) super.actionPerformed(button);
        }
    }
    private int menuTicks;
    private List<EntityPlayer> menuPlayers = new ArrayList<EntityPlayer>();
    private final boolean[] keyWasDown = new boolean[9];
    private List<EntityPlayer> players(Minecraft mc) {
        List<EntityPlayer> result = new ArrayList<EntityPlayer>();
        if (mc.theWorld == null) return result;
        for (Object entry : mc.theWorld.playerEntities) {
            EntityPlayer p = (EntityPlayer) entry;
            if (p != mc.thePlayer && !p.isDead) result.add(p);
        }
        return result;
    }
    private void choose(Minecraft mc, int slot) {
        if (slot < menuPlayers.size() && mc.theWorld.getEntityByID(menuPlayers.get(slot).entityId) == menuPlayers.get(slot) && !menuPlayers.get(slot).isDead) {
            byte[] data = ByteBuffer.allocate(5).put((byte) 1).putInt(menuPlayers.get(slot).entityId).array();
            PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket("Spectator164", data));
            menuTicks = 0;
        }
    }
        public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        TickRegistry.registerTickHandler(this, Side.CLIENT);
        TickRegistry.registerTickHandler(new ITickHandler() {
            public void tickStart(EnumSet<TickType> type, Object... data) { updateCameraAnchor(); }
            public void tickEnd(EnumSet<TickType> type, Object... data) { }
            public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
            public String getLabel() { return "Spectator164Camera"; }
        }, Side.CLIENT);
    }
    private ItemRenderer originalRenderer;
    private ItemRenderer spectatorRenderer;
    public void tickStart(EnumSet<TickType> types, Object... data) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer == null) return;
        boolean worldChanged = currentWorld != mc.theWorld;
        if (worldChanged) {
            stopCamera(mc);
            spectatorIds.clear();
            currentWorld = mc.theWorld;
            if (mc.theWorld == null) SpectatorMod.clearClientState();
        }
        boolean active = mc.thePlayer != null && SpectatorMod.active(mc.thePlayer);
        if (camera != null) {
            if (!active || camera.isDead || camera.worldObj != mc.theWorld || mc.theWorld.getEntityByID(camera.entityId) != camera) {
                if (active) requestCamera(-1);
                stopCamera(mc);
            } else if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
                requestCamera(-1);
                stopCamera(mc);
            } else {
                mc.thePlayer.setPositionAndRotation(SpectatorHooks.cameraX(camera), SpectatorHooks.cameraY(camera) - mc.thePlayer.getEyeHeight(), SpectatorHooks.cameraZ(camera),
                        SpectatorHooks.cameraYaw(camera), camera.rotationPitch);
                mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
            }
        }
        boolean creative = mc.playerController != null && mc.playerController.isInCreativeMode();
        boolean canEdit = mc.thePlayer != null && mc.thePlayer.capabilities.allowEdit;
        if (worldChanged || active != wasActive || creative != creativeMode || canEdit != allowEdit) {
            speed = 0.05F;
            if (mc.thePlayer != null) mc.thePlayer.capabilities.setFlySpeed(speed);
        }
        wasActive = active;
        creativeMode = creative;
        allowEdit = canEdit;
                if (active) {
            float effectiveSpeed = speed * (mc.thePlayer.isSprinting() ? 2.0F : 1.0F);
            mc.thePlayer.capabilities.setFlySpeed(effectiveSpeed);
            mc.thePlayer.onGround = false;
            if (mc.currentScreen == null && camera == null) {
                int vertical = (GameSettings.isKeyDown(mc.gameSettings.keyBindJump) ? 1 : 0)
                             - (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) ? 1 : 0);
                mc.thePlayer.motionY += vertical * 0.15D * (effectiveSpeed / 0.05D - 1.0D);
            }
        }
        if (!active || mc.currentScreen != null) menuTicks = 0;
        if (menuTicks > 0) menuTicks--;
        for (int i = 0; i < 9; i++) {
            boolean down = Keyboard.isKeyDown(Keyboard.KEY_1 + i);
            if (active && mc.currentScreen == null && down && !keyWasDown[i]) {
                if (menuTicks > 0) { choose(mc, i); if (menuTicks > 0) menuTicks = 100; }
                else { menuPlayers = players(mc); menuTicks = 100; }
            }
            keyWasDown[i] = down;
        }
        if (active && mc.entityRenderer.itemRenderer != spectatorRenderer) {
            originalRenderer = mc.entityRenderer.itemRenderer;
            spectatorRenderer = new SpectatorItemRenderer(mc);
            mc.entityRenderer.itemRenderer = spectatorRenderer;
        } else if (!active && spectatorRenderer != null) {
            if (mc.entityRenderer.itemRenderer == spectatorRenderer) mc.entityRenderer.itemRenderer = originalRenderer;
            spectatorRenderer = null;
            originalRenderer = null;
        }
    }
    public void tickEnd(EnumSet<TickType> types, Object... data) { }
    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.CLIENT); }
    public String getLabel() { return "Spectator164Client"; }
    private static final class SpectatorItemRenderer extends ItemRenderer {
        SpectatorItemRenderer(Minecraft mc) { super(mc); }
        @Override public void renderOverlays(float partialTicks) { }
        @Override public void renderItemInFirstPerson(float partialTicks) { }
    }

    @ForgeSubscribe public void openGui(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && SpectatorMod.active(mc.thePlayer) && (event.gui instanceof GuiContainerCreative || (event.gui instanceof GuiInventory && !(event.gui instanceof SpectatorInventory)))) {
            event.gui = new SpectatorInventory(mc.thePlayer);
        }
    }

    @ForgeSubscribe public void mouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null || mc.currentScreen != null || !SpectatorMod.active(mc.thePlayer)) return;
        if (event.button == 1 && event.buttonstate) {
            MovingObjectPosition target = mc.objectMouseOver;
            if (target != null && target.typeOfHit == EnumMovingObjectType.TILE) {
                byte[] data = ByteBuffer.allocate(13).put((byte) 3).putInt(target.blockX).putInt(target.blockY).putInt(target.blockZ).array();
                PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket("Spectator164", data));
            } else if (camera == null && target != null && target.typeOfHit == EnumMovingObjectType.ENTITY && target.entityHit != null) {
                byte[] data = ByteBuffer.allocate(5).put((byte) 11).putInt(target.entityHit.entityId).array();
                PacketDispatcher.sendPacketToServer(PacketDispatcher.getPacket("Spectator164", data));
            }
            event.setCanceled(true);
            return;
        }
        if (event.button == 0 && event.buttonstate) {
            MovingObjectPosition target = mc.objectMouseOver;
            if (camera == null && target != null && target.typeOfHit == EnumMovingObjectType.ENTITY && target.entityHit != null) {
                requestCamera(target.entityHit.entityId);
            }
            event.setCanceled(true);
            return;
        }
        if (event.dwheel == 0) return;
        speed = Math.max(0.005F, Math.min(0.5F, speed + (event.dwheel > 0 ? 0.005F : -0.005F)));
        mc.thePlayer.capabilities.setFlySpeed(speed);
        event.setCanceled(true);
    }

    @ForgeSubscribe public void renderSpectatorHead(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = event.entityPlayer;
        Long seen = spectatorIds.get(player.entityId);
        boolean self = player == mc.thePlayer && SpectatorMod.active(player);
        boolean inventoryPreview = self && SpectatorInventory.isDrawingPlayer();
        if (self && camera != null && !inventoryPreview) { event.setCanceled(true); return; }
        if (mc.thePlayer == null || !SpectatorMod.active(mc.thePlayer) || !(player instanceof AbstractClientPlayer)
                || (!self && (seen == null || System.currentTimeMillis() - seen.longValue() > 5000L))) return;
        float t = event.partialRenderTick;
        double x = inventoryPreview ? 0 : player.lastTickPosX + (player.posX - player.lastTickPosX) * t - RenderManager.renderPosX;
        double y = inventoryPreview ? 0 : player.lastTickPosY + (player.posY - player.lastTickPosY) * t - RenderManager.renderPosY;
        double z = inventoryPreview ? 0 : player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * t - RenderManager.renderPosZ;
        float yaw = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * t;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * t;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glTranslated(x, y + 1.5D - player.yOffset, z);
        GL11.glRotatef(180.0F - yaw, 0, 1, 0);
        GL11.glScalef(-1, -1, 1);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glColor4f(1, 1, 1, 0.35F);
        mc.renderEngine.bindTexture(((AbstractClientPlayer) player).getLocationSkin());
        spectatorHead.bipedHead.rotateAngleX = pitch * (float)Math.PI / 180.0F;
        spectatorHead.bipedHeadwear.rotateAngleX = spectatorHead.bipedHead.rotateAngleX;
        spectatorHead.bipedHead.render(0.0625F);
        spectatorHead.bipedHeadwear.render(0.0625F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
        event.setCanceled(true);
    }
    @ForgeSubscribe public void hideCameraTarget(RenderLivingEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (camera == event.entity && mc.gameSettings.thirdPersonView == 0) event.setCanceled(true);
    }
    @ForgeSubscribe public void menu(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || menuTicks <= 0 || mc.thePlayer == null || !SpectatorMod.active(mc.thePlayer)) return;
        List<EntityPlayer> entries = menuPlayers;
        int width = event.resolution.getScaledWidth();
        int height = event.resolution.getScaledHeight();
        int left = width / 2 - 90;
        Gui.drawRect(left - 1, height - 25, left + 181, height - 2, 0x99000000);
        for (int i = 0; i < 9; i++) {
            int x = left + i * 20;
            Gui.drawRect(x + 1, height - 23, x + 19, height - 4, 0x88606060);
            if (i < entries.size() && entries.get(i) instanceof AbstractClientPlayer) {
                mc.renderEngine.bindTexture(((AbstractClientPlayer) entries.get(i)).getLocationSkin());
                GL11.glColor4f(1, 1, 1, 1);
                drawSkinLayer(x + 2, height - 22, 8, 8);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                drawSkinLayer(x + 2, height - 22, 40, 8);
                GL11.glDisable(GL11.GL_BLEND);
            } else {
                mc.fontRenderer.drawStringWithShadow(Integer.toString(i + 1), x + 6, height - 19, 0xffffff);
            }
        }
        for (int i = 0; i < 9 && i < entries.size(); i++) {
            mc.fontRenderer.drawStringWithShadow((i + 1) + ": " + entries.get(i).username, left, height - 37 - i * 10, 0xffffff);
        }
        if (entries.isEmpty()) mc.fontRenderer.drawStringWithShadow("No nearby players", left, height - 37, 0xffffff);
    }
    private void drawSkinLayer(int x, int y, int u, int v) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + 16, 0, u / 64.0D, (v + 8) / 32.0D);
        t.addVertexWithUV(x + 16, y + 16, 0, (u + 8) / 64.0D, (v + 8) / 32.0D);
        t.addVertexWithUV(x + 16, y, 0, (u + 8) / 64.0D, v / 32.0D);
        t.addVertexWithUV(x, y, 0, u / 64.0D, v / 32.0D);
        t.draw();
    }
    @ForgeSubscribe public void blockHighlight(DrawBlockHighlightEvent event) {
        if (SpectatorMod.active(event.player)) event.setCanceled(true);
    }
    private boolean interactiveTarget(Minecraft mc) {
        MovingObjectPosition target = mc.objectMouseOver;
        if (target == null) return false;
        if (target.typeOfHit == EnumMovingObjectType.ENTITY) return target.entityHit != null;
        if (target.typeOfHit != EnumMovingObjectType.TILE || mc.theWorld == null) return false;
        int id = mc.theWorld.getBlockId(target.blockX, target.blockY, target.blockZ);
        Block block = id > 0 ? Block.blocksList[id] : null;
        return block instanceof BlockChest || block instanceof BlockEnderChest
                || mc.theWorld.getBlockTileEntity(target.blockX, target.blockY, target.blockZ) instanceof IInventory;
    }
    @ForgeSubscribe public void overlay(RenderGameOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !SpectatorMod.active(mc.thePlayer)) return;
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && (camera != null || !interactiveTarget(mc))) {
            event.setCanceled(true);
            return;
        }
        switch (event.type) {
            case HOTBAR: case HEALTH: case FOOD: case ARMOR: case AIR: case EXPERIENCE: case HEALTHMOUNT: case JUMPBAR:
                event.setCanceled(true);
                break;
            default: break;
        }
    }
}









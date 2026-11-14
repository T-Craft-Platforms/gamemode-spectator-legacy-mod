package org.spectator164;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartHopper;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.event.Event.Result;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.IPlayerTracker;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.command.CommandGameMode;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.world.EnumGameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.EnumHelper;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

@Mod(modid="spectator164", name="Spectator 1.6.4", version="0.1.0-dev")
@NetworkMod(clientSideRequired=true, serverSideRequired=true, channels={"Spectator164"}, packetHandler=SpectatorMod.Packets.class)
public final class SpectatorMod implements ITickHandler {
    @SidedProxy(clientSide="org.spectator164.ClientProxy", serverSide="org.spectator164.CommonProxy")
    public static CommonProxy proxy;
    private static final String KEY = "spectator164Active";
    private static boolean clientActive;
    private static EnumGameType spectatorMode;
    private static final Map<EntityPlayerMP, Entity> cameras = new WeakHashMap<EntityPlayerMP, Entity>();
    static boolean clientActive() { return clientActive; }
    static void clearClientState() { clientActive = false; }
    static void setClientMode(EnumGameType mode) { clientActive = mode.getID() == 3; }
    static boolean active(EntityPlayer p) { return p.worldObj.isRemote ? proxy.isClientSpectator(p) : p instanceof EntityPlayerMP && ((EntityPlayerMP)p).theItemInWorldManager.getGameType().getID() == 3; }
    @EventHandler public void preInit(FMLPreInitializationEvent event) {
        spectatorMode = (EnumGameType) EnumHelper.addEnum(EnumGameType.class, "SPECTATOR", new Class<?>[] { Integer.TYPE, String.class }, new Object[] { Integer.valueOf(3), "spectator" });
        if (spectatorMode == null || EnumGameType.getByID(3) != spectatorMode || !"spectator".equals(spectatorMode.getName()))
            throw new IllegalStateException("Could not register spectator game mode");
        LanguageRegistry.instance().addStringLocalization("gameMode.spectator", "en_US", "Spectator Mode");
    }
    @EventHandler public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        proxy.init();
        GameRegistry.registerPlayerTracker(new PlayerTracker());
        TickRegistry.registerTickHandler(this, Side.SERVER);
        TickRegistry.registerTickHandler(this, Side.CLIENT);
    }
    @EventHandler public void serverStart(FMLServerStartingEvent e) { e.registerServerCommand(new Command()); }
    public static class Command extends CommandGameMode {
        @Override protected EnumGameType getGameModeFromCommand(ICommandSender sender, String name) {
            return "spectator".equalsIgnoreCase(name) ? spectatorMode : super.getGameModeFromCommand(sender, name);
        }
        @Override public java.util.List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) return getListOfStringsMatchingLastWord(args, new String[] { "survival", "creative", "adventure", "spectator" });
            return super.addTabCompletionOptions(sender, args);
        }
    }
    public static void configureCapabilities(EnumGameType mode, PlayerCapabilities capabilities) {
        if (mode.getID() != 3) return;
        capabilities.allowFlying = true;
        capabilities.isFlying = true;
        capabilities.disableDamage = true;
        capabilities.isCreativeMode = false;
        capabilities.allowEdit = false;
    }
    public static void changingMode(ItemInWorldManager manager, EnumGameType next) {
        EntityPlayerMP p = manager.thisPlayerMP;
        if (p == null) return;
        int oldId = manager.getGameType().getID();
        boolean enabled = next.getID() == 3;
        if (enabled && oldId != 3 && oldId >= 0) p.getEntityData().setInteger("spectator164PreviousMode", oldId);
        if (oldId == 3 && !enabled) { stopCamera(p); p.noClip = false; p.setInvisible(false); }
        if (enabled) { p.noClip = true; p.fallDistance = 0; p.setInvisible(true); p.clearActivePotions(); }
        if (p.playerNetServerHandler != null && (oldId == 3) != enabled) {
            sync(p, enabled);
            broadcastState(p, enabled);
        }
    }
    private static void cameraPacket(EntityPlayerMP p, int id) {
        PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", ByteBuffer.allocate(5).put((byte) 6).putInt(id).array()), (Player) p);
    }
    private static void stopCamera(EntityPlayerMP p) {
        Entity target = cameras.remove(p);
        if (target != null) {
            p.rotationYaw = p.prevRotationYaw = SpectatorHooks.cameraYaw(target);
            p.rotationPitch = p.prevRotationPitch = target.rotationPitch;
            cameraPacket(p, -1);
        }
    }
    static void sync(EntityPlayerMP p, boolean enabled) { PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", new byte[]{(byte)(enabled ? 1 : 0)}), (Player)p); }
    static void broadcastState(EntityPlayerMP p, boolean enabled) {
        byte[] data = ByteBuffer.allocate(6).put((byte) 2).putInt(p.entityId).put((byte) (enabled ? 1 : 0)).array();
        PacketDispatcher.sendPacketToAllInDimension(PacketDispatcher.getPacket("Spectator164", data), p.dimension);
    }
    private static void sendEntitySnapshot(EntityPlayerMP player, int id) {
        Entity entity = player.worldObj.getEntityByID(id);
        if (entity == null || entity.isDead || player.getDistanceSqToEntity(entity) > 36.0D) return;
        IInventory inventory;
        int kind;
        if (entity.getClass() == EntityHorse.class) {
            kind = 0;
            inventory = ReflectionHelper.getPrivateValue(EntityHorse.class, (EntityHorse) entity, "horseChest", "field_110296_bG");
        } else if (entity.getClass() == EntityMinecartChest.class) {
            kind = 1;
            inventory = (IInventory) entity;
        } else if (entity.getClass() == EntityMinecartHopper.class) {
            kind = 2;
            inventory = (IInventory) entity;
        } else return;
        if (inventory == null) return;
        int size = inventory.getSizeInventory();
        if (kind == 0 ? size != 2 && size != 17 : size != (kind == 1 ? 27 : 5)) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(10);
            out.writeByte(kind);
            String title = kind == 0 ? entity.getEntityName() : inventory.getInvName();
            if (title == null) title = "Container";
            if (title.length() > 64) title = title.substring(0, 64);
            Packet.writeString(title, out);
            out.writeBoolean(kind == 0 || inventory.isInvNameLocalized());
            if (kind == 0) {
                EntityHorse horse = (EntityHorse) entity;
                out.writeByte(horse.getHorseType());
                out.writeInt(horse.getHorseVariant());
                out.writeInt(horse.getGrowingAge());
                out.writeBoolean(horse.isTame());
            }
            out.writeByte(size);
            for (int i = 0; i < size; i++) Packet.writeItemStack(inventory.getStackInSlot(i), out);
            out.flush();
            if (bytes.size() <= 32760) PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", bytes.toByteArray()), (Player) player);
        } catch (IOException ignored) { }
    }
    private static void sendSnapshot(EntityPlayerMP player, int x, int y, int z) {
        if (!player.worldObj.blockExists(x, y, z) || player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) > 64.0D) return;
        int id = player.worldObj.getBlockId(x, y, z);
        Block block = id > 0 ? Block.blocksList[id] : null;
        TileEntity tile = player.worldObj.getBlockTileEntity(x, y, z);
        IInventory inventory = null;
        if (block instanceof BlockChest) inventory = ((BlockChest) block).getInventory(player.worldObj, x, y, z);
        else if (block instanceof BlockEnderChest) inventory = player.getInventoryEnderChest();
        else if (tile instanceof IInventory && tile.getClass().getName().startsWith("net.minecraft.tileentity.")) inventory = (IInventory) tile;
        if (inventory == null) return;
        int kind = tile instanceof TileEntityDropper ? 0 : tile instanceof TileEntityDispenser ? 1
                : tile instanceof TileEntityHopper ? 2 : tile instanceof TileEntityBrewingStand ? 3
                : tile instanceof TileEntityBeacon ? 4 : -1;
        if (kind >= 0) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeByte(9);
                out.writeByte(kind);
                String title = inventory.getInvName();
                if (title == null) title = "Container";
                if (title.length() > 64) title = title.substring(0, 64);
                Packet.writeString(title, out);
                out.writeBoolean(inventory.isInvNameLocalized());
                if (kind == 3) out.writeInt(((TileEntityBrewingStand) tile).getBrewTime());
                if (kind == 4) {
                    TileEntityBeacon beacon = (TileEntityBeacon) tile;
                    out.writeInt(beacon.getLevels());
                    out.writeInt(beacon.getPrimaryEffect());
                    out.writeInt(beacon.getSecondaryEffect());
                }
                for (int i = 0; i < inventory.getSizeInventory(); i++) Packet.writeItemStack(inventory.getStackInSlot(i), out);
                out.flush();
                if (bytes.size() <= 32760) PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", bytes.toByteArray()), (Player) player);
            } catch (IOException ignored) { }
            return;
        }
        if (tile instanceof TileEntityFurnace) {
            try {
                TileEntityFurnace furnace = (TileEntityFurnace) tile;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeByte(8);
                Packet.writeString(furnace.getInvName(), out);
                out.writeBoolean(furnace.isInvNameLocalized());
                out.writeInt(furnace.furnaceBurnTime);
                out.writeInt(furnace.currentItemBurnTime);
                out.writeInt(furnace.furnaceCookTime);
                for (int i = 0; i < 3; i++) Packet.writeItemStack(furnace.getStackInSlot(i), out);
                out.flush();
                if (bytes.size() <= 32760) PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", bytes.toByteArray()), (Player) player);
            } catch (IOException ignored) { }
            return;
        }
        int count = Math.min(inventory.getSizeInventory(), 54);
        int size = Math.max(9, ((count + 8) / 9) * 9);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(4);
            String title = inventory.getInvName();
            if (title == null) title = "Container";
            if (title.length() > 64) title = title.substring(0, 64);
            Packet.writeString(title, out);
            out.writeBoolean(inventory.isInvNameLocalized());
            out.writeByte(size);
            for (int i = 0; i < size; i++) Packet.writeItemStack(i < count ? inventory.getStackInSlot(i) : null, out);
            out.flush();
            if (bytes.size() <= 32760) PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", bytes.toByteArray()), (Player) player);
        } catch (IOException ignored) { }
    }
    public static class Packets implements IPacketHandler {
        public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
                        if (packet.data == null) return;
            EntityPlayer p = (EntityPlayer) player;
            if (p.worldObj.isRemote && packet.data.length == 1) {
                                clientActive = packet.data[0] != 0;
            } else if (p.worldObj.isRemote && packet.data.length == 6 && packet.data[0] == 2) {
                proxy.spectatorState(ByteBuffer.wrap(packet.data, 1, 4).getInt(), packet.data[5] != 0);
            } else if (p.worldObj.isRemote && packet.data.length > 1 && (packet.data[0] == 4 || packet.data[0] == 8 || packet.data[0] == 9 || packet.data[0] == 10)) {
                proxy.showInventorySnapshot(packet.data);
            } else if (p.worldObj.isRemote && packet.data.length == 5 && packet.data[0] == 6) {
                proxy.cameraTarget(ByteBuffer.wrap(packet.data, 1, 4).getInt());
            } else if (!p.worldObj.isRemote && packet.data.length == 1 && packet.data[0] == 7) {
                EntityPlayerMP playerMP = (EntityPlayerMP) p;
                int mode = active(p) ? p.getEntityData().getInteger("spectator164PreviousMode") : 3;
                if (active(p) && (mode < 0 || mode > 2)) mode = 0;
                playerMP.mcServer.getCommandManager().executeCommand(p, "gamemode " + mode);
            } else if (!p.worldObj.isRemote && packet.data.length == 5 && packet.data[0] == 5 && active(p)) {
                EntityPlayerMP spectator = (EntityPlayerMP) p;
                int id = ByteBuffer.wrap(packet.data, 1, 4).getInt();
                if (id == -1) stopCamera(spectator);
                else {
                    Entity target = p.worldObj.getEntityByID(id);
                    if (target != null && target != p && !target.isDead && !SpectatorHooks.isSpectator(target)
                            && p.getDistanceSqToEntity(target) <= 36.0D) {
                        cameras.put(spectator, target);
                        cameraPacket(spectator, id);
                    }
                }
            } else if (!p.worldObj.isRemote && packet.data.length == 5 && packet.data[0] == 11 && active(p)) {
                sendEntitySnapshot((EntityPlayerMP) p, ByteBuffer.wrap(packet.data, 1, 4).getInt());
            } else if (!p.worldObj.isRemote && packet.data.length == 13 && packet.data[0] == 3 && active(p)) {
                ByteBuffer request = ByteBuffer.wrap(packet.data, 1, 12);
                sendSnapshot((EntityPlayerMP) p, request.getInt(), request.getInt(), request.getInt());
            } else if (!p.worldObj.isRemote && packet.data.length == 5 && packet.data[0] == 1 && active(p)) {
                int id = ByteBuffer.wrap(packet.data, 1, 4).getInt();
                Entity target = p.worldObj.getEntityByID(id);
                if (target instanceof EntityPlayerMP && target != p && !target.isDead) {
                    EntityPlayerMP spectator = (EntityPlayerMP) p;
                    stopCamera(spectator);
                    spectator.playerNetServerHandler.setPlayerLocation(target.posX, target.posY, target.posZ, target.rotationYaw, target.rotationPitch);
                }
            }
        }
    }
    private static void restore(EntityPlayer p) {
        if (!active(p)) return;
        p.noClip = true;
        p.fallDistance = 0;
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.capabilities.disableDamage = true;
        p.setInvisible(true);
    }
    @ForgeSubscribe public void joinWorld(EntityJoinWorldEvent event) {
        if (!event.world.isRemote && event.entity instanceof EntityPlayer) restore((EntityPlayer) event.entity);
    }
    public static class PlayerTracker implements IPlayerTracker {
        private void update(EntityPlayer player) {
            EntityPlayerMP p = (EntityPlayerMP) player;
            restore(p);
            p.sendPlayerAbilities();
            sync(p, active(p));
            broadcastState(p, active(p));
        }
        public void onPlayerLogin(EntityPlayer player) {
            EntityPlayerMP p = (EntityPlayerMP) player;
            if (p.getEntityData().getBoolean(KEY)) {
                p.getEntityData().removeTag(KEY);
                if (!active(p) && !p.mcServer.getForceGamemode()) p.setGameType(spectatorMode);
            }
            update(p);
        }
        public void onPlayerChangedDimension(EntityPlayer player) { stopCamera((EntityPlayerMP) player); update(player); }
        public void onPlayerRespawn(EntityPlayer player) { stopCamera((EntityPlayerMP) player); update(player); }
        public void onPlayerLogout(EntityPlayer player) { cameras.remove((EntityPlayerMP) player); }
    }
    public void tickStart(EnumSet<TickType> type, Object... data) {
        EntityPlayer p=(EntityPlayer)data[0];
        if (!p.worldObj.isRemote && p.ticksExisted % 40 == 0) sync((EntityPlayerMP)p, active(p));
        if (active(p)) {
            p.noClip=true; p.fallDistance=0; p.extinguish(); p.capabilities.isFlying=true; p.capabilities.allowFlying=true;
            if (!p.worldObj.isRemote) { p.setInvisible(true); if (p.ticksExisted % 40 == 0) broadcastState((EntityPlayerMP)p, true); }
        } else if (p.worldObj.isRemote) { p.noClip=false; }
    }
    public void tickEnd(EnumSet<TickType> type, Object... data) {
        EntityPlayer p = (EntityPlayer) data[0];
        if (p.worldObj.isRemote) return;
        EntityPlayerMP spectator = (EntityPlayerMP) p;
        Entity target = cameras.get(spectator);
        if (target == null) return;
        if (!active(p) || target.isDead || target.worldObj != p.worldObj || SpectatorHooks.isSpectator(target)) {
            stopCamera(spectator);
            return;
        }
        double x = SpectatorHooks.cameraX(target), y = SpectatorHooks.cameraY(target) - p.getEyeHeight(), z = SpectatorHooks.cameraZ(target);
        if (p.getDistanceSq(x, y, z) > 64.0D) {
            spectator.playerNetServerHandler.setPlayerLocation(x, y, z, SpectatorHooks.cameraYaw(target), target.rotationPitch);
        } else {
            p.setPositionAndRotation(x, y, z, SpectatorHooks.cameraYaw(target), target.rotationPitch);
        }
        p.motionX = p.motionY = p.motionZ = 0;
    }
    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.PLAYER); }
    public String getLabel() { return "Spectator164"; }
    @ForgeSubscribe public void interact(PlayerInteractEvent e) {
        if (!active(e.entityPlayer)) return;
        e.setCanceled(true);
    }
    @ForgeSubscribe public void attack(AttackEntityEvent e) { if (active(e.entityPlayer)) e.setCanceled(true); }
    @ForgeSubscribe public void interactEntity(EntityInteractEvent e) { if (active(e.entityPlayer)) e.setCanceled(true); }
    @ForgeSubscribe public void pickup(EntityItemPickupEvent e) { if (active(e.entityPlayer)) e.setCanceled(true); }
    @ForgeSubscribe public void damage(LivingAttackEvent e) { if (e.entityLiving instanceof EntityPlayer && active((EntityPlayer)e.entityLiving)) e.setCanceled(true); }
}










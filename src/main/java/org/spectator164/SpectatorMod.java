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
import net.minecraft.entity.Entity;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.event.Event.Result;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
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
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.world.EnumGameType;
import net.minecraftforge.common.MinecraftForge;
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
    private static final Map<EntityPlayerMP, Entity> cameras = new WeakHashMap<EntityPlayerMP, Entity>();
    static boolean clientActive() { return clientActive; }
    static void clearClientState() { clientActive = false; }
    static boolean active(EntityPlayer p) { return p.worldObj.isRemote ? proxy.isClientSpectator(p) : p.getEntityData().getBoolean(KEY); }
    @EventHandler public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        proxy.init();
        GameRegistry.registerPlayerTracker(new PlayerTracker());
        TickRegistry.registerTickHandler(this, Side.SERVER);
        TickRegistry.registerTickHandler(this, Side.CLIENT);
    }
    @EventHandler public void serverStart(FMLServerStartingEvent e) { e.registerServerCommand(new Command()); }
    public static class Command extends CommandBase {
        public int compareTo(Object other) { return getCommandName().compareTo(((net.minecraft.command.ICommand)other).getCommandName()); }
        public String getCommandName() { return "spectator"; }
        public String getCommandUsage(ICommandSender sender) { return "/spectator <on|off>"; }
        public int getRequiredPermissionLevel() { return 2; }
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || !(args[0].equals("on") || args[0].equals("off"))) throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
            EntityPlayerMP p = getCommandSenderAsPlayer(sender);
            boolean enabled = args[0].equals("on");
            if (enabled && !active(p)) p.getEntityData().setInteger("spectator164PreviousMode", p.theItemInWorldManager.getGameType().getID());
            if (!enabled && active(p)) p.setGameType(EnumGameType.getByID(p.getEntityData().getInteger("spectator164PreviousMode")));
            if (!enabled) stopCamera(p);
            p.getEntityData().setBoolean(KEY, enabled);
            p.noClip = enabled;
            p.setInvisible(enabled);
            if (enabled) { p.capabilities.allowFlying=true; p.capabilities.isFlying=true; p.capabilities.disableDamage=true; }
            p.sendPlayerAbilities();
            sync(p, enabled);
            broadcastState(p, enabled);
            p.addChatMessage(enabled ? "Spectator enabled" : "Spectator disabled");
        }
    }
    private static void cameraPacket(EntityPlayerMP p, int id) {
        PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", ByteBuffer.allocate(5).put((byte) 6).putInt(id).array()), (Player) p);
    }
    private static void stopCamera(EntityPlayerMP p) {
        if (cameras.remove(p) != null) cameraPacket(p, -1);
    }
    static void sync(EntityPlayerMP p, boolean enabled) { PacketDispatcher.sendPacketToPlayer(PacketDispatcher.getPacket("Spectator164", new byte[]{(byte)(enabled ? 1 : 0)}), (Player)p); }
    static void broadcastState(EntityPlayerMP p, boolean enabled) {
        byte[] data = ByteBuffer.allocate(6).put((byte) 2).putInt(p.entityId).put((byte) (enabled ? 1 : 0)).array();
        PacketDispatcher.sendPacketToAllInDimension(PacketDispatcher.getPacket("Spectator164", data), p.dimension);
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
            } else if (p.worldObj.isRemote && packet.data.length > 1 && packet.data[0] == 4) {
                proxy.showInventorySnapshot(packet.data);
            } else if (p.worldObj.isRemote && packet.data.length == 5 && packet.data[0] == 6) {
                proxy.cameraTarget(ByteBuffer.wrap(packet.data, 1, 4).getInt());
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
        public void onPlayerLogin(EntityPlayer player) { update(player); }
        public void onPlayerChangedDimension(EntityPlayer player) { stopCamera((EntityPlayerMP) player); update(player); }
        public void onPlayerRespawn(EntityPlayer player) { stopCamera((EntityPlayerMP) player); update(player); }
        public void onPlayerLogout(EntityPlayer player) { cameras.remove((EntityPlayerMP) player); }
    }
    public void tickStart(EnumSet<TickType> type, Object... data) {
        EntityPlayer p=(EntityPlayer)data[0];
        if (!p.worldObj.isRemote && p.ticksExisted % 40 == 0) sync((EntityPlayerMP)p, active(p));
        if (active(p)) {
            p.noClip=true; p.fallDistance=0; p.capabilities.isFlying=true; p.capabilities.allowFlying=true;
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
        double y = target.posY + target.getEyeHeight() - p.getEyeHeight();
        if (p.getDistanceSq(target.posX, y, target.posZ) > 64.0D) {
            spectator.playerNetServerHandler.setPlayerLocation(target.posX, y, target.posZ, target.rotationYaw, target.rotationPitch);
        } else {
            p.setPositionAndRotation(target.posX, y, target.posZ, target.rotationYaw, target.rotationPitch);
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










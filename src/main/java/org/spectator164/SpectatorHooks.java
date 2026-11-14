package org.spectator164;

import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet103SetSlot;
import net.minecraft.network.packet.Packet14BlockDig;

public final class SpectatorHooks {
    private SpectatorHooks() { }
    public static boolean isSpectator(Entity entity) {
        return entity instanceof EntityPlayer && SpectatorMod.active((EntityPlayer) entity);
    }
    public static boolean blockEntityCollision(Entity first, Entity second) {
        return isSpectator(first) || isSpectator(second);
    }
    public static boolean blockDrop(NetServerHandler handler, Packet14BlockDig packet) {
        return (packet.status == 3 || packet.status == 4) && blockInventoryChange(handler);
    }
    public static boolean blockInventoryChange(NetServerHandler handler) {
        if (!isSpectator(handler.playerEntity)) return false;
        handler.playerEntity.sendContainerToPlayer(handler.playerEntity.openContainer);
        handler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, handler.playerEntity.inventory.getItemStack()));
        return true;
    }
    public static void spawnParticle(World world, String name, double x, double y, double z,
            double vx, double vy, double vz, Entity source) {
        if (!isSpectator(source)) world.spawnParticle(name, x, y, z, vx, vy, vz);
    }
    public static List movementCollisions(WorldServer world, Entity entity, AxisAlignedBB bounds) {
        return isSpectator(entity) ? Collections.emptyList() : world.getCollidingBoundingBoxes(entity, bounds);
    }
}




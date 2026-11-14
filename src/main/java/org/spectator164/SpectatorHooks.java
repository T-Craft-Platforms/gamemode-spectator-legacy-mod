package org.spectator164;

import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityBoat;
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
    public static double cameraX(Entity entity) {
        if (!(entity instanceof EntityHanging)) return entity.posX;
        double angle = Math.toRadians(((EntityHanging) entity).hangingDirection * 90.0D);
        double distance = (entity.boundingBox.maxX - entity.boundingBox.minX) / 2.0D + 0.25D;
        return entity.posX - Math.sin(angle) * distance;
    }
    public static double cameraZ(Entity entity) {
        if (!(entity instanceof EntityHanging)) return entity.posZ;
        double angle = Math.toRadians(((EntityHanging) entity).hangingDirection * 90.0D);
        double distance = (entity.boundingBox.maxZ - entity.boundingBox.minZ) / 2.0D + 0.25D;
        return entity.posZ + Math.cos(angle) * distance;
    }
    public static double cameraY(Entity entity) {
        if (entity instanceof EntityMinecart || entity instanceof EntityBoat) return entity.boundingBox.maxY + 0.25D;
        if (entity instanceof EntityLivingBase) return entity.boundingBox.minY + entity.getEyeHeight();
        return (entity.boundingBox.minY + entity.boundingBox.maxY) / 2.0D;
    }
    public static float cameraYaw(Entity entity) {
        return entity instanceof EntityHanging ? ((EntityHanging) entity).hangingDirection * 90.0F : entity.rotationYaw;
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




package org.spectator164.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class SpectatorTransformer implements IClassTransformer, Opcodes {
  public byte[] transform(String name, String transformedName, byte[] bytes) {
    if (bytes == null) return null;
    final boolean entity = "net.minecraft.entity.Entity".equals(transformedName);
    final boolean server = "net.minecraft.network.NetServerHandler".equals(transformedName);
    final boolean gui = "net.minecraft.client.gui.inventory.GuiContainer".equals(transformedName);
    final boolean living = "net.minecraft.entity.EntityLivingBase".equals(transformedName);
    final boolean potion = "net.minecraft.potion.Potion".equals(transformedName);
    final boolean playerClass = "net.minecraft.entity.player.EntityPlayer".equals(transformedName);
    final boolean minecart = "net.minecraft.entity.item.EntityMinecart".equals(transformedName);
    final boolean minecraft = "net.minecraft.client.Minecraft".equals(transformedName);
    final boolean forgeHud = "net.minecraftforge.client.GuiIngameForge".equals(transformedName);
    final boolean gameType = "net.minecraft.world.EnumGameType".equals(transformedName);
    final boolean gameManager = "net.minecraft.item.ItemInWorldManager".equals(transformedName);
    final boolean controller =
        "net.minecraft.client.multiplayer.PlayerControllerMP".equals(transformedName);
    if (!entity
        && !server
        && !gui
        && !living
        && !potion
        && !playerClass
        && !minecart
        && !minecraft
        && !forgeHud
        && !gameType
        && !gameManager
        && !controller) return bytes;
    final int[] changes = new int[19];
    ClassReader reader = new ClassReader(bytes);
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
    reader.accept(
        new ClassVisitor(ASM4, writer) {
          @Override
          public MethodVisitor visitMethod(
              int access, final String method, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, method, desc, signature, exceptions);
            final boolean pressure =
                entity
                    && desc.equals("()Z")
                    && (method.equals("doesEntityNotTriggerPressurePlate")
                        || method.equals("func_82144_au"));
            final boolean collisions =
                entity
                    && desc.equals("()V")
                    && (method.equals("doBlockCollisions") || method.equals("func_70017_D"));
            final boolean ladder =
                living
                    && desc.equals("()Z")
                    && (method.equals("isOnLadder") || method.equals("func_70617_f_"));
            final boolean liquid =
                entity
                    && ((desc.equals("()Z")
                            && (method.equals("isInWater")
                                || method.equals("func_70090_H")
                                || method.equals("handleWaterMovement")
                                || method.equals("func_70072_I")
                                || method.equals("handleLavaMovement")
                                || method.equals("func_70058_J")))
                        || (desc.equals("(Lnet/minecraft/block/material/Material;)Z")
                            && (method.equals("isInsideOfMaterial")
                                || method.equals("func_70055_a"))));
            final boolean web =
                entity
                    && desc.equals("()V")
                    && (method.equals("setInWeb") || method.equals("func_70110_aj"));
            final boolean potionApplicable =
                living
                    && desc.equals("(Lnet/minecraft/potion/PotionEffect;)Z")
                    && (method.equals("isPotionApplicable") || method.equals("func_70687_e"));
            final boolean instantPotion =
                potion
                    && desc.equals(
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/EntityLivingBase;ID)V");
            final boolean flying =
                server && (method.equals("handleFlying") || method.equals("func_72498_a"));
            final boolean inventory =
                server
                    && (method.equals("handleWindowClick")
                        || method.equals("func_72523_a")
                        || method.equals("handleEnchantItem")
                        || method.equals("func_72479_a")
                        || method.equals("handleCreativeSetSlot")
                        || method.equals("func_72464_a"));
            final boolean drop =
                server && (method.equals("handleBlockDig") || method.equals("func_72510_a"));
            final boolean collisionEligibility =
                living
                    && desc.equals("()Z")
                    && (method.equals("canBeCollidedWith")
                        || method.equals("func_70067_L")
                        || method.equals("canBePushed")
                        || method.equals("func_70104_M"));
            final boolean pickup =
                playerClass
                    && (method.equals("collideWithPlayer") || method.equals("func_71044_o"))
                    && desc.equals("(Lnet/minecraft/entity/Entity;)V");
            final boolean entityCollision =
                (entity || minecart)
                    && (method.equals("applyEntityCollision") || method.equals("func_70108_f"))
                    && desc.equals("(Lnet/minecraft/entity/Entity;)V");
            final boolean hotbar =
                minecraft
                    && (method.equals("runTick") || method.equals("func_71407_l"))
                    && desc.equals("()V");
            final boolean tooltip =
                forgeHud && method.equals("renderToolHightlight") && desc.equals("(II)V");
            final boolean guiClick =
                gui && (method.equals("handleMouseClick") || method.equals("func_74191_a"));
            final boolean particles =
                (entity || living || playerClass) && (access & ACC_STATIC) == 0;
            final boolean capabilities =
                gameType
                    && (method.equals("configurePlayerCapabilities")
                        || method.equals("func_77147_a"))
                    && desc.equals("(Lnet/minecraft/entity/player/PlayerCapabilities;)V");
            final boolean managerMode =
                gameManager
                    && (method.equals("setGameType") || method.equals("func_73076_a"))
                    && desc.equals("(Lnet/minecraft/world/EnumGameType;)V");
            final boolean clientMode =
                controller
                    && (method.equals("setGameType") || method.equals("func_78746_a"))
                    && desc.equals("(Lnet/minecraft/world/EnumGameType;)V");
            if (!capabilities
                && !managerMode
                && !clientMode
                && !particles
                && !pressure
                && !collisions
                && !ladder
                && !liquid
                && !web
                && !potionApplicable
                && !instantPotion
                && !flying
                && !inventory
                && !guiClick
                && !drop
                && !entityCollision
                && !collisionEligibility
                && !pickup
                && !hotbar
                && !tooltip) return mv;
            if (liquid) changes[17]++;
            if (web) changes[18]++;
            if (capabilities) changes[11]++;
            if (managerMode) changes[12]++;
            if (clientMode) changes[13]++;
            if (inventory) changes[3]++;
            if (guiClick) changes[4]++;
            if (drop) changes[5]++;
            if (entityCollision) changes[6]++;
            if (collisionEligibility) changes[10]++;
            if (pickup) changes[9]++;
            if (tooltip) changes[8]++;
            if (pressure) changes[0]++;
            if (collisions) changes[1]++;
            return new MethodVisitor(ASM4, mv) {
              @Override
              public void visitCode() {
                super.visitCode();
                if (managerMode) {
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitVarInsn(ALOAD, 1);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorMod",
                      "changingMode",
                      "(Lnet/minecraft/item/ItemInWorldManager;Lnet/minecraft/world/EnumGameType;)V");
                }
                if (clientMode) {
                  mv.visitVarInsn(ALOAD, 1);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/ClientHooks",
                      "setGameType",
                      "(Lnet/minecraft/world/EnumGameType;)V");
                }
                if (collisionEligibility || ladder || liquid || potionApplicable) {
                  Label normal = new Label();
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorHooks",
                      "isSpectator",
                      "(Lnet/minecraft/entity/Entity;)Z");
                  mv.visitJumpInsn(IFEQ, normal);
                  mv.visitInsn(ICONST_0);
                  mv.visitInsn(IRETURN);
                  mv.visitLabel(normal);
                  mv.visitFrame(F_SAME, 0, null, 0, null);
                }
                if (instantPotion) {
                  Label normal = new Label();
                  mv.visitVarInsn(ALOAD, 2);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorHooks",
                      "isSpectator",
                      "(Lnet/minecraft/entity/Entity;)Z");
                  mv.visitJumpInsn(IFEQ, normal);
                  mv.visitInsn(RETURN);
                  mv.visitLabel(normal);
                  mv.visitFrame(F_SAME, 0, null, 0, null);
                }
                if (entityCollision) {
                  Label normal = new Label();
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitVarInsn(ALOAD, 1);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorHooks",
                      "blockEntityCollision",
                      "(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)Z");
                  mv.visitJumpInsn(IFEQ, normal);
                  mv.visitInsn(RETURN);
                  mv.visitLabel(normal);
                  mv.visitFrame(F_SAME, 0, null, 0, null);
                }
                if (tooltip) {
                  Label normal = new Label();
                  mv.visitMethodInsn(
                      INVOKESTATIC, "org/spectator164/ClientHooks", "isSpectating", "()Z");
                  mv.visitJumpInsn(IFEQ, normal);
                  mv.visitInsn(RETURN);
                  mv.visitLabel(normal);
                  mv.visitFrame(F_SAME, 0, null, 0, null);
                }
                if (drop) {
                  Label normal = new Label();
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitVarInsn(ALOAD, 1);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorHooks",
                      "blockDrop",
                      "(Lnet/minecraft/network/NetServerHandler;Lnet/minecraft/network/packet/Packet14BlockDig;)Z");
                  mv.visitJumpInsn(IFEQ, normal);
                  mv.visitInsn(RETURN);
                  mv.visitLabel(normal);
                  mv.visitFrame(F_SAME, 0, null, 0, null);
                }
                if (inventory || guiClick) {
                  Label normal = new Label();
                  if (inventory) mv.visitVarInsn(ALOAD, 0);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      inventory
                          ? "org/spectator164/SpectatorHooks"
                          : "org/spectator164/ClientHooks",
                      inventory ? "blockInventoryChange" : "isSpectating",
                      inventory ? "(Lnet/minecraft/network/NetServerHandler;)Z" : "()Z");
                  mv.visitJumpInsn(IFEQ, normal);
                  mv.visitInsn(RETURN);
                  mv.visitLabel(normal);
                  mv.visitFrame(F_SAME, 0, null, 0, null);
                }
                if (pressure || collisions || pickup || web) {
                  Label normal = new Label();
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorHooks",
                      "isSpectator",
                      "(Lnet/minecraft/entity/Entity;)Z");
                  mv.visitJumpInsn(IFEQ, normal);
                  if (pressure) {
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(IRETURN);
                  } else mv.visitInsn(RETURN);
                  mv.visitLabel(normal);
                  mv.visitFrame(F_SAME, 0, null, 0, null);
                }
              }

              @Override
              public void visitInsn(int opcode) {
                if (capabilities && opcode == RETURN) {
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitVarInsn(ALOAD, 1);
                  mv.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorMod",
                      "configureCapabilities",
                      "(Lnet/minecraft/world/EnumGameType;Lnet/minecraft/entity/player/PlayerCapabilities;)V");
                }
                super.visitInsn(opcode);
              }

              @Override
              public void visitFieldInsn(
                  int opcode, String owner, String field, String descriptor) {
                if (hotbar
                    && opcode == PUTFIELD
                    && owner.equals("net/minecraft/client/settings/GameSettings")
                    && descriptor.equals("Z")
                    && (field.equals("showDebugInfo")
                        || field.equals("field_74330_P")
                        || field.equals("showDebugProfilerChart")
                        || field.equals("field_74329_Q"))) {
                  boolean info = field.equals("showDebugInfo") || field.equals("field_74330_P");
                  changes[info ? 14 : 15]++;
                  super.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/ClientHooks",
                      info ? "setDebugInfo" : "setDebugProfiler",
                      "(Lnet/minecraft/client/settings/GameSettings;Z)V");
                } else if (hotbar
                    && opcode == PUTFIELD
                    && owner.equals("net/minecraft/entity/player/InventoryPlayer")
                    && (field.equals("currentItem") || field.equals("field_70461_c"))
                    && descriptor.equals("I")) {
                  changes[7]++;
                  super.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/ClientHooks",
                      "selectHotbarSlot",
                      "(Lnet/minecraft/entity/player/InventoryPlayer;I)V");
                } else super.visitFieldInsn(opcode, owner, field, descriptor);
              }

              @Override
              public void visitMethodInsn(
                  int opcode, String owner, String target, String descriptor) {
                if (hotbar
                    && opcode == INVOKESTATIC
                    && owner.equals("org/lwjgl/input/Keyboard")
                    && target.equals("next")
                    && descriptor.equals("()Z")) {
                  changes[16]++;
                  super.visitMethodInsn(
                      INVOKESTATIC, "org/spectator164/ClientHooks", "nextKeyboardEvent", "()Z");
                } else if (particles
                    && opcode == INVOKEVIRTUAL
                    && owner.equals("net/minecraft/world/World")
                    && (target.equals("spawnParticle") || target.equals("func_72869_a"))
                    && descriptor.equals("(Ljava/lang/String;DDDDDD)V")) {
                  super.visitVarInsn(ALOAD, 0);
                  super.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorHooks",
                      "spawnParticle",
                      "(Lnet/minecraft/world/World;Ljava/lang/String;DDDDDDLnet/minecraft/entity/Entity;)V");
                } else if (flying
                    && opcode == INVOKEVIRTUAL
                    && owner.equals("net/minecraft/world/WorldServer")
                    && (target.equals("getCollidingBoundingBoxes") || target.equals("func_72945_a"))
                    && descriptor.equals(
                        "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;")) {
                  changes[2]++;
                  super.visitMethodInsn(
                      INVOKESTATIC,
                      "org/spectator164/SpectatorHooks",
                      "movementCollisions",
                      "(Lnet/minecraft/world/WorldServer;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;");
                } else super.visitMethodInsn(opcode, owner, target, descriptor);
              }
            };
          }
        },
        0);
    if ((entity
            && (changes[0] != 1
                || changes[1] != 1
                || changes[6] != 1
                || changes[17] != 4
                || changes[18] != 1))
        || (server && (changes[2] != 2 || changes[3] != 3 || changes[5] != 1))
        || (gui && changes[4] != 1)
        || (minecart && changes[6] != 1)
        || (playerClass && changes[9] != 1)
        || (living && changes[10] != 2)
        || (minecraft
            && (changes[7] != 1 || changes[14] != 1 || changes[15] != 1 || changes[16] != 1))
        || (forgeHud && changes[8] != 1)
        || (gameType && changes[11] != 1)
        || (gameManager && changes[12] != 1)
        || (controller && changes[13] != 1)) {
      throw new IllegalStateException(
          "Spectator164 could not apply the expected hooks to "
              + transformedName
              + ": "
              + changes[0]
              + ","
              + changes[1]
              + ","
              + changes[2]);
    }
    System.out.println("Spectator164 hooks verified for " + transformedName);
    return writer.toByteArray();
  }
}

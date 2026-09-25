package com.cryrius.turretcontroller.tileentity;

import java.util.List;

import com.cryrius.turretcontroller.compat.HbmReflection;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.INpc;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * Server-side controller state and scheduler.
 *
 * <p>The controller does not subclass, mix into, or compile against a turret
 * implementation.  It owns policy (filters, range, redstone, and lock) while
 * {@link HbmReflection} translates that policy to the optional target at
 * runtime.</p>
 */
public class TileEntityTurretController extends TileEntity {

    public static final int MODE_HOSTILE = 0;
    public static final int MODE_ALL_LIVING = 1;
    public static final int MODE_PLAYERS = 2;
    public static final int MODE_ANIMALS = 3;
    public static final int MODE_MOBS = 4;
    private static final int MODE_COUNT = 5;

    public static final int REDSTONE_IGNORE = 0;
    public static final int REDSTONE_POWERED = 1;
    public static final int REDSTONE_UNPOWERED = 2;

    public static final int ACTION_TOGGLE = 0;
    public static final int ACTION_CYCLE_MODE = 1;
    public static final int ACTION_RANGE_UP = 2;
    public static final int ACTION_RANGE_DOWN = 3;
    public static final int ACTION_CYCLE_REDSTONE = 4;
    public static final int ACTION_UNLINK = 5;

    private static final int MIN_RANGE = 8;
    private static final int MAX_RANGE = 96;
    private static final int UPDATE_INTERVAL = 5;

    private boolean linked;
    private int turretX;
    private int turretY;
    private int turretZ;
    private boolean enabled = true;
    private int targetMode = MODE_HOSTILE;
    private int range = 32;
    private int redstoneMode = REDSTONE_IGNORE;
    private String owner = "";
    private int updateTimer;

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        updateTimer++;
        if (updateTimer < UPDATE_INTERVAL) {
            return;
        }
        updateTimer = 0;
        controlLinkedTurret();
    }

    private void controlLinkedTurret() {
        if (!linked) {
            return;
        }

        TileEntity tile = worldObj.getTileEntity(turretX, turretY, turretZ);
        if (!HbmReflection.isTurret(tile)) {
            // Keep coordinates so a temporarily unloaded/replaced turret can
            // be repaired without losing the controller's configuration.
            return;
        }

        boolean operational = enabled && redstoneAllowsFire();
        HbmReflection.setEnabled(tile, operational);
        HbmReflection.setTargetingFlags(tile,
                targetMode == MODE_PLAYERS || targetMode == MODE_ALL_LIVING,
                targetMode == MODE_ANIMALS || targetMode == MODE_ALL_LIVING,
                targetMode == MODE_HOSTILE || targetMode == MODE_MOBS || targetMode == MODE_ALL_LIVING,
                false);

        Entity target = HbmReflection.getTarget(tile);
        if (!operational) {
            if (target != null) {
                HbmReflection.setTarget(tile, null);
            }
            return;
        }

        if (!isValidTarget(target, tile)) {
            target = findTarget(tile);
        }
        HbmReflection.setTarget(tile, target);
    }

    private boolean redstoneAllowsFire() {
        if (redstoneMode == REDSTONE_POWERED) {
            return worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
        }
        if (redstoneMode == REDSTONE_UNPOWERED) {
            return !worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
        }
        return true;
    }

    private Entity findTarget(TileEntity turret) {
        double centerX = turret.xCoord + 0.5D;
        double centerY = turret.yCoord + 0.5D;
        double centerZ = turret.zCoord + 0.5D;
        double radius = range;
        AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(
                centerX - radius, centerY - radius, centerZ - radius,
                centerX + radius, centerY + radius, centerZ + radius);
        List<?> candidates = worldObj.getEntitiesWithinAABBExcludingEntity(null, bounds);
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Object object : candidates) {
            if (!(object instanceof Entity)) {
                continue;
            }
            Entity candidate = (Entity) object;
            if (!isValidTarget(candidate, turret)) {
                continue;
            }
            double distance = candidate.getDistanceSq(centerX, centerY, centerZ);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isValidTarget(Entity target, TileEntity turret) {
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
            return false;
        }
        double distance = target.getDistanceSq(turret.xCoord + 0.5D,
                turret.yCoord + 0.5D, turret.zCoord + 0.5D);
        if (distance > (double) range * (double) range) {
            return false;
        }

        // Never cause a controller to target its owner accidentally.
        if (target instanceof EntityPlayer && owner.length() > 0
                && owner.equals(((EntityPlayer) target).getCommandSenderName())) {
            return false;
        }

        switch (targetMode) {
            case MODE_ALL_LIVING:
                return true;
            case MODE_PLAYERS:
                return target instanceof EntityPlayer;
            case MODE_ANIMALS:
                return target instanceof IAnimals && !(target instanceof IMob);
            case MODE_MOBS:
                return target instanceof IMob || target instanceof INpc;
            case MODE_HOSTILE:
            default:
                return target instanceof IMob;
        }
    }

    public boolean linkTurret(World world, int x, int y, int z, EntityPlayer player) {
        if (world == null || world != worldObj || !canConfigure(player)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!HbmReflection.isTurret(tile)) {
            if (player != null) {
                player.addChatMessage(new ChatComponentText("That block is not a supported turret."));
            }
            return false;
        }
        if (player != null && player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) > 128D * 128D) {
            player.addChatMessage(new ChatComponentText("The turret is too far away to link."));
            return false;
        }
        turretX = x;
        turretY = y;
        turretZ = z;
        linked = true;
        changed();
        return true;
    }

    public void unlink() {
        linked = false;
        TileEntity tile = worldObj == null ? null : worldObj.getTileEntity(turretX, turretY, turretZ);
        if (HbmReflection.isTurret(tile)) {
            HbmReflection.setTarget(tile, null);
            HbmReflection.setEnabled(tile, false);
        }
        changed();
    }

    public void toggleEnabled() {
        enabled = !enabled;
        changed();
    }

    public void cycleTargetMode() {
        targetMode = (targetMode + 1) % MODE_COUNT;
        changed();
    }

    public void adjustRange(int amount) {
        range += amount;
        if (range < MIN_RANGE) {
            range = MIN_RANGE;
        }
        if (range > MAX_RANGE) {
            range = MAX_RANGE;
        }
        changed();
    }

    public void cycleRedstoneMode() {
        redstoneMode = (redstoneMode + 1) % 3;
        changed();
    }

    /** Called only by a validated server packet. */
    public void handleAction(int action, EntityPlayer player) {
        if (!canConfigure(player)) {
            return;
        }
        switch (action) {
            case ACTION_TOGGLE:
                toggleEnabled();
                break;
            case ACTION_CYCLE_MODE:
                cycleTargetMode();
                break;
            case ACTION_RANGE_UP:
                adjustRange(8);
                break;
            case ACTION_RANGE_DOWN:
                adjustRange(-8);
                break;
            case ACTION_CYCLE_REDSTONE:
                cycleRedstoneMode();
                break;
            case ACTION_UNLINK:
                unlink();
                break;
            default:
                return;
        }
    }

    public boolean canConfigure(EntityPlayer player) {
        return player != null && (owner.length() == 0 || owner.equals(player.getCommandSenderName())
                || player.capabilities.isCreativeMode);
    }

    public void setOwner(String name) {
        if (name != null && owner.length() == 0) {
            owner = name;
            changed();
        }
    }

    public void sendStatus(EntityPlayer player) {
        if (player == null) {
            return;
        }
        player.addChatMessage(new ChatComponentText("Controller " + (enabled ? "enabled" : "disabled")
                + ", target mode: " + getTargetModeName() + ", range: " + range + "m, redstone: "
                + getRedstoneModeName()));
    }

    public boolean isLinked() {
        return linked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTargetMode() {
        return targetMode;
    }

    public int getRange() {
        return range;
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public String getOwner() {
        return owner;
    }

    public int getTurretX() {
        return turretX;
    }

    public int getTurretY() {
        return turretY;
    }

    public int getTurretZ() {
        return turretZ;
    }

    private void changed() {
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public String getTargetModeName() {
        switch (targetMode) {
            case MODE_ALL_LIVING:
                return "All living";
            case MODE_PLAYERS:
                return "Players";
            case MODE_ANIMALS:
                return "Animals";
            case MODE_MOBS:
                return "Mobs";
            case MODE_HOSTILE:
            default:
                return "Hostile mobs";
        }
    }

    public String getRedstoneModeName() {
        switch (redstoneMode) {
            case REDSTONE_POWERED:
                return "Powered = on";
            case REDSTONE_UNPOWERED:
                return "Unpowered = on";
            default:
                return "Ignored";
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        linked = nbt.getBoolean("Linked");
        turretX = nbt.getInteger("TurretX");
        turretY = nbt.getInteger("TurretY");
        turretZ = nbt.getInteger("TurretZ");
        enabled = !nbt.hasKey("Enabled") || nbt.getBoolean("Enabled");
        targetMode = clamp(nbt.getInteger("TargetMode"), 0, MODE_COUNT - 1);
        range = clamp(nbt.hasKey("Range") ? nbt.getInteger("Range") : 32, MIN_RANGE, MAX_RANGE);
        redstoneMode = clamp(nbt.getInteger("RedstoneMode"), REDSTONE_IGNORE, REDSTONE_UNPOWERED);
        owner = nbt.getString("Owner");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("Linked", linked);
        nbt.setInteger("TurretX", turretX);
        nbt.setInteger("TurretY", turretY);
        nbt.setInteger("TurretZ", turretZ);
        nbt.setBoolean("Enabled", enabled);
        nbt.setInteger("TargetMode", targetMode);
        nbt.setInteger("Range", range);
        nbt.setInteger("RedstoneMode", redstoneMode);
        nbt.setString("Owner", owner);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

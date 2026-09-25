package com.cryrius.turretcontroller.item;

import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;
import com.cryrius.turretcontroller.compat.HbmReflection;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * A two-click linker.  First use stores the position of a compatible turret;
 * second use on a controller binds it.  Only coordinates are saved in the
 * item, not a class reference, which keeps the item safe to serialize without
 * the optional mod installed.
 */
public class ItemTurretLinker extends Item {

    private static final String TAG_BOUND = "Bound";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";

    public ItemTurretLinker() {
        setMaxStackSize(1);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
                             int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityTurretController) {
            if (world.isRemote) {
                return true;
            }
            if (!hasBoundTurret(stack)) {
                player.addChatMessage(new ChatComponentText("Select a compatible turret first."));
                return true;
            }
            int turretX = stack.stackTagCompound.getInteger(TAG_X);
            int turretY = stack.stackTagCompound.getInteger(TAG_Y);
            int turretZ = stack.stackTagCompound.getInteger(TAG_Z);
            boolean linked = ((TileEntityTurretController) tile).linkTurret(
                    world, turretX, turretY, turretZ, player);
            if (linked) {
                clearBoundTurret(stack);
                player.addChatMessage(new ChatComponentText("Turret controller linked."));
            }
            return true;
        }

        if (HbmReflection.isTurret(tile)) {
            if (!world.isRemote) {
                bindTurret(stack, x, y, z);
                player.addChatMessage(new ChatComponentText(
                        "Turret selected. Right-click a Turret Controller to link it."));
            }
            return true;
        }

        return false;
    }

    private static boolean hasBoundTurret(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
                && stack.stackTagCompound.getBoolean(TAG_BOUND);
    }

    private static void bindTurret(ItemStack stack, int x, int y, int z) {
        if (stack.stackTagCompound == null) {
            stack.stackTagCompound = new NBTTagCompound();
        }
        stack.stackTagCompound.setBoolean(TAG_BOUND, true);
        stack.stackTagCompound.setInteger(TAG_X, x);
        stack.stackTagCompound.setInteger(TAG_Y, y);
        stack.stackTagCompound.setInteger(TAG_Z, z);
    }

    private static void clearBoundTurret(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.stackTagCompound.removeTag(TAG_BOUND);
            stack.stackTagCompound.removeTag(TAG_X);
            stack.stackTagCompound.removeTag(TAG_Y);
            stack.stackTagCompound.removeTag(TAG_Z);
        }
    }
}

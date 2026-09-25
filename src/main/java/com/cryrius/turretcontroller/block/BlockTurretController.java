package com.cryrius.turretcontroller.block;

import com.cryrius.turretcontroller.TurretControllerMod;
import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** The controller's physical block. */
public class BlockTurretController extends BlockContainer {

    public BlockTurretController() {
        super(Material.iron);
        setHardness(4.0F);
        setResistance(12.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityTurretController();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z,
                                    EntityPlayer player, int side,
                                    float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityTurretController)) {
            return false;
        }

        TileEntityTurretController controller = (TileEntityTurretController) tile;
        if (player.isSneaking()) {
            if (!world.isRemote) {
                controller.toggleEnabled();
                controller.sendStatus(player);
            }
            return true;
        }

        if (!world.isRemote) {
            player.openGui(TurretControllerMod.INSTANCE, TurretControllerMod.GUI_CONTROLLER,
                    world, x, y, z);
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z,
                                EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityTurretController && placer instanceof EntityPlayer) {
            ((TileEntityTurretController) tile).setOwner(((EntityPlayer) placer).getCommandSenderName());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return 0;
    }
}

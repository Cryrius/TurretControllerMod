package com.cryrius.turretcontroller.client;

import com.cryrius.turretcontroller.common.CommonProxy;
import com.cryrius.turretcontroller.container.ContainerTurretController;
import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ClientProxy extends CommonProxy {

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world,
                                      int x, int y, int z) {
        if (id != 1) {
            return null;
        }
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityTurretController) {
            return new GuiTurretController(player,
                    new ContainerTurretController(player, (TileEntityTurretController) tile));
        }
        return null;
    }
}

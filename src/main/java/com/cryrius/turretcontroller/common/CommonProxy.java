package com.cryrius.turretcontroller.common;

import com.cryrius.turretcontroller.container.ContainerTurretController;
import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Common-side proxy.  It deliberately contains no client-only Minecraft classes. */
public class CommonProxy implements IGuiHandler {

    public void preInit() {
        // Extension point for a server-only deployment.
    }

    public void init() {
        // Extension point for common registrations.
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world,
                                      int x, int y, int z) {
        if (id != 1) {
            return null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityTurretController) {
            return new ContainerTurretController(player, (TileEntityTurretController) tile);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world,
                                      int x, int y, int z) {
        // The client proxy overrides this method. Returning null is important
        // on a dedicated server: no GUI class is loaded there.
        return null;
    }
}

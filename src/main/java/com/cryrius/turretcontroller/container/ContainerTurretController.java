package com.cryrius.turretcontroller.container;

import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/** A deliberately slotless container; all commands are server validated packets. */
public class ContainerTurretController extends Container {

    private final TileEntityTurretController controller;
    public ContainerTurretController(EntityPlayer player, TileEntityTurretController controller) {
        this.player = player;
        this.controller = controller;
    }

    public TileEntityTurretController getController() {
        return controller;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return controller != null && controller.getWorldObj() != null
                && controller.getWorldObj().getTileEntity(controller.xCoord,
                        controller.yCoord, controller.zCoord) == controller
                && player.getDistanceSq(controller.xCoord + 0.5D,
                        controller.yCoord + 0.5D, controller.zCoord + 0.5D) <= 64D;
    }

    @Override
    public void onContainerClosed(EntityPlayer closingPlayer) {
        super.onContainerClosed(closingPlayer);
    }
}

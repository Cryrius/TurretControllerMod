package com.cryrius.turretcontroller.client;

import com.cryrius.turretcontroller.TurretControllerMod;
import com.cryrius.turretcontroller.compat.HbmReflection;
import com.cryrius.turretcontroller.container.ContainerTurretController;
import com.cryrius.turretcontroller.network.ControllerMessage;
import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

/** Compact control screen that does not depend on HBM textures or classes. */
public class GuiTurretController extends GuiContainer {

    private static final int WIDTH = 248;
    private static final int HEIGHT = 178;
    private final EntityPlayer player;
    private final TileEntityTurretController controller;

    public GuiTurretController(EntityPlayer player, ContainerTurretController container) {
        super(container);
        this.player = player;
        this.controller = container.getController();
        xSize = WIDTH;
        ySize = HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        int left = guiLeft;
        int top = guiTop;
        buttonList.add(new GuiButton(TileEntityTurretController.ACTION_TOGGLE,
                left + 8, top + 78, 72, 20, "Enable / disable"));
        buttonList.add(new GuiButton(TileEntityTurretController.ACTION_CYCLE_MODE,
                left + 86, top + 78, 72, 20, "Target mode"));
        buttonList.add(new GuiButton(TileEntityTurretController.ACTION_CYCLE_REDSTONE,
                left + 164, top + 78, 76, 20, "Redstone"));
        buttonList.add(new GuiButton(TileEntityTurretController.ACTION_RANGE_DOWN,
                left + 8, top + 104, 112, 20, "Range -8"));
        buttonList.add(new GuiButton(TileEntityTurretController.ACTION_RANGE_UP,
                left + 128, top + 104, 112, 20, "Range +8"));
        buttonList.add(new GuiButton(TileEntityTurretController.ACTION_UNLINK,
                left + 8, top + 132, 232, 20, "Unlink turret"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (TurretControllerMod.NETWORK != null) {
            TurretControllerMod.NETWORK.sendToServer(new ControllerMessage(button.id,
                    controller.xCoord, controller.yCoord, controller.zCoord));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xff20252b);
        drawRect(guiLeft + 4, guiTop + 4, guiLeft + xSize - 4, guiTop + 24, 0xff39444f);
        drawRect(guiLeft + 4, guiTop + 28, guiLeft + xSize - 4, guiTop + 72, 0xff2b3239);
        drawRect(guiLeft + 4, guiTop + 158, guiLeft + xSize - 4, guiTop + ySize - 4, 0xff15191d);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString("TURRET CONTROLLER", 10, 10, 0xffffff);
        fontRendererObj.drawString(controller.isLinked() ? "Link: CONNECTED" : "Link: NO TURRET", 10, 34,
                controller.isLinked() ? 0x71e08a : 0xff7777);
        fontRendererObj.drawString("Mode: " + controller.getTargetModeName(), 10, 48, 0xd6dbe0);
        fontRendererObj.drawString("Range: " + controller.getRange() + " blocks", 132, 48, 0xd6dbe0);
        fontRendererObj.drawString("Redstone: " + controller.getRedstoneModeName(), 10, 63, 0xd6dbe0);
        fontRendererObj.drawString("Policy: " + (controller.isEnabled() ? "ARMED" : "SAFE"), 132, 63,
                controller.isEnabled() ? 0xffd36b : 0xff7777);

        String telemetry = "No telemetry";
        if (controller.isLinked() && controller.getWorldObj() != null) {
            TileEntity tile = controller.getWorldObj().getTileEntity(controller.getTurretX(),
                    controller.getTurretY(), controller.getTurretZ());
            HbmReflection.TurretSnapshot snapshot = HbmReflection.snapshot(tile);
            if (snapshot.available) {
                String target = "none";
                Entity entity = snapshot.target;
                if (entity != null) {
                    target = entity.getCommandSenderName();
                }
                telemetry = "Native: " + (snapshot.enabled ? "ON" : "OFF")
                        + "  Target: " + target;
            }
        }
        fontRendererObj.drawString(telemetry, 10, 166, 0xaeb8c2);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

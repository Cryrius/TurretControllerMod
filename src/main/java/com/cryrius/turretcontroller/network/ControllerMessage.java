package com.cryrius.turretcontroller.network;

import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Small server-authoritative GUI command. */
public class ControllerMessage implements IMessage {

    private int action;
    private int x;
    private int y;
    private int z;

    public ControllerMessage() {
    }

    public ControllerMessage(int action, int x, int y, int z) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        action = buffer.readByte();
        x = buffer.readInt();
        y = buffer.readInt();
        z = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeByte(action);
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
    }

    public static class Handler implements IMessageHandler<ControllerMessage, IMessage> {
        @Override
        public IMessage onMessage(ControllerMessage message, MessageContext context) {
            EntityPlayer player = context.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }
            World world = player.worldObj;
            // The GUI can only issue actions for a nearby block. This also
            // prevents a forged packet from configuring arbitrary chunks.
            if (player.getDistanceSq(message.x + 0.5D, message.y + 0.5D,
                    message.z + 0.5D) > 16D * 16D) {
                return null;
            }
            TileEntity tile = world.getTileEntity(message.x, message.y, message.z);
            if (tile instanceof TileEntityTurretController) {
                ((TileEntityTurretController) tile).handleAction(message.action, player);
            }
            return null;
        }
    }
}

package top.codfish.projecteteam.network.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.playerData.Transmutation;

public class SyncTransmutationPKT implements IMessage
{
    private double newEmc;

    public SyncTransmutationPKT() {}

    public SyncTransmutationPKT(double newEmc)
    {
        this.newEmc = newEmc;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        newEmc = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeDouble(newEmc);
    }

    public static class Handler implements IMessageHandler<SyncTransmutationPKT, IMessage>
    {
        @Override
        public IMessage onMessage(SyncTransmutationPKT message, MessageContext ctx)
        {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            Transmutation.setEmc(player, message.newEmc);
            if (player.openContainer instanceof TransmutationContainer)
            {
                TransmutationInventory inv = ((TransmutationContainer) player.openContainer).transmutationInventory;
                inv.emc = message.newEmc;
                inv.updateOutputs();
            }
            return null;
        }
    }
}

package top.codfish.projecteteam.network.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.playerData.Transmutation;

@SideOnly(Side.CLIENT)
public class SyncTransmutationPKTHandler implements IMessageHandler<SyncTransmutationPKT, IMessage>
{
    @Override
    public IMessage onMessage(SyncTransmutationPKT message, MessageContext ctx)
    {
        Minecraft mc = Minecraft.getMinecraft();
        Transmutation.setEmc(mc.thePlayer, message.getEmc());
        if (mc.thePlayer.openContainer instanceof TransmutationContainer)
        {
            TransmutationInventory inv = ((TransmutationContainer) mc.thePlayer.openContainer).transmutationInventory;
            inv.emc = message.getEmc();
            inv.updateOutputs();
        }
        return null;
    }
}

package top.codfish.projecteteam.network.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class SyncTransmutationPKTServerHandler implements IMessageHandler<SyncTransmutationPKT, IMessage>
{
    @Override
    public IMessage onMessage(SyncTransmutationPKT message, MessageContext ctx) { return null; }
}

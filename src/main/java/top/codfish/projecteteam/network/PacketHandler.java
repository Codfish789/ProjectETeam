package top.codfish.projecteteam.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import top.codfish.projecteteam.network.packets.SyncTeamDataPKT;
import top.codfish.projecteteam.network.packets.SyncTransmutationPKT;
import top.codfish.projecteteam.network.packets.SyncTransmutationPKTHandler;
import top.codfish.projecteteam.network.packets.SyncTransmutationPKTServerHandler;

public class PacketHandler
{
    private static final SimpleNetworkWrapper HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel("projecteteam");

    public static void registerCommon()
    {
        HANDLER.registerMessage(SyncTeamDataPKT.Handler.class, SyncTeamDataPKT.class, 0, Side.CLIENT);
        // Register discriminator 1 on server side so sendTo() works, but handler does nothing
        HANDLER.registerMessage(SyncTransmutationPKTServerHandler.class, SyncTransmutationPKT.class, 1, Side.SERVER);
    }

    public static void registerClient()
    {
        HANDLER.registerMessage(SyncTransmutationPKTHandler.class, SyncTransmutationPKT.class, 1, Side.CLIENT);
    }

    public static Packet getMCPacket(IMessage message)
    {
        return HANDLER.getPacketFrom(message);
    }

    public static void sendToServer(IMessage msg)
    {
        HANDLER.sendToServer(msg);
    }

    public static void sendToAll(IMessage msg)
    {
        HANDLER.sendToAll(msg);
    }

    public static void sendTo(IMessage msg, EntityPlayerMP player)
    {
        HANDLER.sendTo(msg, player);
    }

    public static void sendToDimension(IMessage msg, int dimension)
    {
        HANDLER.sendToDimension(msg, dimension);
    }
}

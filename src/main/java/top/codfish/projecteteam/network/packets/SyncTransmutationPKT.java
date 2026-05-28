package top.codfish.projecteteam.network.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class SyncTransmutationPKT implements IMessage
{
    private double newEmc;

    public SyncTransmutationPKT() {}

    public SyncTransmutationPKT(double newEmc) { this.newEmc = newEmc; }

    @Override
    public void fromBytes(ByteBuf buf) { newEmc = buf.readDouble(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeDouble(newEmc); }

    public double getEmc() { return newEmc; }
}

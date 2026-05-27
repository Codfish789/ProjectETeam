package top.codfish.projecteteam.network.packets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import top.codfish.projecteteam.ProjectETeam;
import top.codfish.projecteteam.team.TeamData;

public class SyncTeamDataPKT implements IMessage
{
    private NBTTagCompound teamNbt;
    private Map<UUID, String> memberNames;

    public SyncTeamDataPKT() {}

    public SyncTeamDataPKT(NBTTagCompound teamNbt, Map<UUID, String> memberNames)
    {
        this.teamNbt = teamNbt;
        this.memberNames = memberNames;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        teamNbt = ByteBufUtils.readTag(buf);
        memberNames = new HashMap<UUID, String>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++)
        {
            long mostSig = buf.readLong();
            long leastSig = buf.readLong();
            UUID uuid = new UUID(mostSig, leastSig);
            String name = ByteBufUtils.readUTF8String(buf);
            memberNames.put(uuid, name);
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, teamNbt);
        buf.writeInt(memberNames != null ? memberNames.size() : 0);
        if (memberNames != null)
        {
            for (Map.Entry<UUID, String> entry : memberNames.entrySet())
            {
                buf.writeLong(entry.getKey().getMostSignificantBits());
                buf.writeLong(entry.getKey().getLeastSignificantBits());
                ByteBufUtils.writeUTF8String(buf, entry.getValue());
            }
        }
    }

    public static class Handler implements IMessageHandler<SyncTeamDataPKT, IMessage>
    {
        @Override
        public IMessage onMessage(SyncTeamDataPKT message, MessageContext ctx)
        {
            if (message.teamNbt != null)
            {
                TeamData team = TeamData.readFromNBT(message.teamNbt);
                ProjectETeam.proxy.updateClientTeamData(team, message.memberNames);
            }
            else
            {
                ProjectETeam.proxy.updateClientTeamData(null, null);
            }
            return null;
        }
    }
}

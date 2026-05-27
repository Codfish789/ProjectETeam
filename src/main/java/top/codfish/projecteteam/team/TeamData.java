package top.codfish.projecteteam.team;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

public class TeamData
{
    private String name;
    private UUID owner;
    private List<UUID> members;
    private Map<UUID, Long> inviteTimestamps;
    private double sharedEmc;
    private static final long INVITE_EXPIRE_MS = 5 * 60 * 1000; // 5 minutes

    public TeamData(String name, UUID owner)
    {
        this.name = name;
        this.owner = owner;
        this.members = Lists.newArrayList();
        this.inviteTimestamps = Maps.newHashMap();
        this.sharedEmc = 0;
        this.members.add(owner);
    }

    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public List<UUID> getMembers() { return members; }

    public boolean isMember(UUID uuid)
    {
        return members.contains(uuid);
    }

    public boolean isInvited(UUID uuid)
    {
        if (!inviteTimestamps.containsKey(uuid))
        {
            return false;
        }
        if (System.currentTimeMillis() - inviteTimestamps.get(uuid) > INVITE_EXPIRE_MS)
        {
            inviteTimestamps.remove(uuid);
            return false;
        }
        return true;
    }

    public void addMember(UUID uuid)
    {
        if (!members.contains(uuid))
        {
            members.add(uuid);
        }
        inviteTimestamps.remove(uuid);
    }

    public void removeMember(UUID uuid)
    {
        members.remove(uuid);
    }

    public void invite(UUID uuid)
    {
        if (!inviteTimestamps.containsKey(uuid) && !members.contains(uuid))
        {
            inviteTimestamps.put(uuid, System.currentTimeMillis());
        }
    }

    public void removeInvite(UUID uuid)
    {
        inviteTimestamps.remove(uuid);
    }

    public double getSharedEmc() { return sharedEmc; }
    public void setSharedEmc(double emc) { this.sharedEmc = emc; }

    public int getMemberCount()
    {
        return members.size();
    }

    public int getPendingInviteCount()
    {
        pruneExpiredInvites();
        return inviteTimestamps.size();
    }

    public void pruneExpiredInvites()
    {
        Iterator<Map.Entry<UUID, Long>> iter = inviteTimestamps.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<UUID, Long> entry = iter.next();
            if (System.currentTimeMillis() - entry.getValue() > INVITE_EXPIRE_MS)
            {
                iter.remove();
            }
        }
    }

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", name);
        tag.setString("Owner", owner.toString());

        NBTTagList memberList = new NBTTagList();
        for (UUID member : members)
        {
            memberList.appendTag(new NBTTagString(member.toString()));
        }
        tag.setTag("Members", memberList);

        pruneExpiredInvites();
        NBTTagList inviteList = new NBTTagList();
        for (Map.Entry<UUID, Long> entry : inviteTimestamps.entrySet())
        {
            NBTTagCompound inviteTag = new NBTTagCompound();
            inviteTag.setString("UUID", entry.getKey().toString());
            inviteTag.setLong("Timestamp", entry.getValue());
            inviteList.appendTag(inviteTag);
        }
        tag.setTag("Invites", inviteList);
        tag.setDouble("SharedEmc", sharedEmc);

        return tag;
    }

    public static TeamData readFromNBT(NBTTagCompound tag)
    {
        String name = tag.getString("Name");
        UUID owner = UUID.fromString(tag.getString("Owner"));

        TeamData team = new TeamData(name, owner);
        team.members.clear();

        NBTTagList memberList = tag.getTagList("Members", Constants.NBT.TAG_STRING);
        for (int i = 0; i < memberList.tagCount(); i++)
        {
            team.members.add(UUID.fromString(memberList.getStringTagAt(i)));
        }

        NBTTagList inviteList = tag.getTagList("Invites", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < inviteList.tagCount(); i++)
        {
            NBTTagCompound inviteTag = inviteList.getCompoundTagAt(i);
            UUID uuid = UUID.fromString(inviteTag.getString("UUID"));
            long timestamp = inviteTag.getLong("Timestamp");
            team.inviteTimestamps.put(uuid, timestamp);
        }
        team.pruneExpiredInvites();
        team.sharedEmc = tag.getDouble("SharedEmc");

        return team;
    }
}

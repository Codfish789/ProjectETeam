package top.codfish.projecteteam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import top.codfish.projecteteam.network.PacketHandler;
import top.codfish.projecteteam.network.packets.SyncTeamDataPKT;
import top.codfish.projecteteam.network.packets.SyncTransmutationPKT;
import top.codfish.projecteteam.team.TeamData;
import moze_intel.projecte.api.event.PlayerKnowledgeChangeEvent;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.playerData.Transmutation;
import moze_intel.projecte.utils.PELogger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.Constants;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

public class TeamManager
{
    private static final Map<UUID, TeamData> teamsByOwner = new HashMap<UUID, TeamData>();
    private static final Map<UUID, UUID> playerTeamMap = new HashMap<UUID, UUID>();
    private static final Map<UUID, Integer> lastKnowledgeSizes = new HashMap<UUID, Integer>();
    private static final Map<UUID, Double> memberEmcBaselines = new HashMap<UUID, Double>();
    private static File saveDir;
    private static boolean suppressEventSync;

    // ---- Setup ----

    public static void setSaveDir(File dir) { saveDir = dir; }

    // ---- Team operations ----

    public static TeamData createTeam(String name, EntityPlayer owner)
    {
        UUID ownerUUID = owner.getUniqueID();
        if (teamsByOwner.containsKey(ownerUUID) || playerTeamMap.containsKey(ownerUUID))
        {
            return null;
        }
        for (TeamData existing : teamsByOwner.values())
        {
            if (existing.getName().equalsIgnoreCase(name))
            {
                return null;
            }
        }
        TeamData team = new TeamData(name, ownerUUID);
        double ownerEmc;
        if (owner.openContainer instanceof TransmutationContainer)
        {
            ownerEmc = ((TransmutationContainer) owner.openContainer).transmutationInventory.emc;
        }
        else
        {
            ownerEmc = Transmutation.getEmc(owner);
        }
        team.setSharedEmc(ownerEmc);
        teamsByOwner.put(ownerUUID, team);
        playerTeamMap.put(ownerUUID, ownerUUID);
        memberEmcBaselines.put(ownerUUID, ownerEmc);
        Transmutation.setEmc(owner, ownerEmc);
        Transmutation.sync(owner);
        syncTeamToAllMembers(team);
        return team;
    }

    public static boolean invitePlayer(UUID teamOwner, UUID target)
    {
        TeamData team = teamsByOwner.get(teamOwner);
        if (team == null || playerTeamMap.containsKey(target) || team.isMember(target))
        {
            return false;
        }
        team.invite(target);
        syncTeamToAllMembers(team);
        return true;
    }

    public static boolean acceptInvite(EntityPlayer player, UUID teamOwner)
    {
        TeamData team = teamsByOwner.get(teamOwner);
        if (team == null || !team.isInvited(player.getUniqueID()) || playerTeamMap.containsKey(player.getUniqueID()))
        {
            return false;
        }
        team.addMember(player.getUniqueID());
        playerTeamMap.put(player.getUniqueID(), teamOwner);
        shareAllKnowledgeAndEmcToNewMember(player, team);
        lastKnowledgeSizes.put(player.getUniqueID(), Transmutation.getKnowledge(player).size());
        syncTeamToAllMembers(team);
        return true;
    }

    public static boolean declineInvite(EntityPlayer player, UUID teamOwner)
    {
        TeamData team = teamsByOwner.get(teamOwner);
        if (team == null || !team.isInvited(player.getUniqueID()))
        {
            return false;
        }
        team.removeInvite(player.getUniqueID());
        syncTeamToAllMembers(team);
        return true;
    }

    public static boolean kickMember(UUID teamOwner, UUID target)
    {
        TeamData team = teamsByOwner.get(teamOwner);
        if (team == null || !team.isMember(target) || target.equals(teamOwner))
        {
            return false;
        }
        withdrawMemberFromPool(target, team);
        team.removeMember(target);
        playerTeamMap.remove(target);
        memberEmcBaselines.remove(target);
        lastKnowledgeSizes.remove(target);
        EntityPlayerMP kickedPlayer = getPlayerByUUID(target);
        if (kickedPlayer != null)
        {
            if (Config.knowledgeDistributionMode == Config.KNOWLEDGE_MODE_CLEAR)
            {
                Transmutation.clearKnowledge(kickedPlayer);
            }
            PacketHandler.sendTo(new SyncTeamDataPKT(null, null), kickedPlayer);
        }
        syncTeamToAllMembers(team);
        return true;
    }

    public static boolean leaveTeam(EntityPlayer player)
    {
        UUID playerUUID = player.getUniqueID();
        UUID teamOwner = getTeamOwner(playerUUID);
        if (teamOwner == null)
        {
            return false;
        }
        if (teamOwner.equals(playerUUID))
        {
            return disbandTeam(playerUUID);
        }
        TeamData team = teamsByOwner.get(teamOwner);
        if (team != null)
        {
            withdrawMemberFromPool(playerUUID, team);
            team.removeMember(playerUUID);
            playerTeamMap.remove(playerUUID);
            memberEmcBaselines.remove(playerUUID);
            lastKnowledgeSizes.remove(playerUUID);
            if (Config.knowledgeDistributionMode == Config.KNOWLEDGE_MODE_CLEAR)
            {
                Transmutation.clearKnowledge(player);
            }
            syncTeamToAllMembers(team);
            PacketHandler.sendTo(new SyncTeamDataPKT(null, null), (EntityPlayerMP) player);
        }
        return true;
    }

    public static boolean disbandTeam(UUID owner)
    {
        TeamData team = teamsByOwner.remove(owner);
        if (team == null)
        {
            return false;
        }
        double totalPool = team.getSharedEmc();
        int memberCount = team.getMemberCount();
        double share;
        if (Config.emcDistributionMode == Config.EMC_MODE_CLEAR)
        {
            share = 0;
        }
        else
        {
            share = memberCount > 0 ? totalPool / memberCount : 0;
        }
        for (UUID memberUUID : team.getMembers())
        {
            playerTeamMap.remove(memberUUID);
            memberEmcBaselines.remove(memberUUID);
            lastKnowledgeSizes.remove(memberUUID);
            EntityPlayerMP player = getPlayerByUUID(memberUUID);
            if (player != null)
            {
                Transmutation.setEmc(player, share);
                if (player.openContainer instanceof TransmutationContainer)
                {
                    ((TransmutationContainer) player.openContainer).transmutationInventory.emc = share;
                }
                if (Config.knowledgeDistributionMode == Config.KNOWLEDGE_MODE_CLEAR)
                {
                    Transmutation.clearKnowledge(player);
                }
                Transmutation.sync(player);
                PacketHandler.sendTo(new SyncTeamDataPKT(null, null), player);
                PacketHandler.sendTo(new SyncTransmutationPKT(share), player);
            }
        }
        return true;
    }

    // ---- EMC shared pool ----

    /**
     * Add new member's personal EMC to the shared pool, then set all members to the pool total.
     */
    private static void shareAllKnowledgeAndEmcToNewMember(EntityPlayer newMember, TeamData team)
    {
        UUID newUUID = newMember.getUniqueID();

        // Capture original knowledge before merging (to share only unique items to existing members)
        java.util.ArrayList<ItemStack> originalNewMemberKnowledge = new java.util.ArrayList<ItemStack>();
        for (ItemStack stack : Transmutation.getKnowledge(newMember))
        {
            originalNewMemberKnowledge.add(stack.copy());
        }

        // Deposit new member's personal EMC into shared pool
        double newMemberEmc;
        if (newMember.openContainer instanceof TransmutationContainer)
        {
            newMemberEmc = ((TransmutationContainer) newMember.openContainer).transmutationInventory.emc;
        }
        else
        {
            newMemberEmc = Transmutation.getEmc(newMember);
        }
        double pool = team.getSharedEmc() + newMemberEmc;
        team.setSharedEmc(pool);

        // Batch knowledge sync — suppress per-item events
        suppressEventSync = true;
        try
        {
            for (UUID memberUUID : team.getMembers())
            {
                if (memberUUID.equals(newUUID)) continue;
                EntityPlayerMP existing = getPlayerByUUID(memberUUID);
                if (existing == null) continue;

                for (ItemStack stack : Transmutation.getKnowledge(existing))
                {
                    Transmutation.addKnowledge(stack.copy(), newMember);
                }
                for (ItemStack stack : originalNewMemberKnowledge)
                {
                    Transmutation.addKnowledge(stack.copy(), existing);
                }
            }
        }
        finally
        {
            suppressEventSync = false;
        }

        // All online members now see the shared pool
        SyncTransmutationPKT guiPacket = new SyncTransmutationPKT(pool);
        for (UUID memberUUID : team.getMembers())
        {
            EntityPlayerMP member = getPlayerByUUID(memberUUID);
            if (member == null) continue;
            Transmutation.setEmc(member, pool);
            memberEmcBaselines.put(memberUUID, pool);
            Transmutation.sync(member);
            if (member.openContainer instanceof TransmutationContainer)
            {
                ((TransmutationContainer) member.openContainer).transmutationInventory.emc = pool;
            }
            PacketHandler.sendTo(guiPacket, member);
        }
    }

    public static void syncSharedEmcPool(TeamData team)
    {
        if (team == null) return;

        double totalDelta = 0;
        for (UUID memberUUID : team.getMembers())
        {
            EntityPlayerMP member = getPlayerByUUID(memberUUID);
            if (member == null) continue;
            Double baseline = memberEmcBaselines.get(memberUUID);
            double current;
            if (member.openContainer instanceof TransmutationContainer)
            {
                current = ((TransmutationContainer) member.openContainer).transmutationInventory.emc;
            }
            else
            {
                current = Transmutation.getEmc(member);
            }
            if (baseline != null)
            {
                totalDelta += (current - baseline);
            }
        }
        if (totalDelta == 0) return;

        double newPool = team.getSharedEmc() + totalDelta;
        if (newPool < 0) newPool = 0;
        team.setSharedEmc(newPool);

        SyncTransmutationPKT guiPacket = new SyncTransmutationPKT(newPool);
        for (UUID memberUUID : team.getMembers())
        {
            EntityPlayerMP member = getPlayerByUUID(memberUUID);
            if (member == null) continue;
            Transmutation.setEmc(member, newPool);
            memberEmcBaselines.put(memberUUID, newPool);
            Transmutation.sync(member);
            if (member.openContainer instanceof TransmutationContainer)
            {
                ((TransmutationContainer) member.openContainer).transmutationInventory.emc = newPool;
            }
            PacketHandler.sendTo(guiPacket, member);
        }
    }

    /**
     * Before removing a member from the team, handle their EMC share based on config.
     */
    private static void withdrawMemberFromPool(UUID leavingUUID, TeamData team)
    {
        int count = team.getMemberCount();
        if (count <= 1) return;
        double newPool = team.getSharedEmc();
        double share = 0;

        if (Config.emcDistributionMode == Config.EMC_MODE_EQUAL)
        {
            share = team.getSharedEmc() / count;
            newPool = team.getSharedEmc() - share;
            if (newPool < 0) newPool = 0;
        }
        // MODE_CLEAR: share stays 0, pool unchanged

        team.setSharedEmc(newPool);

        EntityPlayerMP leavingPlayer = getPlayerByUUID(leavingUUID);
        if (leavingPlayer != null)
        {
            Transmutation.setEmc(leavingPlayer, share);
            memberEmcBaselines.put(leavingUUID, share);
            Transmutation.sync(leavingPlayer);
            if (leavingPlayer.openContainer instanceof TransmutationContainer)
            {
                ((TransmutationContainer) leavingPlayer.openContainer).transmutationInventory.emc = share;
            }
            PacketHandler.sendTo(new SyncTransmutationPKT(share), leavingPlayer);
        }

        // Remaining online members get updated to new pool value
        SyncTransmutationPKT guiPacket = new SyncTransmutationPKT(newPool);
        for (UUID memberUUID : team.getMembers())
        {
            if (memberUUID.equals(leavingUUID)) continue;
            EntityPlayerMP member = getPlayerByUUID(memberUUID);
            if (member == null) continue;
            Transmutation.setEmc(member, newPool);
            memberEmcBaselines.put(memberUUID, newPool);
            Transmutation.sync(member);
            if (member.openContainer instanceof TransmutationContainer)
            {
                ((TransmutationContainer) member.openContainer).transmutationInventory.emc = newPool;
            }
            PacketHandler.sendTo(guiPacket, member);
        }
    }

    // ---- Knowledge sharing ----

    public static void shareKnowledgeToOnlineMembers(EntityPlayer source)
    {
        TeamData team = getTeamForPlayer(source.getUniqueID());
        if (team == null) return;
        suppressEventSync = true;
        try
        {
            for (UUID memberUUID : team.getMembers())
            {
                if (memberUUID.equals(source.getUniqueID())) continue;
                EntityPlayerMP member = getPlayerByUUID(memberUUID);
                if (member != null)
                {
                    for (ItemStack stack : Transmutation.getKnowledge(source))
                    {
                        Transmutation.addKnowledge(stack.copy(), member);
                    }
                    Transmutation.sync(member);
                    PacketHandler.sendTo(new SyncTransmutationPKT(team.getSharedEmc()), member);
                }
            }
        }
        finally
        {
            suppressEventSync = false;
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event)
    {
        UUID uuid = event.player.getUniqueID();
        TeamData team = getTeamForPlayer(uuid);
        if (team == null) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        // Sync EMC to shared pool
        double pool = team.getSharedEmc();
        Transmutation.setEmc(player, pool);
        memberEmcBaselines.put(uuid, pool);
        if (player.openContainer instanceof TransmutationContainer)
        {
            ((TransmutationContainer) player.openContainer).transmutationInventory.emc = pool;
        }

        // Catch up on knowledge from other online members
        suppressEventSync = true;
        try
        {
            for (UUID memberUUID : team.getMembers())
            {
                if (memberUUID.equals(uuid)) continue;
                EntityPlayerMP other = getPlayerByUUID(memberUUID);
                if (other != null)
                {
                    for (ItemStack stack : Transmutation.getKnowledge(other))
                    {
                        Transmutation.addKnowledge(stack.copy(), player);
                    }
                }
            }
        }
        finally
        {
            suppressEventSync = false;
        }

        Transmutation.sync(player);
        lastKnowledgeSizes.put(uuid, Transmutation.getKnowledge(player).size());
    }

    @SubscribeEvent
    public void onPlayerKnowledgeChange(PlayerKnowledgeChangeEvent event)
    {
        if (suppressEventSync) return;
        EntityPlayerMP player = getPlayerByUUID(event.playerUUID);
        if (player == null) return;
        TeamData team = getTeamForPlayer(event.playerUUID);
        if (team == null) return;

        int currentSize = Transmutation.getKnowledge(player).size();
        Integer lastSize = lastKnowledgeSizes.get(event.playerUUID);
        lastKnowledgeSizes.put(event.playerUUID, currentSize);

        if (lastSize != null && currentSize > lastSize)
        {
            shareKnowledgeToOnlineMembers(player);
        }
        syncSharedEmcPool(team);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
        if (event.phase != ServerTickEvent.Phase.END) return;
        for (TeamData team : teamsByOwner.values())
        {
            for (UUID memberUUID : team.getMembers())
            {
                EntityPlayerMP player = getPlayerByUUID(memberUUID);
                if (player == null) continue;
                Double baseline = memberEmcBaselines.get(memberUUID);
                double current;
                if (player.openContainer instanceof TransmutationContainer)
                {
                    current = ((TransmutationContainer) player.openContainer).transmutationInventory.emc;
                }
                else
                {
                    current = Transmutation.getEmc(player);
                }
                if (baseline != null && current != baseline)
                {
                    syncSharedEmcPool(team);
                    break;
                }
            }
        }
    }

    // ---- Queries ----

    public static TeamData getTeamByOwner(UUID owner) { return teamsByOwner.get(owner); }

    public static TeamData getTeamForPlayer(UUID playerUUID)
    {
        UUID owner = playerTeamMap.get(playerUUID);
        return owner != null ? teamsByOwner.get(owner) : null;
    }

    public static UUID getTeamOwner(UUID playerUUID) { return playerTeamMap.get(playerUUID); }

    public static Collection<TeamData> getAllTeams() { return teamsByOwner.values(); }

    // ---- Internal helpers ----

    private static void syncTeamToAllMembers(TeamData team)
    {
        NBTTagCompound teamNBT = team.writeToNBT();
        Map<UUID, String> names = new HashMap<UUID, String>();
        for (UUID memberUUID : team.getMembers())
        {
            EntityPlayerMP member = getPlayerByUUID(memberUUID);
            if (member != null)
            {
                names.put(memberUUID, member.getCommandSenderName());
            }
        }
        SyncTeamDataPKT packet = new SyncTeamDataPKT(teamNBT, names);
        for (Map.Entry<UUID, String> entry : names.entrySet())
        {
            EntityPlayerMP member = getPlayerByUUID(entry.getKey());
            if (member != null)
            {
                PacketHandler.sendTo(packet, member);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static EntityPlayerMP getPlayerByUUID(UUID uuid)
    {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return null;
        for (EntityPlayerMP player : (Iterable<EntityPlayerMP>) server.getConfigurationManager().playerEntityList)
        {
            if (player.getUniqueID().equals(uuid))
            {
                return player;
            }
        }
        return null;
    }

    // ---- Persistence ----

    public static void saveAll()
    {
        if (saveDir == null) return;
        File file = new File(saveDir, "teams.dat");
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList teamList = new NBTTagList();
        for (TeamData team : teamsByOwner.values())
        {
            teamList.appendTag(team.writeToNBT());
        }
        root.setTag("Teams", teamList);
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(file);
            CompressedStreamTools.writeCompressed(root, fos);
        }
        catch (IOException e)
        {
            PELogger.logFatal("Failed to save ProjectE Team data: " + e.getMessage());
        }
        finally
        {
            if (fos != null) { try { fos.close(); } catch (IOException ignored) {} }
        }
    }

    public static void loadAll()
    {
        if (saveDir == null) return;
        File file = new File(saveDir, "teams.dat");
        if (!file.exists()) return;
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(file);
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            NBTTagList teamList = root.getTagList("Teams", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < teamList.tagCount(); i++)
            {
                TeamData team = TeamData.readFromNBT(teamList.getCompoundTagAt(i));
                teamsByOwner.put(team.getOwner(), team);
                for (UUID member : team.getMembers())
                {
                    playerTeamMap.put(member, team.getOwner());
                }
            }
        }
        catch (IOException e)
        {
            PELogger.logFatal("Failed to load ProjectE Team data: " + e.getMessage());
        }
        finally
        {
            if (fis != null) { try { fis.close(); } catch (IOException ignored) {} }
        }
    }

    public static void clearAll()
    {
        teamsByOwner.clear();
        playerTeamMap.clear();
        lastKnowledgeSizes.clear();
        memberEmcBaselines.clear();
    }
}

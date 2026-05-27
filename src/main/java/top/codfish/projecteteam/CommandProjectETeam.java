package top.codfish.projecteteam;

import java.util.List;
import java.util.UUID;

import top.codfish.projecteteam.team.TeamData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public class CommandProjectETeam extends CommandBase
{
    private static final String[] SUBCOMMANDS = {"help", "create", "invite", "accept", "decline", "kick", "leave", "disband", "info"};

    @Override
    public String getCommandName()
    {
        return "projecteteam";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return StatCollector.translateToLocal("pteam.help.syntax");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline")))
            return getListOfStringsMatchingLastWord(args, getOnlineTeamNames());
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick")))
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        return null;
    }

    @Override
    public java.util.List<String> getCommandAliases()
    {
        return java.util.Arrays.asList("pteam", "team");
    }

    private static void sendMsg(ICommandSender sender, String msg)
    {
        sender.addChatMessage(new ChatComponentText(msg));
    }

    private static void sendHelp(ICommandSender sender)
    {
        sendMsg(sender, t("pteam.help.header"));
        sendMsg(sender, t("pteam.help.create"));
        sendMsg(sender, t("pteam.help.invite"));
        sendMsg(sender, t("pteam.help.accept"));
        sendMsg(sender, t("pteam.help.decline"));
        sendMsg(sender, t("pteam.help.kick"));
        sendMsg(sender, t("pteam.help.leave"));
        sendMsg(sender, t("pteam.help.disband"));
        sendMsg(sender, t("pteam.help.info"));
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        return true;
    }

    private static String t(String key, Object... args)
    {
        String s = StatCollector.translateToLocal(key);
        return args.length > 0 ? String.format(s, args) : s;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length < 1 || args[0].equalsIgnoreCase("help"))
        {
            sendHelp(sender);
            return;
        }

        String subCmd = args[0].toLowerCase();

        if (subCmd.equals("create")) processCreate(sender, args);
        else if (subCmd.equals("invite")) processInvite(sender, args);
        else if (subCmd.equals("accept")) processAccept(sender, args);
        else if (subCmd.equals("decline")) processDecline(sender, args);
        else if (subCmd.equals("kick")) processKick(sender, args);
        else if (subCmd.equals("leave")) processLeave(sender);
        else if (subCmd.equals("disband")) processDisband(sender);
        else if (subCmd.equals("info")) processInfo(sender);
        else sendHelp(sender);
    }

    private void processCreate(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        if (args.length < 2) { sendMsg(sender, t("pteam.create.usage")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        String teamName = args[1];
        TeamData team = TeamManager.createTeam(teamName, player);
        if (team == null) sendMsg(player, t("pteam.create.fail"));
        else sendMsg(player, t("pteam.create.success", teamName));
    }

    private void processInvite(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        if (args.length < 2) { sendMsg(sender, t("pteam.invite.usage")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        UUID teamOwner = TeamManager.getTeamOwner(player.getUniqueID());
        if (teamOwner == null) { sendMsg(player, t("pteam.invite.notInTeam")); return; }
        if (!teamOwner.equals(player.getUniqueID())) { sendMsg(player, t("pteam.invite.notOwner")); return; }
        EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(args[1]);
        if (target == null) { sendMsg(player, t("pteam.invite.playerNotFound", args[1])); return; }
        if (TeamManager.invitePlayer(teamOwner, target.getUniqueID()))
        {
            String teamName = TeamManager.getTeamByOwner(teamOwner).getName();
            sendMsg(player, t("pteam.invite.success", args[1]));
            sendMsg(target, t("pteam.invite.notify", teamName, teamName));
        }
        else sendMsg(player, t("pteam.invite.fail", args[1]));
    }

    private void processAccept(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        if (args.length < 2) { sendMsg(player, t("pteam.accept.usage")); return; }
        TeamData targetTeam = findTeamByName(args[1]);
        if (targetTeam == null) { sendMsg(player, t("pteam.accept.teamNotFound", args[1])); return; }
        if (TeamManager.acceptInvite(player, targetTeam.getOwner()))
        {
            TeamData team = TeamManager.getTeamForPlayer(player.getUniqueID());
            sendMsg(player, t("pteam.accept.success", team != null ? team.getName() : "Unknown"));
            EntityPlayerMP owner = TeamManager.getPlayerByUUID(targetTeam.getOwner());
            if (owner != null) sendMsg(owner, t("pteam.accept.notifyOwner", player.getCommandSenderName()));
        }
        else sendMsg(player, t("pteam.accept.fail"));
    }

    private void processDecline(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        if (args.length < 2) { sendMsg(player, t("pteam.decline.usage")); return; }
        TeamData targetTeam = findTeamByName(args[1]);
        if (targetTeam == null) { sendMsg(player, t("pteam.accept.teamNotFound", args[1])); return; }
        if (TeamManager.declineInvite(player, targetTeam.getOwner())) sendMsg(player, t("pteam.decline.success"));
        else sendMsg(player, t("pteam.decline.fail"));
    }

    private void processKick(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        if (args.length < 2) { sendMsg(sender, t("pteam.kick.usage")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        UUID teamOwner = TeamManager.getTeamOwner(player.getUniqueID());
        if (teamOwner == null || !teamOwner.equals(player.getUniqueID())) { sendMsg(player, t("pteam.kick.notOwner")); return; }
        EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(args[1]);
        if (target == null) { sendMsg(player, t("pteam.invite.playerNotFound", args[1])); return; }
        if (TeamManager.kickMember(teamOwner, target.getUniqueID()))
        {
            sendMsg(player, t("pteam.kick.success", args[1]));
            sendMsg(target, t("pteam.kick.notify"));
        }
        else sendMsg(player, t("pteam.kick.fail", args[1]));
    }

    private void processLeave(ICommandSender sender)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        if (TeamManager.leaveTeam(player)) sendMsg(player, t("pteam.leave.success"));
        else sendMsg(player, t("pteam.leave.fail"));
    }

    private void processDisband(ICommandSender sender)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        TeamData team = TeamManager.getTeamForPlayer(player.getUniqueID());
        if (team == null || !team.getOwner().equals(player.getUniqueID())) { sendMsg(player, t("pteam.disband.notOwner")); return; }
        String teamName = team.getName();
        java.util.List<UUID> members = new java.util.ArrayList<UUID>(team.getMembers());
        if (TeamManager.disbandTeam(player.getUniqueID()))
        {
            sendMsg(player, t("pteam.disband.success", teamName));
            for (UUID memberUUID : members)
            {
                if (!memberUUID.equals(player.getUniqueID()))
                {
                    EntityPlayerMP member = TeamManager.getPlayerByUUID(memberUUID);
                    if (member != null) sendMsg(member, t("pteam.disband.notify", teamName));
                }
            }
        }
    }

    private void processInfo(ICommandSender sender)
    {
        if (!(sender instanceof EntityPlayer)) { sendMsg(sender, t("pteam.command.playerOnly")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        TeamData team = TeamManager.getTeamForPlayer(player.getUniqueID());
        if (team == null) { sendMsg(player, t("pteam.info.notInTeam")); return; }
        EntityPlayerMP owner = TeamManager.getPlayerByUUID(team.getOwner());
        String ownerName = owner != null ? owner.getCommandSenderName() : team.getOwner().toString().substring(0, 8);
        sendMsg(player, t("pteam.info.header", team.getName()));
        sendMsg(player, t("pteam.info.owner", ownerName));
        sendMsg(player, t("pteam.info.members", team.getMemberCount()));
        for (UUID memberUUID : team.getMembers())
        {
            EntityPlayerMP member = TeamManager.getPlayerByUUID(memberUUID);
            String memberName = member != null ? member.getCommandSenderName() : memberUUID.toString().substring(0, 8);
            boolean isOwner = memberUUID.equals(team.getOwner());
            sendMsg(player, (isOwner ? "  " + EnumChatFormatting.RED : "  " + EnumChatFormatting.GREEN) + memberName);
        }
        int pendingInvites = team.getPendingInviteCount();
        if (pendingInvites > 0) sendMsg(player, t("pteam.info.pendingInvites", pendingInvites));
    }

    private String[] getOnlineTeamNames()
    {
        java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        for (TeamData team : TeamManager.getAllTeams()) names.add(team.getName());
        return names.toArray(new String[0]);
    }

    private TeamData findTeamByName(String name)
    {
        for (TeamData team : TeamManager.getAllTeams())
            if (team.getName().equalsIgnoreCase(name)) return team;
        return null;
    }
}

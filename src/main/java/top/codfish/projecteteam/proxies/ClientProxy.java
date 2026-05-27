package top.codfish.projecteteam.proxies;

import java.util.Map;
import java.util.UUID;

import top.codfish.projecteteam.team.TeamData;

public class ClientProxy implements IProxy
{
    private static TeamData clientTeamData;
    private static Map<UUID, String> clientMemberNames;

    @Override
    public void registerClientOnlyEvents() {}

    @Override
    public void updateClientTeamData(TeamData team, Map<UUID, String> memberNames)
    {
        clientTeamData = team;
        clientMemberNames = memberNames;
    }

    public static TeamData getClientTeamData()
    {
        return clientTeamData;
    }

    public static Map<UUID, String> getClientMemberNames()
    {
        return clientMemberNames;
    }
}

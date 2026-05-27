package top.codfish.projecteteam.proxies;

import java.util.Map;
import java.util.UUID;

import top.codfish.projecteteam.team.TeamData;

public class ServerProxy implements IProxy
{
    @Override
    public void registerClientOnlyEvents() {}

    @Override
    public void updateClientTeamData(TeamData team, Map<UUID, String> memberNames) {}
}

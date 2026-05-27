package top.codfish.projecteteam.proxies;

import java.util.Map;
import java.util.UUID;

import top.codfish.projecteteam.team.TeamData;

public interface IProxy
{
    void registerClientOnlyEvents();
    void updateClientTeamData(TeamData team, Map<UUID, String> memberNames);
}

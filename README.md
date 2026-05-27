# ProjectE Team

A ProjectE addon for Minecraft 1.7.10 that adds **team functionality** — share EMC and item knowledge with teammates in real-time.

## Features

- **Shared EMC Pool** — All team members share a single EMC pool. Changes sync instantly while the Transmutation Table is open.
- **Shared Knowledge** — Items learned by one member are immediately unlocked for all teammates.
- **Real-time GUI Sync** — Both players see EMC and knowledge updates live without closing the table.
- **Team Management** — Create teams, invite players, kick members, leave or disband.
- **Configurable** — Choose whether leaving members keep or lose EMC and knowledge.
- **i18n** — 10 languages supported (EN, ZH, JA, KO, RU, DE, FR, ES, PT, ZH-TW).

## Commands

| Command | Description |
|---|---|
| `/projecteteam help` | Show help menu |
| `/projecteteam create <name>` | Create a new team |
| `/projecteteam invite <player>` | Invite a player to your team |
| `/projecteteam accept <team>` | Accept a pending invite |
| `/projecteteam decline <team>` | Decline a pending invite |
| `/projecteteam kick <player>` | Kick a member (owner only) |
| `/projecteteam leave` | Leave your current team |
| `/projecteteam disband` | Disband your team (owner only) |
| `/projecteteam info` | Show team information |

Aliases: `/pteam`, `/team`

## Configuration

`config/ProjectETeam.cfg`:

```ini
team {
    # EMC distribution on leave/kick/disband
    # 0 = equal (fair share), 1 = clear (member gets 0)
    S:emcDistributionMode=equal

    # Knowledge handling on leave/kick/disband
    # 0 = keep (retain items), 1 = clear (wipe knowledge)
    S:knowledgeDistributionMode=keep
}
```

## Requirements

- Minecraft 1.7.10
- Forge 10.13.4.1614+
- [ProjectE 1.7.10](https://www.curseforge.com/minecraft/mc-mods/projecte) (PE1.10.1+)

## Building

1. Place `ProjectE-1.7.10-PE1.10.1.jar` in the project root
2. Run `./gradlew build` (requires JDK 8 and Gradle 4.4.1)
3. Output: `build/libs/ProjectETeam-1.0.jar`

```
./gradlew setupDecompWorkspace
./gradlew build
```

## License

This project is provided as-is. ProjectE is developed by moze_intel.

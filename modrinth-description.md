## About

ProjectE Team adds team functionality to ProjectE — share your EMC and learned items with friends in real-time!

Two players can both have the Transmutation Table open, and when one learns a new item or earns EMC, the other sees it instantly.

## Features

- **Shared EMC Pool** — All team members draw from a single EMC pool. Burning items as one player immediately updates the EMC display for everyone.
- **Shared Knowledge** — Items unlocked by one teammate are instantly available to all members. No need to pass items around.
- **Real-time GUI Sync** — Changes appear in the Transmutation Table without closing and reopening it.
- **Team Management** — Full `/projecteteam` command system with create, invite, accept, kick, leave, disband, and info.
- **Configurable** — Choose whether leaving members keep their EMC share or lose it, and whether knowledge is retained or wiped.
- **Multi-language** — 10 languages supported.

## Commands

| Command | Description |
|---|---|
| `/projecteteam help` | Show command help |
| `/projecteteam create <name>` | Create a new team |
| `/projecteteam invite <player>` | Invite a player to join |
| `/projecteteam accept <team>` | Accept a pending invite |
| `/projecteteam decline <team>` | Decline a pending invite |
| `/projecteteam kick <player>` | Kick a member (owner only) |
| `/projecteteam leave` | Leave your team |
| `/projecteteam disband` | Disband your team (owner only) |
| `/projecteteam info` | Show team overview |

Aliases: `/pteam`, `/team`

## Configuration

Edit `config/ProjectETeam.cfg`:

- `emcDistributionMode` — `equal` (fair share) or `clear` (member gets 0)
- `knowledgeDistributionMode` — `keep` (retain items) or `clear` (wipe knowledge)

## Requirements

- Minecraft 1.7.10
- Forge 10.13.4.1614+
- [ProjectE](https://modrinth.com/mod/projecte) 1.7.10

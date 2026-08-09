# Find the Block

Multiplayer **Find-the-Block** minigame for Minecraft **Java Edition 26.1.2** (Fabric).

Above the screen a bossbar permanently shows the block being searched. The first player who finds/breaks the target block wins exactly **1 point** for their team, then the next round starts automatically with the next block from a configured, fixed order.

## Installation

1. Install a Fabric server (see below) or use your existing Fabric server.
2. Place `fabric-api-*.jar` into the `mods/` folder.
3. Place `findtheblock-*.jar` into the `mods/` folder.
4. Start the server once.
5. Stop the server.
6. Edit `config/findtheblock/` (files are auto-created on first start).
7. Start the server again.
8. Run `/findblock start`.

## Fabric version

- Minecraft: **26.1.2**
- Fabric Loader: **>= 0.18.4** (tested with 0.19.x)
- Fabric API: **>= 0.155.2+26.1.2**
- Java: **>= 25**

The mod runs on a dedicated server. It contains **no client code**, so vanilla clients can connect.

## Build

```bash
./gradlew build
```

Windows:

```cmd
gradlew.bat build
```

Output jar: `build/libs/findtheblock-1.0.0.jar`

## Configuration

All files live in `config/findtheblock/` and are auto-created on first start.

### `config.json`

```json
{
  "countdownSeconds": 5,
  "betweenRoundSeconds": 3,
  "maxRounds": 10,
  "triggerMode": "BREAK",
  "showBossBar": true,
  "showScoreboard": true,
  "allowUnassignedPlayers": false,
  "loopBlocks": false,
  "allowedDimensions": [
    "minecraft:overworld",
    "minecraft:the_nether",
    "minecraft:the_end"
  ]
}
```

| Option | Description |
|--------|-------------|
| `countdownSeconds` | Countdown before the game starts and between rounds |
| `betweenRoundSeconds` | Pause after a block is found before the next round countdown |
| `maxRounds` | Number of rounds; after the last round a winner is declared |
| `triggerMode` | `BREAK`, `PLACE` or `INTERACT` – what counts as "found" |
| `showBossBar` | Show the target block bossbar at the top of the screen |
| `showScoreboard` | Show the compact team score sidebar |
| `allowUnassignedPlayers` | Allow players without a team to find blocks (no team point) |
| `loopBlocks` | After the last block, start again with the first block |
| `allowedDimensions` | Dimensions in which the target block counts |

## Block list

`blocks.json` – fixed order, used exactly as listed (no shuffling):

```json
{
  "blocks": [
    "minecraft:oak_log",
    "minecraft:stone",
    "minecraft:coal_ore",
    "minecraft:iron_ore",
    "minecraft:gold_ore",
    "minecraft:redstone_ore",
    "minecraft:lapis_ore",
    "minecraft:diamond_ore",
    "minecraft:emerald_ore"
  ]
}
```

Invalid identifiers are logged as a warning and skipped; they never crash the game.

## Teams

`teams.json` – rename teams freely (not hardcoded):

```json
{
  "teams": [
    { "id": "team1", "name": "Team 1" },
    { "id": "team2", "name": "Team 2" }
  ]
}
```

## Player–team assignment

`players.json` – assignments are stored server-side by UUID and survive restarts:

```json
{
  "players": {
    "5b7a...uuid...": "team1"
  }
}
```

Use the in-game commands to assign players instead of editing this file by hand.

## Commands

Admin commands require operator (game master) permission.

```
/findblock start
/findblock stop
/findblock pause
/findblock resume
/findblock restart
/findblock status
/findblock next
/findblock reload

/findblock team list
/findblock team info <team>
/findblock team add <player> <team>
/findblock team remove <player>

/findblock score
/findblock score <team>
```

Example:

```
/findblock team add Steven team1
```

## Points

- Only one point per round (atomic server-side check, no double wins).
- Scores are stored in `scores.json` and survive server restarts.
- Only teams get points – no individual player scores.

## How a round feels

```
          GESUCHT: DIAMANTENERZ          (bossbar)
          Team 1: 2 Punkte               (sidebar)

Player breaks diamond ore →
  ✓ DIAMANTENERZ GEFUNDEN!
  Steven hat den Block gefunden.
  Team 1 erhält 1 Punkt!

  Nächste Runde in 5 ... 4 3 2 1
          GESUCHT: EISENERZ
```

After the configured number of rounds:

```
SPIEL BEENDET
🏆 TEAM 2 GEWINNT!
```

Ties are announced as `UNENTSCHIEDEN`.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Mod resolution failed ... requires version 0.19.3` | Update Fabric Loader on the server to `>= 0.18.4` (ideally 0.19.x) |
| `Keine gültigen Blöcke gefunden` | Check `blocks.json` – only registered block identifiers are accepted |
| No bossbar / sidebar | Set `showBossBar` / `showScoreboard` to `true` and run `/findblock reload` |
| Points lost after restart | Points are only saved when a point is awarded; scores persist in `scores.json` |
| Unassigned players can't win | Assign them with `/findblock team add <player> <team>` or set `allowUnassignedPlayers` |

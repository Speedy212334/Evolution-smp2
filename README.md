# EvolutionSMP 1.0.0

A clean-slate evolution-based Minecraft SMP plugin for Paper 26.3 and Java 25.

## Current architecture

- **One unified plugin JAR** — gameplay, GUI, persistence, and cinematic FX share one plugin lifecycle.
- **Seven paths** — Hunt, Alchemy, Enchantment, Prospector, Ender, Trade, Calamity.
- **Progression** — T1, T2, T3, T4, and separate Apex state.
- **Persistence** — player path, tier, XP, Apex state, and temporary ability state are stored in the plugin data folder.
- **Custom FX** — seven unique path sigils orbit, accelerate, and launch away during path reveal; custom ItemDisplay animations are configurable in `animations.yml`.
- **Resource pack** — generated and validated for Minecraft Java 26.3.

## Implemented gameplay

- No automatic first-join path assignment. Administrators use `/evo start <player>`.
- Path-specific XP sources for all seven paths.
- T2–T4 death drops a distinct Evolution Orb and regresses the player to T1. Apex players do not drop orbs and do not regress through this rule.
- T2, T3, and T4 have separate shaped recipes. T4 does not require a previous tier orb.
- T4 players can attempt Apex using 8 Echo Shards. Success chance: 20%. A successful ascension is broadcast.
- Trade Apex: `/evo gambit` grants 3 minutes of invulnerability, then destroys armor in the player's inventory and locks armor use for 20 minutes.
- Calamity Apex: `/evo heart` grants its combat effects for 60 seconds and displays a heart symbol for 3 minutes.
- Hunt Tier IV: `/evo veil` hides the player temporarily.
- Calamity Tier IV: `/evo fracture` releases a damaging knockback shockwave.
- Ender Tier IV: `/evo swap <player>` swaps positions with a nearby player; Void Step can rescue the player from the void, and fall damage is negated.
- Enchantment Tier IV: `/evo curse` opens a held-item curse-removal GUI.
- Passive path perks include Hunt speed/fall mitigation, Alchemy brewing and potion mastery, Prospector bonus ore yields/Auto Smelt/Haste, Enchantment lapis refunds, Trade discounts/trade-use preservation, and Calamity damage mitigation/Strength.
- Both Apex abilities currently share a 30-minute cooldown.
- The Evolution Path Changer can be issued by an admin with `/evo pathchanger <player>`; right-click it to select a new path.

## Commands

| Command | Purpose |
|---|---|
| `/evo` | Open the main menu |
| `/evo status` | View path, tier, and XP |
| `/evo recipes` | Browse crafting recipes |
| `/evo guide [topic]` | Curated in-game guide |
| `/evo start <player>` | Admin path assignment/reassignment |
| `/evo exp <player> <amount>` | Admin XP award |
| `/evo tier <player> <tier>` | Admin tier adjustment |
| `/evo apex` | T4 Apex attempt with 8 Echo Shards |
| `/evo gambit` | Trade Apex ability |
| `/evo heart` | Calamity Apex ability |
| `/evo veil` | Hunt Tier IV ability |
| `/evo fracture` | Calamity Tier IV ability |
| `/evo swap <player>` | Ender Tier IV position swap |
| `/evo curse` | Enchantment Tier IV curse-removal GUI |
| `/enderchest` | Ender Tier IV personal storage |
| `/evo pathchanger <player>` | Give a Path Changer |
| `/evofx list` | List custom animations |
| `/evofx play <id> <player> [seconds]` | Play a custom animation |
| `/evofx stop <player>` | Stop a player's custom animations |
| `/evofx reload` | Reload `animations.yml` |
| `/evofx reveal <player>` | Replay the path reveal |

## Build locally

Requirements: Java 25, Gradle 9.1+, Python 3.

```bash
gradle clean test jar generateResourcePack
```

Outputs:
- `build/libs/EvolutionSMP-1.0.0.jar`
- `build/pack/EvolutionSMP-ResourcePack-26.3.zip`

The GitHub Actions workflow builds and uploads both artifacts on pushes to `main`.

## Resource pack

```bash
python3 pack/generate_resource_pack.py --version 26.3 --output build/pack/EvolutionSMP-ResourcePack-26.3.zip
```

The generator checks distinct T2/T3/T4 orb silhouettes, Echo Shard separation, the Totem texture and animated particle atlas, seven path sigils, Path Changer, FX gem model references, PNG integrity, and pack metadata.

## Verification boundaries

CI compilation, unit tests, and resource-pack validation are automated. A live Paper 26.3 server smoke test has not yet been performed. Automatic resource-pack delivery is intentionally disabled until a real public HTTPS ZIP URL and matching SHA-1 are configured.

Earlier EvolutionSMP repositories remain historical references and are not modified by this rebuild.

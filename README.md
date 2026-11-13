# EvolutionSMP 5.0.16

A clean-slate evolution-based Minecraft SMP plugin for Paper 26.3 and Java 25.

## Current architecture

- **Two plugin JARs** — `EvolutionSMP` owns progression, gameplay, persistence, and the original GUI; optional `EvolutionFX` owns configurable ItemDisplay animations and `/evofx` controls.
- **Optional FX add-on** — EvolutionSMP remains playable without EvolutionFX; install both for the complete visual experience.
- **Seven paths** — Hunt, Alchemy, Enchantment, Prospector, Ender, Trade, Calamity.
- **Progression** — T1, T2, T3, T4, and separate Apex state.
- **Persistence** — player path, tier, XP, Apex state, and temporary ability state are stored in the plugin data folder.
- **Custom FX** — the core retains the path-awakening cinematic; the optional EvolutionFX JAR supplies configurable ItemDisplay animations, original spark/shard/rune/flare sprites, collision-aware physics bursts, layered sigils, and per-preset particle palettes.
- **Resource pack** — generated and validated for Minecraft Java 26.3.

## Implemented gameplay

- No automatic first-join path assignment. Administrators use `/evo start`.
- Path-specific XP sources for all seven paths.
- T2–T4 death drops a distinct Evolution Orb and regresses the player to T1. Apex players do not drop orbs and do not regress through this rule.
- T2, T3, and T4 have separate shaped recipes. T4 does not require a previous tier orb.
- T4 players can attempt Apex using 8 Evolution Shards. Success chance: 20%. A successful ascension is broadcast.
- Trade Apex: `/evo gambit` grants 3 minutes of invulnerability, then destroys armor in the player's inventory and locks armor use for 20 minutes.
- Calamity Apex: `/evo heart` grants its combat effects for 60 seconds and displays a heart symbol for 3 minutes.
- Hunt Tier IV: `/evo veil` hides the player for 10 seconds with a three-minute cooldown.
- Prospector Tier IV: `/evo resonance` scans loaded chunks within a 50-block radius and creates player-only, ore-coloured glowing outlines for 60 seconds with a five-minute cooldown; it does not reveal coordinates or load chunks.
- Calamity Tier IV: `/evo fracture` telegraphs an expanding ring before its damaging knockback shockwave.
- Ender Tier IV: `/evo swap <player>` swaps positions with a nearby player; Void Step can rescue the player from the void, and fall damage is negated.
- Enchantment Tier IV: `/evo curse` opens a held-item curse-removal GUI.
- Alchemy Tier IV and Apex: combine two drinkable potions in any crafting grid to fuse their effects into one potion. Distinct effects are retained; duplicate effects keep the stronger amplifier, or the longer duration when amplifiers match. Fusion consumes one of each input Potion and produces one fused Potion.
- Passive path perks include Hunt speed/fall mitigation, Alchemy brewing and potion mastery, Prospector bonus ore yields/Auto Smelt and Efficiency X on mining tools at Tier IV and Apex, Enchantment lapis refunds, Trade discounts/trade-use preservation, and Calamity damage mitigation/Strength.
- Both Apex abilities currently share a 30-minute cooldown.
- The Evolution Path Changer can be issued by an admin with `/evo pathchanger <player>`; right-click it to select a new path.

## Ability guide and controls

**There are no custom keyboard keybinds in this server-plugin build.** Abilities are activated through the chat commands below, crafting, or automatic passive effects. A server plugin cannot add a new client-side key binding by itself; that would require a client mod. The in-game guide is available with `/evo guide abilities`.

### Hunt
- **Passive — Hunter's Pace:** Speed I from T1 and Speed II from T3. From T2, fall damage is reduced by 55%. From T3, your player-kill death message is hidden.
- **Active — Veil (T4):** `/evo veil`. You become hidden from other players for 10 seconds. While active, you cannot attack, launch projectiles, use items, interact with blocks or containers, interact with entities, break/place blocks, or drop items. Any open inventory is closed when Veil begins. Cooldown: 3 minutes.

### Alchemy
- **Passive — Brewing Mastery:** Brewing time is reduced by 25% at T1, 45% at T2, 65% at T3, and to 1 tick at T4/Apex. Brewing fuel power is boosted.
- **Passive — Toxin Resistance (T2+):** Negates poison, wither, slowness, weakness, blindness, hunger, mining fatigue, nausea, darkness and levitation effects.
- **Passive — Potency (T3+):** Potion effects you consume last 1.5 times as long at T3 and twice as long at T4/Apex.
- **Active — Potion Fusion (T4/Apex):** Put exactly two ordinary drinkable Potions into a crafting grid, with no other ingredients. Their effects are combined into one Potion. If both contain the same effect, the stronger amplifier wins; if amplifiers match, the longer duration wins. Take the result with an empty cursor; shift-click is disabled. Crafting consumes one of each input Potion.

### Enchantment
- **Passive — Arcane Reserve:** Enchanting has a chance to refund one Lapis Lazuli: 10% at T1, 20% at T2/T3, and 30% at T4/Apex.
- **Active — Curse Removal (T4):** Hold the target item in your main hand and use `/evo curse`. Select Curse of Binding and/or Curse of Vanishing in the menu, then confirm. Only the selected curse types are removed.

### Prospector
- **Passive — Orecraft:** Mining ores grants evolution XP. From T2, raw iron, gold and copper drops are smelted automatically (unless using Silk Touch). Ore drops have a bonus-yield chance: 20% at T2, 30% at T3, and 50% at T4/Apex. The T4/Apex bonus adds two items when it triggers.
- **Passive — Master Tooling (T4/Apex):** Efficiency X is applied to pickaxes, axes, shovels, hoes and shears in your inventory.
- **Active — Ore Resonance (T4):** `/evo resonance`. Scans currently loaded chunks within 50 blocks and marks nearby ores with ore-specific glowing colours visible through blocks. Highlights last 60 seconds; cooldown: 5 minutes. It does not load chunks or reveal coordinates. Server-wide marker limits can mean some ores are not highlighted.

### Ender
- **Passive — Pearl Mastery (T2+):** Ender Pearl cooldown becomes 10 seconds at T2, 5 seconds at T3 and 2 seconds at T4. Pearl velocity is increased by 35% at T3 and 50% at T4.
- **Passive — Void Step (T4):** No fall damage. If you fall into the void, you are returned to your last safe ground position, if that position is still safe and loaded. Cooldown: 60 seconds.
- **Active — Void Swap (T4):** `/evo swap <player>`. Exchanges your position with an online player in the same world within 20 blocks. Both locations must be safe. Cooldown: 30 seconds.
- **Utility — Ender Storage (T4):** `/enderchest` opens your personal Ender Chest.

### Trade
- **Passive — Better Bargains (T2+):** Villager trade costs are reduced by one item from T2 and by two items at T4, subject to the trade's price limits.
- **Passive — Repeat Customer (T3+):** A trade has a 20% chance not to consume a villager trade use at T3, rising to 35% at T4.
- **Passive — Village Favour (T4):** Permanently maintained Hero of the Village II and Luck II.
- **Active — Idle Death Gambit (Apex):** `/evo gambit`. Grants 3 minutes of invulnerability. When it ends, all armour in your inventory and equipped slots is destroyed, and equipping armour is blocked for 20 minutes. Cooldown: 30 minutes.

### Calamity
- **Passive — Calamity's Toll (T1+):** Food-based healing is reduced by 10%.
- **Passive — Damage Ward (T2+):** Fire, fire ticks, lava, block/entity explosions and sonic-boom damage are reduced by 20%.
- **Passive — Might (T3+):** Permanently maintained Strength I.
- **Active — Fracture (T4):** `/evo fracture`. Shows an expanding warning ring, then creates a shockwave that deals 8 damage and launches nearby living targets within a 6-block radius. Cooldown: 20 seconds.
- **Active — Minotaur's Heart (Apex):** `/evo heart`. Grants Strength IV, Speed III and Resistance II for 60 seconds; a heart particle symbol appears above you for 3 minutes. Cooldown: 30 minutes.

### Apex and common controls
- **Apex attempt:** `/evo ascend` (or `/evo apex`) while at T4 with 8 Evolution Shards in your inventory. The attempt has a 20% success chance and consumes all 8 shards whether it succeeds or fails.
- **Status:** `/evo status` shows your path, tier and evolution XP.
- **Guide:** `/evo guide abilities` shows the ability guide in chat; `/evo guide` lists all guide topics.
- **Main menu:** `/evo` or `/evo menu`.
- **Recipes:** `/evo recipes`.
- **Path Changer:** An administrator gives it with `/evo pathchanger <player>`; right-click the item to open path selection. This is an item interaction, not a keyboard binding.

## Commands

| Command | Purpose |
|---|---|
| `/evo` | Open the main menu |
| `/evo status` | View path, tier, and XP |
| `/evo recipes` | Browse crafting recipes |
| `/evo guide [topic]` | Curated in-game guide |
| `/evo start` | Admin path assignment/reassignment |
| `/evo exp <player> <amount>` | Admin XP award |
| `/evo tier <player> <tier>` | Admin tier adjustment |
| `/evo ascend` | T4 Apex attempt with 8 Evolution Shards |
| `/evo gambit` | Trade Apex ability |
| `/evo heart` | Calamity Apex ability |
| `/evo veil` | Hunt Tier IV ability |
| `/evo resonance` | Prospector Tier IV ore highlighting |
| `/evo fracture` | Calamity Tier IV ability |
| `/evo swap <player>` | Ender Tier IV position swap |
| `/evo curse` | Enchantment Tier IV curse-removal GUI |
| `/enderchest` | Ender Tier IV personal storage |
| `/evo pathchanger <player>` | Give a Path Changer |
| `/evofx list` | List custom animations |
| `/evofx play <id> <player> [seconds]` | Play a custom animation |
| `/evofx stop <player>` | Stop a player's custom animations |
| `/evofx reload` | Reload `animations.yml` |
| `/evo reveal <player>` | Replay the path reveal |

## Build locally

Requirements: Java 25, Gradle 9.1+, Python 3.

**Windows PowerShell, Linux, and macOS**
```bash
gradle clean test jar evolutionFxJar generateResourcePack verifyArtifacts
```

This repository does not include the Gradle Wrapper, so use an installed Gradle 9.1 or newer.

The Gradle task selects `python` on Windows and `python3` elsewhere. If needed, override it with `-PpythonExecutable=py` or `-PpythonExecutable=python3`.

Outputs:
- `build/libs/EvolutionSMP-5.0.16.jar` — required gameplay/core plugin
- `build/libs/EvolutionFX-5.0.16.jar` — optional visual-effects add-on
- `build/pack/EvolutionSMP-ResourcePack-26.3.zip` — shared custom models and textures

The GitHub Actions workflow runs on pushes to `main` and pull requests targeting `main`. It runs Java 25 compilation and unit tests, builds both plugin JARs, generates and validates the 26.3 resource pack, verifies all three deliverables, and uploads them together.

## Resource pack

```powershell
python pack/generate_resource_pack.py --version 26.3 --output build/pack/EvolutionSMP-ResourcePack-26.3.zip
```

The generator checks distinct T2/T3/T4 orb silhouettes, Evolution Shard separation, the animated Totem particle atlas, seven path sigils, Path Changer, FX gem model references, PNG integrity, pack metadata, and that vanilla item/block assets are not overridden.


## Detailed custom animations

The animation library includes seven path-specific signatures: Hunt Awakening, Alchemy Catalyst, Enchantment Ascension, Prospector Resonance, Ender Rift, Trade Prosperity, and Calamity Cataclysm. Each uses its own path model, movement pattern, sound profile, and colour palette.

Animations can combine:
- Orbiting, helix, figure-eight, rise, pulse, vortex, and collision-aware physics bursts.
- Layered ground sigils with counter-rotating rings that expand over the animation cycle.
- Configurable ring point count, layers, radius, elevation, colour, size, and update interval.
- Coloured dust transitions and short Paper trail particles on physics bursts.
- Bounded particle output per animation tick and capped display-entity counts.

Install both plugin JARs in the server's `plugins` folder and restart. The EvolutionFX add-on provides `/evofx list`, `/evofx play <id> <player> [seconds]`, `/evofx stop <player>`, and `/evofx reload`. FX administration uses the `evolutionfx.admin` permission (default: operators). Ring settings are optional in `plugins/EvolutionFX/animations.yml`; set `ring-enabled: true` on an animation to activate them. The seven signature presets are examples and can be tuned without changing Java code.

## Verification boundaries

CI compilation, unit tests, and resource-pack validation are automated. A live Paper 26.3 server smoke test has not yet been performed. Automatic resource-pack delivery remains disabled until a real public HTTPS ZIP URL and matching SHA-1 are configured. Both plugin JARs and the resource pack must be installed/downloaded separately for now.

Earlier EvolutionSMP repositories remain historical references and are not modified by this rebuild.


## Reference-inspired visual presets

The optional EvolutionFX add-on includes original, project-owned visual equivalents inspired by the two supplied gameplay videos. These are not copied third-party texture files.

- `bliss-gem-shatter` — gold/ivory crystal burst, falling shard fragments, and a three-layer expanding seal.
- `pebbles-crimson-rift` — crimson vertical vortex with a broad ground sigil.
- `pebbles-impact-bloom` — short white-to-crimson hit bloom with physics-driven fragments.
- `pebbles-void-collapse` — violet portal implosion and expanding ring.
- `pebbles-radiant-seal` — wide ivory/gold triple orbit seal.
- `pebbles-cyan-spire` — rising cyan-black energy column with a base ring.

Administrators can preview them with:
- `/evofx play bliss-gem-shatter <player> 3`
- `/evofx play pebbles-crimson-rift <player> 5`
- `/evofx play pebbles-impact-bloom <player> 2`
- `/evofx play pebbles-void-collapse <player> 4`
- `/evofx play pebbles-radiant-seal <player> 5`
- `/evofx play pebbles-cyan-spire <player> 3`

The presets use the existing EvolutionFX resource-pack item models and bounded particle/display budgets. They are previewable presets, not yet automatically bound to every gameplay ability. The existing main GUI layout is intentionally unchanged.

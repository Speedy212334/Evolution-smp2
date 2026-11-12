# EvolutionSMP 1.0.0 QA and Live-Server Test Plan

Target: Paper 26.3, Java 25. Run on a disposable test server with a backup and at least two test accounts. Never run destructive Gambit/Death tests on the production world.

## 1. Build and artifact integrity

- Run `gradle clean test jar generateResourcePack verifyArtifacts`.
- Confirm the unified plugin JAR and 26.3 resource-pack ZIP are both produced.
- Confirm the resource-pack validator rejects any file under `assets/minecraft/models/`, `assets/minecraft/items/`, `assets/minecraft/textures/item/`, or `assets/minecraft/textures/block/`.
- Install the pack on a clean client and verify the seven path sigils, three Evolution Orbs, Evolution Shard, Path Changer, and FX gem.
- Verify vanilla gold ingot, compass, paper, echo shard, totem item, and all block textures/models remain vanilla. The Totem particle atlas is the only intended Minecraft-namespace override.
- Join with legacy EvolutionSMP items in inventory and confirm their custom models migrate without changing their PDC identity or function.

## 2. Progression, persistence, and commands

- First join: no path is assigned automatically.
- Run `/evo start <player>` as an in-game admin and from console; confirm the online target receives the path menu.
- Assign each of the seven paths and confirm the reveal cinematic cleans up.
- Use Path Changer at T1, T2, T3, T4, and Apex. Confirm tier is preserved, path XP resets to zero, and path cooldowns reset.
- Confirm changing paths cannot cancel Gambit's delayed armor destruction or its 20-minute armor lock.
- Restart the server and confirm path, tier, XP, Apex, cooldowns, and temporary state recover correctly.
- Confirm no XP is accumulated at T4/Apex; negative XP and invalid admin inputs are rejected cleanly.
- Confirm cancelled block breaks, cancelled potion consumption, cancelled enchanting, and cancelled teleports do not award XP.

## 3. Evolution Orbs and Apex

- Die at T2, T3, and T4. Confirm the correct orb drops and progression resets to T1.
- Pick up an orb once; confirm exactly one orb is consumed and XP is awarded once.
- Test a stacked orb entity: each pickup event consumes only one orb and awards only one orb's XP.
- Attempt pickup while unassigned; confirm no XP and no inventory pickup.
- Confirm Apex players do not drop death orbs or regress through the death rule.
- Confirm seven Evolution Shards are insufficient; eight authenticated Evolution Shards are consumed on both success and failure.
- Confirm plain Echo Shards do not count, no previous-tier orb is required, and success is exactly a 20% roll with the broadcast/title.

## 4. Ability and passive checks

### Hunt
- Veil: Hunt T4 only; 10-second duration, three-minute cooldown, no attacks/projectiles/block interaction/item drops, no chest/container interaction (including closing an inventory already open at activation), silent/invisible, hidden from current and newly joining viewers, and correctly restored after world changes, quit, or timeout.
- Hunt T3+: player kill death messages/kill-credit are suppressed; lower Hunt tiers and other paths retain normal death messages.
- Verify Hunt speed and fall mitigation at each intended tier.

### Prospector
- Ore Resonance: Prospector T4 only; scans a spherical 50-block radius in loaded chunks, displays ore-coloured outlines and accent particles only to the activating player, lasts 60 seconds, reveals no coordinates, and never loads chunks. Verify other online players and players joining mid-session cannot see either the markers or their particles.
- Verify Diamond, Lapis, Redstone, Emerald, Gold, Iron, Copper, Coal, Quartz, and Ancient Debris colours.
- Test the per-player and global marker safety caps, cleanup on timeout/quit/death/world change, immediate removal when an ore is mined, five-minute cooldown, and Path Changer cooldown reset.
- Efficiency X: original enchantment levels are restored on path/tier loss, item drop, shift-click/container transfer, drag-transfer attempts, pickup by a non-Prospector, death with keep-inventory, quit, and plugin shutdown.

### Ender
- Void Swap: valid nearby target swaps immediately; test protection plugins cancelling either teleport and confirm rollback/no cooldown on failure.
- Ender pearl cooldowns are measured in seconds, not ticks.
- Void Step and fall-damage prevention work at world-height/void boundaries.

### Calamity
- Fracture: expanding telegraph ring appears before impact; test target escape, damage radius, approximately 5–10 blocks of vertical and horizontal launch, PvP targets, mobs, cooldown, and self-exclusion. Confirm no terrain blocks are changed.
- Heart: Strength IV, Speed III, and Resistance II last 60 seconds; heart symbol lasts three minutes; both Apex abilities share their cooldown.

### Trade
- Gambit: three minutes of invulnerability; then armor destruction and 20-minute lock.
- Try equipping armor through cursor, shift-click, number-key/hotbar swap, offhand swap, right-click, and dispenser. All routes must be blocked during lock.
- Log out/rejoin during Gambit and restart the server during the timer; confirm the timer and delayed consequence resume.

### Enchantment and Alchemy
- Curse Removal: Enchantment T4 only; confirm only the original held item is modified, item swaps/path changes abort, GUI dragging cannot move items, and only selected Binding/Vanishing curses are removed (including stored curses on books).
- Potion fusion: Alchemy T4/Apex only; test two drinkable potions, different effects, duplicate effects with stronger amplifier, duplicate effects with longer duration, unsupported potion types, empty/invalid grids, rapid/double clicks, hotbar-number/drop/offhand clicks, and inventory-full behavior. Confirm exactly one input potion is consumed from each slot and exactly one fused potion is granted; shift-click is disabled, the cursor must be empty, overflow is dropped once, and no click route can duplicate output. Confirm non-Alchemists cannot claim the result.

## 5. Cinematics, FX, and cleanup

- Path reveal: seven distinct sigils orbit; six launch away; chosen sigil remains; five FX gems use the namespaced FX model; ground rings and particles match path colours.
- Interrupt reveal by quit, death, world change, starting another reveal, and plugin shutdown. Confirm every ItemDisplay is removed.
- Custom animations: test every animation ID, maximum copies, repeated play, stop, reload, offline player, and maximum runtime. Confirm no orphan Display entities.

## 6. Performance and regression

- Test with multiple players using Fracture and Ore Resonance simultaneously.
- Verify Ore Resonance never exceeds 128 markers per player or 512 globally, never loads chunks, and removes all markers/scoreboard entries.
- Mine/brew/enchant/trade/consume potions repeatedly; confirm XP is awarded once per successful action and player data is not written to disk on every XP event.
- Monitor MSPT/TPS, entity count, memory, and logs during a 30-minute mixed-ability session.
- Repeat the full build after any fix. CI success does not replace these live-server checks.

## Known live-test boundary

Automated Java 25 compilation, unit tests, resource-pack generation, and artifact validation run in GitHub Actions. PvP balance, client rendering, per-player visibility, chunk safety under real load, and compatibility with other server plugins require live Paper 26.3 testing.

## 7. Regression checks added during code review

- Brewing XP is credited only when a player actually inserts/replaces an ingredient or fuel item in the brewing stand. Taking items out, cancelled clicks, and unrelated clicks must not earn credit. Abandoned brewing-stand attribution entries expire.
- Gambit logout timing: log out during the three-minute active phase, then rejoin both before and after its scheduled end. Armor destruction must occur on rejoin if it expired offline, and the 20-minute armor lock must be measured from the original Gambit end time rather than login time.
- Void Step: simulate another plugin cancelling the rescue teleport. The cooldown must remain unused, and an unsafe or unloaded saved location must not be used.
- Custom animation config: valid simple particles and supported dust payloads render; unsupported data-bearing particle types (for example BLOCK, ITEM, TRAIL, or VIBRATION through the generic particle setting) are disabled with a clear warning instead of throwing at runtime.
- Path Changer: using the item in either hand opens one menu only and consumes exactly one token after a path is selected.
- Ore Resonance load: each session processes at most 20 horizontal columns per tick; test multiple simultaneous users and confirm the scan does not cause sustained MSPT spikes while respecting the marker caps.

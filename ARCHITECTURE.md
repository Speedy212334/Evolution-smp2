# EvolutionSMP 5.0.16 — Architecture

## Product shape
Two server plugin JARs share one resource pack:
- **EvolutionSMP**: progression, abilities, persistence, menus, guide, and path-awakening cinematic.
- **EvolutionFX**: optional visual add-on containing configurable ItemDisplay animations and /evofx administration. It depends on EvolutionSMP's public visual-event API; the core remains fully playable without the add-on.

## Compatibility
- Intended server: Paper 26.3.
- Java runtime/toolchain: 25.
- Paper API is pinned in Gradle to 26.3.build.242-stable; dependency resolution and compilation are CI gates.
- api-version is a plugin compatibility declaration, not proof of runtime testing.

## Domain rules
- Seven paths: Hunt, Alchemy, Enchantment, Prospector, Ender, Trade, Calamity.
- T1–T4 progression plus separate Apex state.
- No automatic first-join path assignment. /evo start opens path selection for all online players; each chooses their own path.
- Player state and progression changes are validated server-side and persisted.
- T2–T4 death-orb rules and the Apex exception are explicit domain rules.
- Path Changer preserves the player's tier/Apex state, resets path XP, and clears path-specific cooldowns.

## Modules
- domain: paths, tiers, player progression, and XP rules.
- service / persistence: progression orchestration, cached player state, and YAML persistence.
- platform: Paper listeners, commands, inventory adapters, and scheduler integration.
- fx: core semantic visual-event API and path-awakening cinematic.
- EvolutionFX: optional animation runner, presets, and admin controls.
- pack: deterministic namespaced item-model and texture generation plus validation.
- guide: curated in-plugin help; no external AI service.

## Visual integration
The core emits a Bukkit EvolutionVisualEvent only after supported actions succeed. EvolutionFX listens and maps the event's preset ID to its configured animation library. If EvolutionFX is absent, these events have no gameplay effect. The six video-inspired presets use original project-authored textures and bounded particle/display budgets. No vanilla item or block textures/models are replaced; the Totem particle atlas is the only intended Minecraft-namespace override.

## Quality gates
1. Resolve the pinned Paper API and Java 25 toolchain.
2. Compile the core and optional FX source sets.
3. Run automated progression, Fracture physics, and domain regression tests.
4. Verify both plugin descriptors, the visual-event bridge classes, and resource-pack metadata.
5. Validate generated model/texture references and reject vanilla item/block asset overrides.
6. Report CI results separately from live Paper/client testing.
7. Never describe an artifact as tested unless it was built and validated at the exact commit being distributed.

## Live-test boundary
A successful CI run does not replace testing on a disposable Paper 26.3 server with multiple clients. PvP balance, client rendering, visibility rules, chunk safety under load, and compatibility with other server plugins still require live verification.

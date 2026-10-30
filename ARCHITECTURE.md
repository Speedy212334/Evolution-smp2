# EvolutionSMP 1.0.0 — Architecture

## Product shape
One Paper plugin and one distributable JAR. Gameplay and cinematic FX are separate internal packages/services but are started and stopped by the same plugin lifecycle. Resource-pack generation is a deterministic build tool shipped with the source.

## Compatibility gate
- Intended server: Minecraft Java 26.3 / Paper 26.3.
- Intended Java runtime/toolchain: 25.
- The requested Paper API coordinate is configured as `26.3.build.+` and must resolve in Gradle before implementation is considered buildable. If it does not resolve, stop and report the blocker; do not silently downgrade.
- `api-version` in plugin.yml is the minimum Bukkit/Paper API compatibility declaration, not proof that the requested server target exists.

## Domain rules
- Seven paths: Hunt, Alchemy, Enchantment, Prospector, Ender, Trade, Calamity.
- T1–T4 progression plus separate Apex state.
- No automatic path assignment on first join. Admin-controlled start and reassignment.
- Player state and progression-changing operations must be validated server-side, persistent, and safe across restart/reconnect.
- T2–T4 death-orb rules and Apex exception must be explicit, tested domain rules.

## Internal modules
- `domain`: paths, tiers, player progression, requirements, XP rules.
- `application`: use cases and orchestration; no Bukkit inventory/event logic in domain rules.
- `platform`: Paper listeners, commands, inventory adapters, scheduler integration.
- `persistence`: versioned player data, migrations, safe writes.
- `fx`: semantic visual events, bounded animation lifetimes, task cleanup.
- `pack`: deterministic model/texture generation and validation.
- `guide`: curated in-plugin help; no external AI service.

## Quality gates
1. Resolve exact Paper API and Java toolchain.
2. Compile the single plugin JAR.
3. Run unit tests for progression, XP, orb/death behavior, and path registration.
4. Validate command permissions and plugin descriptor.
5. Validate generated pack metadata, models, and texture references.
6. Report CI build and live-server smoke tests separately.
7. Never describe a JAR as tested unless it was actually built and tested.

## Historical porting policy
Old repositories, v6 experiments, FX classes, and pack generators are reference material. Audit each subsystem, port only code that passes review, and convert known regressions into tests. Do not copy the old separate-FX-JAR arrangement into this unified build.

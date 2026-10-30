# EvolutionSMP

**A clean-slate evolution-based Minecraft SMP plugin.**

- Project version: `1.0.0`
- Target platform: Paper 26.3
- Target Java: 25
- Architecture: one unified plugin JAR with integrated gameplay, cinematic FX, and resource-pack generation support.

## Rebuild principles

This repository is the new implementation. Earlier EvolutionSMP repositories and releases are historical references only; they are not modified by this rebuild. Existing code and prior design decisions will be audited and selectively ported, with regressions and unresolved defects treated as test cases rather than copied forward.

## Planned systems

- Seven fully registered evolution paths.
- Four progression tiers plus Apex.
- Persistent player progression, XP, recipes, and death-orb rules.
- Admin-controlled start and reassignment; no automatic first-join path assignment.
- GUI-based path, recipe, and curse-management flows.
- Integrated cinematic FX and configurable custom animations.
- Distinct resource-pack assets for paths, path changers, and tier items.
- Built-in curated guide and help system; no external AI API.

## Build

The Gradle build and automated tests are being established as the first implementation milestone. A release is not considered complete until the unified JAR builds successfully and the gameplay, FX lifecycle, and resource-pack outputs pass validation.

## Status

Early development — architecture and build foundation.
# EvolutionSMP 5.0.16 Resource Pack

Generate the Minecraft Java 26.3 pack from the repository root:

```bash
python3 pack/generate_resource_pack.py --version 26.3 --output build/pack/EvolutionSMP-ResourcePack-26.3.zip
```

The deterministic generator validates:
- Distinct Tier II, Tier III, and Tier IV Evolution Orb silhouettes.
- A separate Evolution Shard texture.
- An animated Totem particle atlas (the vanilla Totem item texture/model is untouched).
- Seven unique path sigils and a dedicated Path Changer model.
- Six original reference-inspired FX textures: Bliss crystal, crimson rift, impact bloom, violet collapse, radiant seal, and cyan spire.
- Namespaced item definitions, model references, and every item-model referenced by animations.yml.
- No vanilla item or block model/texture overrides.
- PNG integrity and pack metadata.

The resource pack is a separate artifact shared by the EvolutionSMP core JAR and optional EvolutionFX add-on. Automatic server delivery remains disabled until a real public HTTPS hosting URL and matching SHA-1 are configured.

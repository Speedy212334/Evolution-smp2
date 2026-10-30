# EvolutionSMP 1.0.0 Resource Pack

Generate the Minecraft Java 26.3 pack from the repository root:

```bash
python3 pack/generate_resource_pack.py --version 26.3 --output build/pack/EvolutionSMP-ResourcePack-26.3.zip
```

The generator is deterministic and validates:
- Distinct Tier II, Tier III, and Tier IV orb silhouettes.
- A separate Echo Shard texture.
- A Totem of Undying texture and animated particle atlas.
- Seven unique path sigils.
- A dedicated Path Changer model.
- Minecraft 26.3 item definitions and model references.
- PNG integrity and pack metadata.

The resource pack is built as a separate artifact beside the single unified plugin JAR. Automatic server delivery remains disabled until a real public HTTPS hosting URL and matching SHA-1 are configured.

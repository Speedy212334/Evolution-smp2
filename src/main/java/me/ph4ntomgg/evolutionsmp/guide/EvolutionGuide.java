package me.ph4ntomgg.evolutionsmp.guide;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;

public final class EvolutionGuide {
    private static final Map<String, String[]> TOPICS = Map.of(
            "paths", new String[]{
                    "There are seven paths: Hunt, Alchemy, Enchantment, Prospector, Ender, Trade and Calamity.",
                    "Each path has its own XP sources and abilities. Your path is assigned by an administrator."
            },
            "tiers", new String[]{
                    "Normal progression is T1 → T2 → T3 → T4. Apex is a separate secret tier.",
                    "XP thresholds currently are 1,000 to T2, 3,500 additional XP to T3, and 9,000 additional XP to T4.",
                    "Reaching T4 does not automatically grant Apex."
            },
            "death", new String[]{
                    "Dying at T2, T3 or T4 drops a tier-specific Evolution Orb and resets the player to T1.",
                    "Apex players do not drop Evolution Orbs and do not regress through this death rule."
            },
            "orbs", new String[]{
                    "T2, T3 and T4 Evolution Orbs have distinct items and crafting recipes.",
                    "T2 uses iron, diamond and emerald blocks. T3 uses netherite ingots, totems and a beacon.",
                    "T4 uses a Nether Star, totems and Evolution Shards. It does not require a previous tier orb."
            },
            "apex", new String[]{
                    "Reach T4, then use /evo ascend while carrying 8 Evolution Shards. The attempt has a 20% success chance and consumes the shards.",
                    "On success, the server broadcasts that the player has reached peak of evolution. Apex players do not drop death orbs."
            },
            "abilities", new String[]{
                    "KEYBINDS: This build has no custom keyboard keybinds. Use the commands below in chat. Passive perks activate automatically; right-click a Path Changer to open its menu.",
                    "HUNT — Passive: Speed I from T1, Speed II from T3; from T2, take 55% less fall damage. From T3, your player-kill death message is hidden. Active at T4: /evo veil hides you from other players for 10 seconds; you cannot attack, shoot, use items, interact with blocks/containers/entities, break/place blocks, or drop items while hidden; any open inventory is closed. Cooldown: 3 minutes.",
                    "ALCHEMY — Passive: brewing becomes faster (T1 25%, T2 45%, T3 65%, T4/Apex effectively instant); brewing fuel is boosted. From T2, resist poison, wither, slowness, weakness, blindness, hunger, mining fatigue, nausea, darkness and levitation. From T3, consumed potion effects last 1.5x longer; T4/Apex: 2x. Active at T4/Apex: place exactly two drinkable Potions in a crafting grid to fuse their effects into one Potion. Take the result normally with an empty cursor; shift-click is disabled.",
                    "ENCHANTMENT — Passive: enchanting can refund 1 Lapis Lazuli: 10% chance at T1, 20% at T2/T3, 30% at T4/Apex. Active at T4: hold the item in your MAIN hand, run /evo curse, select Curse of Binding and/or Curse of Vanishing in the menu, then confirm.",
                    "PROSPECTOR — Passive: mining ores grants evolution XP. From T2, raw iron/gold/copper drops auto-smelt and ore drops can receive bonus yield (20% at T2, 30% at T3, 50% at T4/Apex; T4/Apex bonus is +2 items). T4/Apex: Efficiency X is applied to mining tools in your inventory. Active at T4: /evo resonance highlights ores in currently loaded chunks within 50 blocks for 60 seconds; each ore has its own colour. Cooldown: 5 minutes. No coordinates are shown.",
                    "ENDER — Passive: Ender Pearls recharge faster (10 seconds at T2, 5 seconds at T3, 2 seconds at T4); pearl speed increases by 35% at T3 and 50% at T4. T4 also prevents fall damage and automatically returns you to your last safe ground location if you fall into the void (60-second cooldown). Active at T4: /evo swap <player> swaps positions with a player within 20 blocks in the same world (30-second cooldown). /enderchest opens your personal Ender Chest.",
                    "TRADE — Passive: from T2, villager trades cost 1 fewer item; T4 discount is 2 (subject to the trade's price limit). From T3, a trade has a 20% chance not to use up a trade; T4 chance is 35%. T4 also grants Hero of the Village II and Luck II. Active at Apex: /evo gambit gives 3 minutes of invulnerability. When it ends, all armour in your inventory/equipment is destroyed and armour cannot be equipped for 20 minutes. Cooldown: 30 minutes.",
                    "CALAMITY — Passive: from T1, food-based healing is reduced by 10%; from T2, fire, lava, explosions and sonic-boom damage are reduced by 20%; from T3, gain Strength I. Active at T4: /evo fracture creates a short warning ring, then deals 8 damage and launches nearby living targets within 6 blocks. Cooldown: 20 seconds. Active at Apex: /evo heart grants Strength IV, Speed III and Resistance II for 60 seconds; the heart particle symbol lasts 3 minutes. Cooldown: 30 minutes.",
                    "APEX — /evo ascend (or /evo apex) attempts Apex from T4. It consumes 8 Evolution Shards whether it succeeds or fails. Success chance: 20%.",
                    "All listed abilities are activated by commands, crafting, menus or automatic passives—not by custom keyboard bindings. Check /evo status for your current path and tier."
            },
            "xp", new String[]{
                    "Hunt gains XP from hostile mob and player kills.",
                    "Alchemy gains XP from completed brewing batches and potion consumption.",
                    "Prospector gains XP from mining ores. Enchantment gains XP from enchanting.",
                    "Ender gains XP from Ender Pearl travel and entering the End. Trade gains XP from villager trades.",
                    "Calamity gains XP from boss kills."
            }
    );

    private EvolutionGuide() {}

    public static void send(CommandSender sender, String topic) {
        if (topic == null || topic.isBlank()) {
            sender.sendMessage(ChatColor.GOLD + "Evolution Guide: paths, tiers, death, orbs, apex, abilities, xp");
            sender.sendMessage(ChatColor.GRAY + "Use /evo guide <topic> for a short explanation.");
            return;
        }
        String key = topic.toLowerCase(Locale.ROOT);
        String[] lines = TOPICS.get(key);
        if (lines == null) {
            sender.sendMessage(ChatColor.RED + "I don't have a guide entry for that topic yet.");
            send(sender, null);
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Evolution Guide » " + key.toUpperCase(Locale.ROOT));
        for (String line : lines) sender.sendMessage(ChatColor.GRAY + "• " + line);
    }
}

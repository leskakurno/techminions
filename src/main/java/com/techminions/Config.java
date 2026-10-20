package com.techminions;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BASE_TICKS = BUILDER
            .comment("Ticks per platform step (place one block / break one block) at minion level I. 140 = 7 seconds, like Hypixel. "
                    + "Levels follow the Hypixel curve (7, 7, 6, 6, 5, 5, 4.5, 4.5, 4, 4, 3.5 s at 140); other values scale it.")
            .defineInRange("baseTicksPerBlock", 140, 20, 6000);

    public static final ModConfigSpec.IntValue MIN_TICKS = BUILDER
            .comment("Absolute minimum ticks per mined block (prevents lag from over-upgraded minions).")
            .defineInRange("minTicksPerBlock", 10, 5, 6000);

    public static final ModConfigSpec.IntValue PLATFORM_Y_OFFSET = BUILDER
            .comment("Vertical offset of the 5x5 platform relative to the minion's feet block. 0 = same level as the minion, -1 = one block below (only placed into empty air).")
            .defineInRange("platformYOffset", 0, -1, 2);

    public static final ModConfigSpec.DoubleValue UPGRADE_COST_MULTIPLIER = BUILDER
            .comment("Multiplier for minion upgrade costs (1.0 = default). Set the same value on server and client.")
            .defineInRange("upgradeCostMultiplier", 1.0, 0.1, 20.0);

    public static final ModConfigSpec.IntValue HUNT_MOB_SPAWN_SEC = BUILDER
            .comment("Hunter minions: seconds for a normal mob to 'spawn' (from the hunt egg) before it can be killed.")
            .defineInRange("huntMobSpawnSeconds", 25, 1, 100000);

    public static final ModConfigSpec.IntValue HUNT_MOB_KILL_SEC = BUILDER
            .comment("Hunter minions: seconds it takes to kill a normal mob.")
            .defineInRange("huntMobKillSeconds", 25, 1, 100000);

    public static final ModConfigSpec.IntValue HUNT_BOSS_SPAWN_MIN = BUILDER
            .comment("Hunter minions: minutes for a boss to 'spawn' (from the hunt egg).")
            .defineInRange("huntBossSpawnMinutes", 60, 1, 100000);

    public static final ModConfigSpec.IntValue HUNT_BOSS_KILL_MIN = BUILDER
            .comment("Hunter minions: minutes it takes to kill a boss.")
            .defineInRange("huntBossKillMinutes", 20, 1, 100000);

    public static final ModConfigSpec.BooleanValue HUNT_SCALE = BUILDER
            .comment("If true, minion level and Speed Upgrades shorten the hunt times above (like mining). Default false = exact times.")
            .define("huntScaleWithUpgrades", false);

    public static final ModConfigSpec.DoubleValue EGG_CHANCE_PER_LEVEL = BUILDER
            .comment("Egg Collector enchantment: chance (0.001 = 0.1%) per level that a mob killed by a player drops its Hunt Egg.")
            .defineInRange("eggChancePerLevel", 0.001, 0.0, 0.2);

    public static final ModConfigSpec.DoubleValue EGG_LOOTING_BONUS = BUILDER
            .comment("Each Looting level on the same weapon raises the egg chance by this fraction (0.5 = +50% per level).")
            .defineInRange("eggLootingBonusPerLevel", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue EGG_BOSS_MULT = BUILDER
            .comment("Multiplier of the egg chance for bosses (boss kills are rare, so they get a higher chance).")
            .defineInRange("eggBossChanceMultiplier", 5.0, 0.0, 100.0);

    private static final String DEFAULT_BOSSES = String.join(",",
            "twilightforest:naga", "twilightforest:hydra", "twilightforest:lich", "twilightforest:minoshroom",
            "twilightforest:knight_phantom", "twilightforest:ur_ghast", "twilightforest:alpha_yeti",
            "twilightforest:snow_queen",
            "mowziesmobs:ferrous_wroughtnaut", "mowziesmobs:frostmaw", "mowziesmobs:umvuthi", "mowziesmobs:sculptor",
            "cataclysm:ender_guardian", "cataclysm:netherite_monstrosity", "cataclysm:ignis", "cataclysm:the_harbinger",
            "cataclysm:the_leviathan", "cataclysm:scylla", "cataclysm:ancient_remnant", "cataclysm:maledictus",
            "alexscaves:luxtructosaurus", "alexscaves:tremorzilla", "alexsmobs:void_worm",
            "graveyard:lich", "graveyard:nightmare",
            "aether:slider", "aether:valkyrie_queen", "aether:sun_spirit");

    public static final ModConfigSpec.ConfigValue<String> BOSS_ENTITIES = BUILDER
            .comment("Extra entity ids (comma-separated) that count as bosses for hunter minions and Hunt Eggs, "
                    + "in addition to everything in the c:bosses entity tag. Unknown ids are simply ignored.")
            .define("bossEntities", DEFAULT_BOSSES);

    private static String bossRaw;
    private static java.util.Set<String> bossSet = java.util.Set.of();

    /** Ids существ-боссов из конфига (разбор кэшируется). */
    public static synchronized java.util.Set<String> bossEntityIds() {
        String raw = BOSS_ENTITIES.get();
        if (!raw.equals(bossRaw)) {
            java.util.Set<String> set = new java.util.HashSet<>();
            for (String part : raw.split(",")) {
                String id = part.trim();
                if (!id.isEmpty()) {
                    set.add(id);
                }
            }
            bossSet = set;
            bossRaw = raw;
        }
        return bossSet;
    }

    // ---------- v3.0: управление совместимостью и лутом охотников ----------
    public static final ModConfigSpec.IntValue PLATFORM_RADIUS = BUILDER
            .comment("Radius of the mining platform around a minion: 1 = 3x3, 2 = 5x5 (default), 3 = 7x7.")
            .defineInRange("platformRadius", 2, 1, 3);

    public static final ModConfigSpec.DoubleValue HUNT_BOSS_LOOT_CHANCE = BUILDER
            .comment("Hunter minions: chance (0.0 - 1.0) that each item of a BOSS loot roll is actually kept. 1.0 = everything, lower it to make boss hunting less generous.")
            .defineInRange("huntBossLootChance", 1.0, 0.0, 1.0);

    public static final ModConfigSpec.ConfigValue<String> HUNT_LOOT_BLACKLIST = BUILDER
            .comment("Hunter minions: item ids (comma-separated, e.g. 'twilightforest:lich_trophy') that hunters never collect. "
                    + "Use it for progression-critical or unique items.")
            .define("huntLootBlacklist", "");

    public static final ModConfigSpec.ConfigValue<String> DISABLED_MODS = BUILDER
            .comment("Mod ids (comma-separated, e.g. 'mekanism,thermal') whose minions are switched off. "
                    + "A minion with several possible mods stays on while at least one of them is not listed here.")
            .define("disabledMods", "");

    public static final ModConfigSpec.ConfigValue<String> DISABLED_MINIONS = BUILDER
            .comment("Minion types (comma-separated, e.g. 'iridium,hunt_lich') that are switched off. Names are the ones in the log at startup.")
            .define("disabledMinions", "");

    public static final ModConfigSpec.ConfigValue<String> DISABLED_BOSSES = BUILDER
            .comment("Entity ids (comma-separated, e.g. 'twilightforest:hydra') whose Hunt Eggs are not accepted by any hunter minion.")
            .define("disabledEntities", "");

    private static final java.util.Map<String, java.util.Set<String>> CSV_CACHE = new java.util.HashMap<>();

    /** Разбор списка через запятую (с кэшем по исходной строке). */
    public static synchronized java.util.Set<String> csv(String raw) {
        return CSV_CACHE.computeIfAbsent(raw, r -> {
            java.util.Set<String> set = new java.util.HashSet<>();
            for (String part : r.split(",")) {
                String id = part.trim();
                if (!id.isEmpty()) {
                    set.add(id);
                }
            }
            return set;
        });
    }

    public static java.util.Set<String> disabledMods() {
        return csv(DISABLED_MODS.get());
    }

    public static java.util.Set<String> disabledMinions() {
        return csv(DISABLED_MINIONS.get());
    }

    public static java.util.Set<String> disabledEntities() {
        return csv(DISABLED_BOSSES.get());
    }

    public static java.util.Set<String> huntLootBlacklist() {
        return csv(HUNT_LOOT_BLACKLIST.get());
    }

    // ---------- v3.2: механизмы, планшет ----------
    private static final String DEFAULT_MACHINES = String.join(",",
            "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:brewing_stand",
            "mekanism", "mekanismgenerators", "thermal", "immersiveengineering", "create", "oritech", "powah",
            "draconicevolution", "create_new_age", "modern_industrialization", "ae2", "enderio",
            "industrialforegoing", "pneumaticcraft");

    public static final ModConfigSpec.ConfigValue<String> MACHINE_BLOCKS = BUILDER
            .comment("Machines a minion with the Machine Feed / Machine Collect upgrades may work with. "
                    + "Entries are mod ids (all blocks of that mod) or exact block ids (required for minecraft: blocks). "
                    + "Chests, barrels, hoppers, drawers, pipes and other storage / transport blocks are always ignored.")
            .define("machineBlocks", DEFAULT_MACHINES);

    public static final ModConfigSpec.IntValue MACHINE_IO_TICKS = BUILDER
            .comment("How often (in ticks) a minion exchanges items with adjacent machines.")
            .defineInRange("machineIoTicks", 20, 5, 1200);

    public static final ModConfigSpec.IntValue MACHINE_IO_ITEMS = BUILDER
            .comment("Maximum items moved per machine per exchange.")
            .defineInRange("machineIoItems", 16, 1, 256);

    public static final ModConfigSpec.IntValue REGISTRY_TICKS = BUILDER
            .comment("How often (in ticks) a minion reports its status to the Minions tab / Tablet.")
            .defineInRange("statusReportTicks", 100, 20, 1200);

    public static final ModConfigSpec.ConfigValue<String> UNIVERSAL_TAGS = BUILDER
            .comment("Item tags the Universal Miner accepts as a sample (comma separated). Works with any mod that uses these tags.")
            .define("universalMinerTags", "c:raw_materials,c:gems,c:dusts,c:crops,minecraft:logs");

    public static final ModConfigSpec.ConfigValue<String> UNIVERSAL_BLACKLIST = BUILDER
            .comment("Items the Universal Miner must never accept (comma separated item ids).")
            .define("universalMinerBlacklist", "minecraft:netherite_scrap,minecraft:nether_star");

    public static final ModConfigSpec.BooleanValue NOTIFICATIONS = BUILDER
            .comment("Send the minion's owner a chat message when a minion stops (storage full, needs setup, no egg...).")
            .define("notifications", true);

    public static final ModConfigSpec.IntValue NOTIFY_COOLDOWN_SEC = BUILDER
            .comment("Minimum seconds between two notifications of the same kind from the same minion.")
            .defineInRange("notificationCooldownSeconds", 300, 10, 86400);

    public static java.util.Set<String> machineBlocks() {
        return csv(MACHINE_BLOCKS.get());
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}

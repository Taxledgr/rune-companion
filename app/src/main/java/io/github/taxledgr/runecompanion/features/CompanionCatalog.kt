package io.github.taxledgr.runecompanion.features

import io.github.taxledgr.runecompanion.toolkit.HiscoreCatalog

data class BankedXpPreset(
    val category: String,
    val item: String,
    val skill: String,
    val xpEach: Double,
)

data class FarmPatchPreset(val category: String, val name: String)

data class CropPreset(
    val category: String,
    val name: String,
    val minutes: Int,
    val note: String = "",
)

data class SlayerPreset(
    val category: String,
    val monster: String,
    val style: String,
    val locations: String,
    val requiredItems: String,
    val notes: String,
)

data class ActivityPreset(val category: String, val name: String)

data class CollectionPreset(
    val category: String,
    val item: String,
    val denominator: Int,
    val sourceActivity: String? = null,
)

data class RoutinePreset(
    val category: String,
    val title: String,
    val weekly: Boolean,
)

data class LoadoutPreset(
    val category: String,
    val name: String,
    val inventory: String,
    val equipment: String,
    val notes: String,
)

object CompanionCatalog {
    private val RAID_ACTIVITIES = setOf(
        "Chambers of Xeric",
        "Chambers of Xeric: Challenge Mode",
        "Theatre of Blood",
        "Theatre of Blood: Hard Mode",
        "Tombs of Amascut",
        "Tombs of Amascut: Expert Mode",
    )
    private val WILDERNESS_ACTIVITIES = setOf(
        "Artio", "Callisto", "Calvar'ion", "Chaos Elemental", "Chaos Fanatic",
        "Crazy Archaeologist", "King Black Dragon", "Scorpia", "Spindel",
        "Venenatis", "Vet'ion",
    )
    private val SKILLING_ACTIVITIES = setOf(
        "Soul Wars Zeal", "Rifts closed", "Colosseum Glory", "Barrows Chests",
        "Hespori", "Lunar Chests", "Mimic", "Tempoross", "The Gauntlet",
        "The Corrupted Gauntlet", "Wintertodt", "Zalcano",
    )

    val skills = HiscoreCatalog.skillNames.filterNot { it == "Overall" }

    val slayerTaskNames = listOf(
        "Aberrant spectres",
        "Abyssal demons",
        "The Abyssal Sire",
        "The Alchemical Hydra",
        "Ankou",
        "Aquanites",
        "Araxxor",
        "Araxytes",
        "Aviansies",
        "Bandits",
        "Banshees",
        "Barrows Brothers",
        "Basilisks",
        "Bats",
        "Bears",
        "Birds",
        "Black demons",
        "Black dragons",
        "Black Knights",
        "Bloodveld",
        "Blue dragons",
        "Brine rats",
        "Callisto",
        "Catablepon",
        "Cave bugs",
        "Cave crawlers",
        "Cave horrors",
        "Cave kraken",
        "Cave slimes",
        "Cerberus",
        "Chaos druids",
        "The Chaos Elemental",
        "The Chaos Fanatic",
        "Cockatrice",
        "Cows",
        "Crabs",
        "Crawling hands",
        "Crazy Archaeologists",
        "Crocodiles",
        "Custodian Stalkers",
        "Dagannoth",
        "Dagannoth Kings",
        "Dark beasts",
        "Dark warriors",
        "Deranged Archaeologist",
        "Dogs",
        "Drakes",
        "Duke Sucellus",
        "Dust devils",
        "Dwarves",
        "Earth warriors",
        "Elves",
        "Ents",
        "Fever spiders",
        "Fire giants",
        "Fleshcrawlers",
        "Fossil island wyverns",
        "Frost dragons",
        "Gargoyles",
        "General Graardor",
        "Ghosts",
        "Ghouls",
        "The Giant Mole",
        "Goblins",
        "Greater demons",
        "Green dragons",
        "The Grotesque Guardians",
        "Gryphons",
        "Harpie bug swarms",
        "Hellhounds",
        "Hill giants",
        "Hobgoblins",
        "Hydras",
        "Icefiends",
        "Ice giants",
        "Ice warriors",
        "Infernal mages",
        "TzTok-Jad",
        "Jellies",
        "Jungle horrors",
        "Kalphites",
        "The Kalphite Queen",
        "Killerwatts",
        "The King Black Dragon",
        "The Cave Kraken Boss",
        "Kree'arra",
        "K'ril Tsutsaroth",
        "Kurask",
        "Lava Dragons",
        "Lesser demons",
        "Lesser Nagua",
        "Lizardmen",
        "Lizards",
        "The Maggot King",
        "Magic axes",
        "Mammoths",
        "Metal dragons",
        "Minotaurs",
        "Mogres",
        "Molanisks",
        "Monkeys",
        "Moss giants",
        "Mutated zygomites",
        "Nechryael",
        "Ogres",
        "Otherworldly beings",
        "The Phantom Muspah",
        "Pirates",
        "Pyrefiends",
        "Rats",
        "Red dragons",
        "Revenants",
        "Rockslugs",
        "Rogues",
        "Sarachnis",
        "Scabarites",
        "Scorpia",
        "Scorpions",
        "Sea snakes",
        "Shades",
        "Shadow warriors",
        "The Shellbane Gryphon",
        "Skeletal wyverns",
        "Skeletons",
        "Smoke devils",
        "Sourhogs",
        "Spiders",
        "Spiritual creatures",
        "Suqahs",
        "Terror dogs",
        "The Leviathan",
        "The Whisperer",
        "The Thermonuclear Smoke Devil",
        "Trolls",
        "Turoth",
        "Tzhaar",
        "Vampyres",
        "Vardorvis",
        "Venenatis",
        "Vet'ion",
        "Vorkath",
        "Wall beasts",
        "Warped Creatures",
        "Waterfiends",
        "Werewolves",
        "Wolves",
        "Wyrms",
        "Commander Zilyana",
        "Zombies",
        "TzKal-Zuk",
        "Zulrah",
    ).sortedBy { it.removePrefix("The ").lowercase() }

    fun slayerTaskCategory(name: String): String {
        val first = name.removePrefix("The ").firstOrNull()?.uppercaseChar() ?: return "Other"
        return when (first) {
            in 'A'..'F' -> "A–F"
            in 'G'..'L' -> "G–L"
            in 'M'..'R' -> "M–R"
            in 'S'..'Z' -> "S–Z"
            else -> "Other"
        }
    }

    val bankedXp = listOf(
        BankedXpPreset("Herblore", "Attack potion", "Herblore", 25.0),
        BankedXpPreset("Herblore", "Strength potion", "Herblore", 50.0),
        BankedXpPreset("Herblore", "Prayer potion", "Herblore", 87.5),
        BankedXpPreset("Herblore", "Super attack", "Herblore", 100.0),
        BankedXpPreset("Herblore", "Super strength", "Herblore", 125.0),
        BankedXpPreset("Herblore", "Super restore", "Herblore", 142.5),
        BankedXpPreset("Herblore", "Super defence", "Herblore", 150.0),
        BankedXpPreset("Herblore", "Ranging potion", "Herblore", 162.5),
        BankedXpPreset("Herblore", "Magic potion", "Herblore", 172.5),
        BankedXpPreset("Herblore", "Saradomin brew", "Herblore", 180.0),
        BankedXpPreset("Prayer", "Dragon bones — gilded altar", "Prayer", 252.0),
        BankedXpPreset("Prayer", "Dragon bones — Ectofuntus", "Prayer", 288.0),
        BankedXpPreset("Cooking", "Cooked karambwan", "Cooking", 190.0),
        BankedXpPreset("Cooking", "Shark", "Cooking", 210.0),
        BankedXpPreset("Cooking", "Anglerfish", "Cooking", 230.0),
        BankedXpPreset("Crafting", "Water orb to battlestaff", "Crafting", 100.0),
        BankedXpPreset("Smithing", "Cannonball", "Smithing", 25.6),
        BankedXpPreset("Fletching", "Yew longbow (u)", "Fletching", 75.0),
        BankedXpPreset("Fletching", "Magic longbow (u)", "Fletching", 91.5),
    )

    val farmPatches = listOf(
        FarmPatchPreset("Herb", "Falador herb"),
        FarmPatchPreset("Herb", "Catherby herb"),
        FarmPatchPreset("Herb", "Ardougne herb"),
        FarmPatchPreset("Herb", "Morytania herb"),
        FarmPatchPreset("Herb", "Hosidius herb"),
        FarmPatchPreset("Herb", "Troll Stronghold herb"),
        FarmPatchPreset("Herb", "Weiss herb"),
        FarmPatchPreset("Herb", "Harmony Island herb"),
        FarmPatchPreset("Herb", "Civitas illa Fortis herb"),
        FarmPatchPreset("Allotment", "Falador allotment"),
        FarmPatchPreset("Allotment", "Catherby allotment"),
        FarmPatchPreset("Allotment", "Ardougne allotment"),
        FarmPatchPreset("Allotment", "Morytania allotment"),
        FarmPatchPreset("Allotment", "Hosidius allotment"),
        FarmPatchPreset("Tree", "Lumbridge tree"),
        FarmPatchPreset("Tree", "Varrock tree"),
        FarmPatchPreset("Tree", "Falador tree"),
        FarmPatchPreset("Tree", "Taverley tree"),
        FarmPatchPreset("Tree", "Gnome Stronghold tree"),
        FarmPatchPreset("Fruit tree", "Catherby fruit tree"),
        FarmPatchPreset("Fruit tree", "Gnome Stronghold fruit tree"),
        FarmPatchPreset("Fruit tree", "Tree Gnome Village fruit tree"),
        FarmPatchPreset("Fruit tree", "Brimhaven fruit tree"),
        FarmPatchPreset("Fruit tree", "Lletya fruit tree"),
        FarmPatchPreset("Fruit tree", "Farming Guild fruit tree"),
        FarmPatchPreset("Special", "Farming Guild Hespori"),
        FarmPatchPreset("Special", "Fossil Island hardwood"),
        FarmPatchPreset("Special", "Farming Guild redwood"),
    )

    val crops = listOf(
        CropPreset("Herb", "Guam", 80),
        CropPreset("Herb", "Ranarr", 80),
        CropPreset("Herb", "Toadflax", 80),
        CropPreset("Herb", "Irit", 80),
        CropPreset("Herb", "Avantoe", 80),
        CropPreset("Herb", "Kwuarm", 80),
        CropPreset("Herb", "Snapdragon", 80),
        CropPreset("Herb", "Cadantine", 80),
        CropPreset("Herb", "Lantadyme", 80),
        CropPreset("Herb", "Dwarf weed", 80),
        CropPreset("Herb", "Torstol", 80),
        CropPreset("Allotment", "Potato", 40),
        CropPreset("Allotment", "Tomato", 40),
        CropPreset("Allotment", "Sweetcorn", 60),
        CropPreset("Allotment", "Watermelon", 80),
        CropPreset("Allotment", "Snape grass", 80),
        CropPreset("Flower", "Limpwurt", 20),
        CropPreset("Flower", "White lily", 20),
        CropPreset("Tree", "Oak", 200),
        CropPreset("Tree", "Willow", 280),
        CropPreset("Tree", "Maple", 320),
        CropPreset("Tree", "Yew", 400),
        CropPreset("Tree", "Magic", 480),
        CropPreset("Fruit tree", "Apple", 960),
        CropPreset("Fruit tree", "Banana", 960),
        CropPreset("Fruit tree", "Papaya", 960),
        CropPreset("Fruit tree", "Palm", 960),
        CropPreset("Fruit tree", "Dragonfruit", 960),
        CropPreset("Special", "Hespori", 1_920, "Growth time can vary"),
        CropPreset("Special", "Teak", 3_840),
        CropPreset("Special", "Mahogany", 5_120),
        CropPreset("Special", "Redwood", 6_400),
    )

    val slayer = listOf(
        SlayerPreset(
            "Common", "Aberrant spectres", "Protect from Magic; melee or ranged",
            "Slayer Tower; Stronghold Slayer Cave", "Nose peg or Slayer helmet",
            "Bring herb sack for common herb drops.",
        ),
        SlayerPreset(
            "Common", "Bloodveld", "Melee; barrage in multi-combat areas",
            "Catacombs of Kourend; Slayer Tower; Stronghold Slayer Cave", "None",
            "Magic defence is high; use melee when not barraging.",
        ),
        SlayerPreset(
            "Common", "Dagannoth", "Melee, ranged, or barrage",
            "Lighthouse; Catacombs of Kourend; Waterbirth Island", "None",
            "The Lighthouse is fast cannon experience; Catacombs is multi-combat.",
        ),
        SlayerPreset(
            "Common", "Fire giants", "Melee or ranged",
            "Waterfall Dungeon; Catacombs of Kourend; Giants' Den", "None",
            "Catacombs variants can be fought in multi-combat.",
        ),
        SlayerPreset(
            "Common", "Greater demons", "Melee or ranged",
            "Catacombs of Kourend; Chasm of Fire; Wilderness Slayer Cave", "None",
            "K'ril Tsutsaroth and Skotizo can count where task rules permit.",
        ),
        SlayerPreset(
            "Common", "Hellhounds", "Melee or ranged",
            "Catacombs of Kourend; Taverley Dungeon; Witchaven Dungeon", "None",
            "Cerberus is the high-level boss alternative.",
        ),
        SlayerPreset(
            "Common", "Kalphite", "Melee or ranged",
            "Kalphite Cave; Kalphite Lair; Slayer Cave", "Rope for some entrances",
            "Cannoning workers or soldiers is the fast option.",
        ),
        SlayerPreset(
            "Common", "Black demons", "Melee or ranged",
            "Catacombs; Chasm of Fire; Taverley Dungeon", "None",
            "Demonic gorillas are an alternative after their quest unlock.",
        ),
        SlayerPreset(
            "Burst / barrage", "Dust devils", "Ice burst or barrage",
            "Catacombs of Kourend; Smoke Dungeon", "Face mask or Slayer helmet",
            "Stack them in multi-combat; bring prayer gear and bonecrusher if available.",
        ),
        SlayerPreset(
            "Burst / barrage", "Nechryael", "Ice burst or barrage",
            "Catacombs of Kourend; Slayer Tower", "None",
            "Greater nechryael in the Catacombs are the common barrage target.",
        ),
        SlayerPreset(
            "High level", "Abyssal demons", "Melee or barrage",
            "Slayer Tower; Catacombs of Kourend; Abyssal Area", "None",
            "Arclight is effective; Abyssal Sire is the boss alternative.",
        ),
        SlayerPreset(
            "High level", "Gargoyles", "Crush-focused melee",
            "Slayer Tower", "Rock hammer or Slayer unlock",
            "Grotesque Guardians are available from the rooftop entrance.",
        ),
        SlayerPreset(
            "High level", "Kurask", "Leaf-bladed weapons or broad ammunition",
            "Fremennik Slayer Dungeon; Iorwerth Dungeon",
            "Leaf-bladed weapon or broad bolts/arrows",
            "Other weapons do not damage them.",
        ),
        SlayerPreset(
            "High level", "Cave horrors", "Melee or ranged",
            "Mos Le'Harmless Cave", "Witchwood icon or Slayer helmet; light source",
            "Black masks are the signature drop.",
        ),
        SlayerPreset(
            "High level", "Basilisk Knights", "Ranged or melee",
            "Jormungand's Prison", "V's shield or mirror shield",
            "Basilisk jaw rate is improved while on a basilisk task.",
        ),
        SlayerPreset(
            "Karuulm", "Wyrms", "Ranged or melee",
            "Karuulm Slayer Dungeon", "Boots of stone or heat protection",
            "Use the agility shortcuts when unlocked.",
        ),
        SlayerPreset(
            "Karuulm", "Drakes", "Ranged or stab-focused melee",
            "Karuulm Slayer Dungeon", "Boots of stone or heat protection",
            "A dragonfire shield reduces their special attack damage.",
        ),
        SlayerPreset(
            "Karuulm", "Hydras", "Ranged or melee",
            "Lower level of Karuulm Slayer Dungeon", "Boots of stone or heat protection",
            "Alchemical Hydra is the boss alternative at 95 Slayer.",
        ),
        SlayerPreset(
            "Boss", "Abyssal Sire", "Demonbane melee; ranged for respiratory systems",
            "Abyssal Nexus", "Super restore; stamina; combat supplies",
            "Requires an abyssal demons task.",
        ),
        SlayerPreset(
            "Boss", "Cerberus", "Crush or high-damage melee",
            "Taverley Dungeon hellhound area", "Key master teleport optional",
            "Requires a hellhounds task and 91 Slayer.",
        ),
        SlayerPreset(
            "Boss", "Kraken", "Magic",
            "Kraken Cove", "Fishing explosive; magic gear",
            "Requires a cave kraken task and 87 Slayer.",
        ),
        SlayerPreset(
            "Boss", "Alchemical Hydra", "Ranged or melee",
            "Mount Karuulm", "Boots of stone or heat protection",
            "Requires a hydras task and 95 Slayer.",
        ),
        SlayerPreset(
            "Boss", "Grotesque Guardians", "Melee and ranged switches",
            "Slayer Tower rooftop", "Rock hammer; brittle key unlock",
            "Requires a gargoyles task and 75 Slayer.",
        ),
        SlayerPreset(
            "Boss", "Araxxor", "Crush-focused melee",
            "Morytania Araxyte lair", "Anti-venom and combat supplies",
            "Requires an araxytes task and 92 Slayer.",
        ),
        SlayerPreset(
            "Wilderness", "Revenants", "Ranged or melee",
            "Revenant Caves", "Bracelet of ethereum; Wilderness escape teleport",
            "DANGEROUS: other players can attack you; risk only what you can lose.",
        ),
        SlayerPreset(
            "Wilderness", "Wilderness bosses", "Boss-specific melee or ranged",
            "Wilderness boss lairs", "Looting bag; one-click escape where usable",
            "DANGEROUS: verify the combat bracket, escape route, and carried risk.",
        ),
    )

    val activities: List<ActivityPreset> = HiscoreCatalog.activityNames
        .filterNot {
            it in setOf(
                "Grid Points", "League Points", "Deadman Points",
                "Bounty Hunter - Hunter", "Bounty Hunter - Rogue",
                "Bounty Hunter (Legacy) - Hunter", "Bounty Hunter (Legacy) - Rogue",
                "LMS - Rank", "PvP Arena - Rank", "Collections Logged",
            )
        }
        .map { name ->
            ActivityPreset(
                category = when {
                    name.startsWith("Clue Scrolls") -> "Clues"
                    name in RAID_ACTIVITIES -> "Raids"
                    name in WILDERNESS_ACTIVITIES -> "Wilderness"
                    name in SKILLING_ACTIVITIES -> "Skilling"
                    else -> "Bosses"
                },
                name = name,
            )
        }

    val collections = listOf(
        CollectionPreset("Slayer", "Abyssal whip", 512),
        CollectionPreset("Slayer", "Black mask", 512),
        CollectionPreset("Slayer", "Dragon boots", 128),
        CollectionPreset("Slayer", "Kraken tentacle", 400, "Kraken"),
        CollectionPreset("Slayer", "Unsired", 100, "Abyssal Sire"),
        CollectionPreset("Slayer", "Hydra's claw", 1_000, "Alchemical Hydra"),
        CollectionPreset("Boss", "Vorkath's head", 50, "Vorkath"),
        CollectionPreset("Boss", "Skeletal visage", 5_000, "Vorkath"),
        CollectionPreset("Boss", "Kalphite Queen head", 128, "Kalphite Queen"),
        CollectionPreset("Boss", "Kbd heads", 128, "King Black Dragon"),
        CollectionPreset("Boss", "Scurrius' spine", 33, "Scurrius"),
        CollectionPreset("Boss", "Zulrah unique — specific item", 1_024, "Zulrah"),
        CollectionPreset("Boss", "Enhanced crystal weapon seed", 400, "The Corrupted Gauntlet"),
        CollectionPreset("Boss", "Bandos hilt", 508, "General Graardor"),
        CollectionPreset("Boss", "Armadyl hilt", 508, "Kree'Arra"),
        CollectionPreset("Clues", "Ranger boots", 1_133, "Clue Scrolls (medium)"),
        CollectionPreset("Clues", "Bloodhound", 1_000, "Clue Scrolls (master)"),
    )

    val routines = listOf(
        RoutinePreset("Daily", "Battlestaves from Zaff", false),
        RoutinePreset("Daily", "Managing Miscellania approval", false),
        RoutinePreset("Daily", "Farming contract", false),
        RoutinePreset("Daily", "Herb run", false),
        RoutinePreset("Daily", "Birdhouse run", false),
        RoutinePreset("Daily", "Crystal tree", false),
        RoutinePreset("Daily", "Bonemeal and slime", false),
        RoutinePreset("Daily", "Dynamite collection", false),
        RoutinePreset("Daily", "Sand from Bert", false),
        RoutinePreset("Daily", "Runes from shops", false),
        RoutinePreset("Weekly", "Tears of Guthix", true),
        RoutinePreset("Weekly", "Kingdom coffers check", true),
        RoutinePreset("Weekly", "Agility Arena ticket check", true),
    )

    val loadouts = listOf(
        LoadoutPreset(
            "Slayer", "Melee Slayer",
            "Food, prayer potions, combat potion, teleport, task-specific item",
            "Slayer helmet, melee weapon, defender/shield, melee armour, boots, gloves",
            "Adjust prayer and task protection items for the selected monster.",
        ),
        LoadoutPreset(
            "Slayer", "Burst / barrage Slayer",
            "Rune pouch, prayer potions, food, emergency teleport",
            "Slayer helmet, magic damage gear, prayer bonus gear, occult-style necklace",
            "Confirm the task area is multi-combat before stacking monsters.",
        ),
        LoadoutPreset(
            "Runs", "Herb run",
            "Seeds, ultracompost, seed dibber, spade, rake, noted payments if needed",
            "Magic secateurs; graceful or weight-reducing gear",
            "Configure teleports in route planner for every unlocked patch.",
        ),
        LoadoutPreset(
            "Runs", "Birdhouse run",
            "4 clockworks, 4 logs, 40 hop seeds, digsite pendant",
            "Lightweight gear",
            "Use the best logs available for the Hunter level.",
        ),
        LoadoutPreset(
            "Boss", "Vorkath — ranged",
            "Extended antifire, anti-venom, ranging potion, prayer, food, crumble undead runes",
            "Dragon hunter crossbow or ranged weapon, anti-dragon shield, ranged gear",
            "Keep a one-click teleport and verify ammo/bolt selection.",
        ),
        LoadoutPreset(
            "Boss", "Barrows",
            "Prayer potions, food, spade, teleport, combat switches",
            "Magic setup with melee/ranged switch as preferred",
            "Bring a method to reach the chest tunnels and restore stats afterward.",
        ),
        LoadoutPreset(
            "Minigame", "Guardians of the Rift",
            "Rune pouches, chisel, pickaxe, binding necklace if crafting combination runes",
            "Graceful or Raiments of the Eye",
            "Select pouches and cells for the player's Runecraft level.",
        ),
        LoadoutPreset(
            "Raid", "Raid base checklist",
            "Combat potions, restores, food, rune pouch, emergency teleport if applicable",
            "Melee, ranged, and magic switches",
            "This is a starting checklist; customise it for team, invocation, and raid.",
        ),
    )

}

package io.github.taxledgr.runecompanion.features

data class SkillRequirement(
    val skill: String,
    val level: Int,
)

data class BossPreset(
    val id: String,
    val name: String,
    val category: String,
    val skills: List<SkillRequirement>,
    val manualChecks: List<String>,
    val wikiTitle: String,
)

data class ItineraryPreset(
    val title: String,
    val region: String,
    val teleport: String,
    val category: String,
)

data class ProgressPreset(
    val id: String,
    val name: String,
    val category: String,
    val skills: List<SkillRequirement>,
    val prerequisites: String,
    val wikiTitle: String,
)

data class MonsterPreset(
    val name: String,
    val category: String,
    val style: String,
    val locations: String,
    val requirement: String,
    val notableDrops: String,
    val wikiTitle: String = name,
)

object ExpansionCatalog {
    val bosses = listOf(
        BossPreset(
            "barrows", "Barrows", "Mid-level", listOf(
                SkillRequirement("Magic", 50),
                SkillRequirement("Prayer", 43),
            ),
            listOf("Spade", "Prayer restoration", "Emergency teleport", "Barrows loadout"),
            "Barrows",
        ),
        BossPreset(
            "giant_mole", "Giant Mole", "Mid-level", listOf(
                SkillRequirement("Attack", 60),
                SkillRequirement("Strength", 60),
                SkillRequirement("Defence", 60),
            ),
            listOf("Light source", "Falador shield recommended", "Stamina supplies", "Melee loadout"),
            "Giant Mole",
        ),
        BossPreset(
            "dagannoth_kings", "Dagannoth Kings", "Mid-level", listOf(
                SkillRequirement("Ranged", 70),
                SkillRequirement("Magic", 70),
                SkillRequirement("Prayer", 43),
            ),
            listOf("Pet rock / thrownaxe access", "Antipoison", "Prayer restoration", "Hybrid loadout"),
            "Dagannoth Kings",
        ),
        BossPreset(
            "jad", "TzTok-Jad", "Challenges", listOf(
                SkillRequirement("Ranged", 70),
                SkillRequirement("Prayer", 43),
                SkillRequirement("Hitpoints", 70),
            ),
            listOf("Fight Cave supplies", "Ranged loadout", "Prayer plan", "Two uninterrupted hours"),
            "TzTok-Jad",
        ),
        BossPreset(
            "zulrah", "Zulrah", "Quest bosses", listOf(
                SkillRequirement("Ranged", 75),
                SkillRequirement("Magic", 75),
                SkillRequirement("Defence", 70),
            ),
            listOf("Regicide access", "Antivenom", "Ring of recoil", "Zul-andra teleport", "Hybrid loadout"),
            "Zulrah",
        ),
        BossPreset(
            "vorkath", "Vorkath", "Quest bosses", listOf(
                SkillRequirement("Ranged", 80),
                SkillRequirement("Defence", 70),
                SkillRequirement("Prayer", 74),
            ),
            listOf("Dragon Slayer II", "Antifire protection", "Anti-venom", "Crumble Undead runes", "Vorkath loadout"),
            "Vorkath",
        ),
        BossPreset(
            "muspah", "Phantom Muspah", "Quest bosses", listOf(
                SkillRequirement("Ranged", 80),
                SkillRequirement("Magic", 70),
                SkillRequirement("Prayer", 70),
            ),
            listOf("Secrets of the North", "Stamina supplies", "Prayer restoration", "Muspah loadout"),
            "Phantom Muspah",
        ),
        BossPreset(
            "gauntlet", "The Gauntlet", "Challenges", listOf(
                SkillRequirement("Attack", 80),
                SkillRequirement("Strength", 80),
                SkillRequirement("Defence", 80),
                SkillRequirement("Ranged", 80),
                SkillRequirement("Magic", 80),
            ),
            listOf("Song of the Elves", "Preparation route understood", "Hunllef mechanics reviewed"),
            "The Gauntlet",
        ),
        BossPreset(
            "toa", "Tombs of Amascut", "Raids", listOf(
                SkillRequirement("Attack", 75),
                SkillRequirement("Strength", 75),
                SkillRequirement("Ranged", 75),
                SkillRequirement("Magic", 75),
                SkillRequirement("Prayer", 70),
            ),
            listOf("Beneath Cursed Sands", "Invocation plan", "Raid supplies", "Three-style loadout"),
            "Tombs of Amascut",
        ),
        BossPreset(
            "cox", "Chambers of Xeric", "Raids", listOf(
                SkillRequirement("Attack", 80),
                SkillRequirement("Strength", 80),
                SkillRequirement("Ranged", 80),
                SkillRequirement("Magic", 80),
                SkillRequirement("Herblore", 78),
            ),
            listOf("Raid layout/scouting plan", "Stamina supplies", "Three-style loadout", "Olm roles"),
            "Chambers of Xeric",
        ),
        BossPreset(
            "tob", "Theatre of Blood", "Raids", listOf(
                SkillRequirement("Attack", 90),
                SkillRequirement("Strength", 90),
                SkillRequirement("Ranged", 90),
                SkillRequirement("Magic", 90),
                SkillRequirement("Prayer", 77),
            ),
            listOf("A Night at the Theatre access", "Team and role agreed", "Raid supplies", "ToB loadout"),
            "Theatre of Blood",
        ),
        BossPreset(
            "nex", "Nex", "Group bosses", listOf(
                SkillRequirement("Ranged", 90),
                SkillRequirement("Defence", 80),
                SkillRequirement("Hitpoints", 90),
                SkillRequirement("Prayer", 74),
            ),
            listOf("Frozen Door access", "Ancient ceremonial or bank access", "Team size selected", "Nex loadout"),
            "Nex",
        ),
    )

    val itinerary = listOf(
        ItineraryPreset("Birdhouse run", "Fossil Island", "Digsite pendant / Mushroom teleport", "Runs"),
        ItineraryPreset("Catherby herb patch", "Kandarin", "Camelot teleport", "Farming"),
        ItineraryPreset("Ardougne herb patch", "Kandarin", "Ardougne cloak / teleport", "Farming"),
        ItineraryPreset("Falador herb patch", "Asgarnia", "Explorer's ring / Falador teleport", "Farming"),
        ItineraryPreset("Morytania herb patch", "Morytania", "Ectophial", "Farming"),
        ItineraryPreset("Hosidius herb patch", "Kourend", "Xeric's talisman", "Farming"),
        ItineraryPreset("Farming Guild", "Kebos", "Skills necklace", "Farming"),
        ItineraryPreset("Weiss herb patch", "Fremennik", "Icy basalt", "Farming"),
        ItineraryPreset("Troll Stronghold herb patch", "Fremennik", "Stony basalt", "Farming"),
        ItineraryPreset("Harmony Island herb patch", "Morytania", "Harmony Island tablet", "Farming"),
        ItineraryPreset("Kingdom approval", "Fremennik", "Ring of wealth", "Dailies"),
        ItineraryPreset("Battlestaves", "Asgarnia", "Staff shop / POH route", "Dailies"),
        ItineraryPreset("Master clue Sherlock", "Kandarin", "Sherlock teleport", "Clues"),
        ItineraryPreset("Grand Exchange restock", "Misthalin", "Ring of wealth / Varrock teleport", "Supplies"),
    )

    val progress = listOf(
        ProgressPreset("waterfall", "Waterfall Quest", "Quest", emptyList(), "None", "Waterfall Quest"),
        ProgressPreset("tree_gnome", "Tree Gnome Village", "Quest", emptyList(), "None", "Tree Gnome Village"),
        ProgressPreset("grand_tree", "The Grand Tree", "Quest", listOf(SkillRequirement("Agility", 25)), "Tree Gnome Village recommended", "The Grand Tree"),
        ProgressPreset("fairytale_ii", "Fairytale II - Cure a Queen", "Quest", listOf(SkillRequirement("Farming", 49), SkillRequirement("Herblore", 57), SkillRequirement("Thieving", 40)), "Fairytale I", "Fairytale II - Cure a Queen"),
        ProgressPreset("desert_treasure", "Desert Treasure I", "Quest", listOf(SkillRequirement("Magic", 50), SkillRequirement("Thieving", 53)), "Multiple quest prerequisites", "Desert Treasure I"),
        ProgressPreset("lunar_diplomacy", "Lunar Diplomacy", "Quest", listOf(SkillRequirement("Magic", 65), SkillRequirement("Mining", 60), SkillRequirement("Crafting", 61)), "The Fremennik Trials and related quests", "Lunar Diplomacy"),
        ProgressPreset("recipe_disaster", "Recipe for Disaster", "Quest", listOf(SkillRequirement("Cooking", 70)), "Long multi-quest chain", "Recipe for Disaster"),
        ProgressPreset("mm2", "Monkey Madness II", "Quest", listOf(SkillRequirement("Slayer", 69), SkillRequirement("Agility", 55), SkillRequirement("Thieving", 55)), "Monkey Madness I and related quests", "Monkey Madness II"),
        ProgressPreset("ds2", "Dragon Slayer II", "Quest", listOf(SkillRequirement("Magic", 75), SkillRequirement("Smithing", 70), SkillRequirement("Mining", 68), SkillRequirement("Crafting", 62)), "200 Quest points and quest chain", "Dragon Slayer II"),
        ProgressPreset("sote", "Song of the Elves", "Quest", listOf(SkillRequirement("Agility", 70), SkillRequirement("Construction", 70), SkillRequirement("Farming", 70), SkillRequirement("Herblore", 70), SkillRequirement("Hunter", 70), SkillRequirement("Mining", 70), SkillRequirement("Smithing", 70), SkillRequirement("Woodcutting", 70)), "Mourning's End quest chain", "Song of the Elves"),
        ProgressPreset("sins_father", "Sins of the Father", "Quest", listOf(SkillRequirement("Woodcutting", 62), SkillRequirement("Fletching", 60), SkillRequirement("Crafting", 56), SkillRequirement("Agility", 52)), "Myreque quest chain", "Sins of the Father"),
        ProgressPreset("beneath_sands", "Beneath Cursed Sands", "Quest", listOf(SkillRequirement("Agility", 62), SkillRequirement("Crafting", 55), SkillRequirement("Firemaking", 55)), "Contact! and related quests", "Beneath Cursed Sands"),
        ProgressPreset("secrets_north", "Secrets of the North", "Quest", listOf(SkillRequirement("Agility", 69), SkillRequirement("Thieving", 64), SkillRequirement("Hunter", 56)), "Mahjarrat quest chain", "Secrets of the North"),
        ProgressPreset("fremennik_easy", "Fremennik Easy Diary", "Achievement Diary", emptyList(), "Regional tasks", "Fremennik Diary"),
        ProgressPreset("lumbridge_medium", "Lumbridge & Draynor Medium Diary", "Achievement Diary", emptyList(), "Regional tasks", "Lumbridge & Draynor Diary"),
        ProgressPreset("ardougne_hard", "Ardougne Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Ardougne Diary"),
        ProgressPreset("desert_hard", "Desert Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Desert Diary"),
        ProgressPreset("falador_hard", "Falador Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Falador Diary"),
        ProgressPreset("kandarin_hard", "Kandarin Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Kandarin Diary"),
        ProgressPreset("kourend_hard", "Kourend & Kebos Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Kourend & Kebos Diary"),
        ProgressPreset("morytania_hard", "Morytania Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Morytania Diary"),
        ProgressPreset("varrock_hard", "Varrock Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Varrock Diary"),
        ProgressPreset("western_hard", "Western Provinces Hard Diary", "Achievement Diary", emptyList(), "Regional tasks and quests", "Western Provinces Diary"),
        ProgressPreset("wilderness_hard", "Wilderness Hard Diary", "Achievement Diary", emptyList(), "Dangerous regional tasks", "Wilderness Diary"),
    )

    val monsters = listOf(
        MonsterPreset("Abyssal demon", "Slayer", "Melee / demonbane", "Slayer Tower; Catacombs; Abyssal Area", "85 Slayer", "Abyssal whip; abyssal dagger", "Abyssal demon"),
        MonsterPreset("Alchemical Hydra", "Slayer bosses", "Ranged", "Karuulm Slayer Dungeon", "95 Slayer and hydras task", "Hydra leather; dragon hunter lance components", "Alchemical Hydra"),
        MonsterPreset("Araxxor", "Slayer bosses", "Melee", "Morytania", "92 Slayer and araxytes task", "Noxious components; rancour component", "Araxxor"),
        MonsterPreset("Cerberus", "Slayer bosses", "Melee", "Taverley Dungeon", "91 Slayer and hellhounds task", "Primordial, pegasian and eternal crystals", "Cerberus"),
        MonsterPreset("Kraken", "Slayer bosses", "Magic", "Kraken Cove", "87 Slayer and cave kraken task", "Trident of the seas; tentacle", "Kraken"),
        MonsterPreset("Gargoyle", "Slayer", "Melee", "Slayer Tower", "75 Slayer; rock hammer", "Granite maul; mystic robe top", "Gargoyle"),
        MonsterPreset("Kurask", "Slayer", "Leaf-bladed weapon", "Fremennik Slayer Dungeon; Iorwerth Dungeon", "70 Slayer", "Leaf-bladed battleaxe; mystic robe top", "Kurask"),
        MonsterPreset("Nechryael", "Slayer", "Melee / magic", "Slayer Tower; Catacombs", "80 Slayer", "Rune boots; herb and rune drops", "Nechryael"),
        MonsterPreset("Dust devil", "Slayer", "Magic", "Smoke Dungeon; Catacombs", "65 Slayer; face protection", "Dragon chainbody; dust battlestaff", "Dust devil"),
        MonsterPreset("Smoke devil", "Slayer", "Magic", "Smoke Devil Dungeon", "93 Slayer and task", "Occult necklace; smoke battlestaff", "Smoke devil"),
        MonsterPreset("Vorkath", "Bosses", "Ranged / melee", "Ungael", "Dragon Slayer II", "Superior dragon bones; draconic visage; uniques"),
        MonsterPreset("Zulrah", "Bosses", "Ranged and Magic", "Zul-Andra", "Regicide", "Zulrah scales; blowpipe; trident; visage"),
        MonsterPreset("Phantom Muspah", "Bosses", "Ranged / Magic", "Ghorrock Dungeon", "Secrets of the North", "Ancient essence; venator shard"),
        MonsterPreset("Giant Mole", "Bosses", "Melee", "Falador Mole Lair", "Spade and light source", "Mole skin; mole claw"),
        MonsterPreset("Kree'arra", "God Wars", "Ranged", "Armadyl's Eyrie", "70 Ranged; rope; Armadyl protection", "Armadyl armour; hilt"),
        MonsterPreset("K'ril Tsutsaroth", "God Wars", "Melee / Ranged", "Zamorak's Fortress", "70 Hitpoints; Zamorak protection", "Zamorakian spear; hilt"),
        MonsterPreset("General Graardor", "God Wars", "Melee / Ranged", "Bandos' Stronghold", "70 Strength; Bandos protection", "Bandos armour; hilt"),
        MonsterPreset("Commander Zilyana", "God Wars", "Ranged", "Saradomin's Encampment", "70 Agility; rope; Saradomin protection", "Armadyl crossbow; Saradomin sword; hilt"),
        MonsterPreset("Callisto", "Wilderness bosses", "Ranged / Magic", "Callisto's Den", "Wilderness risk", "Tyrannical ring; voidwaker component"),
        MonsterPreset("Venenatis", "Wilderness bosses", "Ranged", "Silk Chasm", "Wilderness risk", "Treasonous ring; voidwaker component"),
        MonsterPreset("Vet'ion", "Wilderness bosses", "Crush", "Skeletal Tomb", "Wilderness risk", "Ring of the gods; voidwaker component"),
        MonsterPreset("The Leviathan", "Desert Treasure II", "Ranged", "The Scar", "Desert Treasure II", "Venator vestige; axe piece"),
        MonsterPreset("The Whisperer", "Desert Treasure II", "Magic", "Lassar Undercity", "Desert Treasure II", "Bellator vestige; axe piece"),
        MonsterPreset("Vardorvis", "Desert Treasure II", "Melee", "Stranglewood", "Desert Treasure II", "Ultor vestige; axe piece"),
        MonsterPreset("Duke Sucellus", "Desert Treasure II", "Melee", "Ghorrock Prison", "Desert Treasure II", "Magus vestige; axe piece"),
    )

    fun guessRegion(text: String): String {
        val value = text.lowercase()
        return when {
            listOf("catherby", "ardougne", "camelot", "kandarin").any(value::contains) -> "Kandarin"
            listOf("falador", "taverley", "asgarnia").any(value::contains) -> "Asgarnia"
            listOf("morytania", "canifis", "mort", "harmony").any(value::contains) -> "Morytania"
            listOf("hosidius", "kourend", "farming guild", "kebos").any(value::contains) -> "Kourend & Kebos"
            listOf("weiss", "troll", "fremennik").any(value::contains) -> "Fremennik"
            listOf("varrock", "lumbridge", "misthalin", "grand exchange").any(value::contains) -> "Misthalin"
            listOf("fossil", "birdhouse").any(value::contains) -> "Fossil Island"
            listOf("wilderness", "revenant").any(value::contains) -> "Wilderness"
            else -> "Other"
        }
    }

    fun suggestedTeleport(region: String): String = when (region) {
        "Kandarin" -> "Camelot / Ardougne teleport"
        "Asgarnia" -> "Falador teleport"
        "Morytania" -> "Ectophial / Morytania legs"
        "Kourend & Kebos" -> "Xeric's talisman / Skills necklace"
        "Fremennik" -> "Basalt / Fremennik teleport"
        "Misthalin" -> "Varrock / Lumbridge teleport"
        "Fossil Island" -> "Digsite pendant"
        "Wilderness" -> "Review risk before travelling"
        else -> "Choose a configured teleport"
    }
}

package io.github.taxledgr.runecompanion.features

object TeleportCatalog {
    val spellbooks = listOf(
        t("home_lumbridge", "Lumbridge Home Teleport", "Lumbridge Castle", "Misthalin", TeleportKind.HOME, cap = "home_lumbridge", note = "30-minute cooldown"),
        t("home_edgeville", "Edgeville Home Teleport", "Edgeville", "Misthalin", TeleportKind.HOME, book = Spellbook.ANCIENT),
        t("home_lunar", "Lunar Home Teleport", "Lunar Isle", "Fremennik", TeleportKind.HOME, book = Spellbook.LUNAR),
        t("home_arceuus", "Arceuus Home Teleport", "Dark Altar", "Kourend", TeleportKind.HOME, book = Spellbook.ARCEUUS),

        t("spell_varrock", "Varrock Teleport", "Varrock", "Misthalin", TeleportKind.SPELL, 25, Spellbook.STANDARD),
        t("spell_lumbridge", "Lumbridge Teleport", "Lumbridge", "Misthalin", TeleportKind.SPELL, 31, Spellbook.STANDARD),
        t("spell_falador", "Falador Teleport", "Falador", "Asgarnia", TeleportKind.SPELL, 37, Spellbook.STANDARD),
        t("spell_poh", "Teleport to House", "Player-owned house", "POH", TeleportKind.SPELL, 40, Spellbook.STANDARD, "poh_access"),
        t("spell_camelot", "Camelot Teleport", "Camelot / Seers' Village", "Kandarin", TeleportKind.SPELL, 45, Spellbook.STANDARD),
        t("spell_ardougne", "Ardougne Teleport", "East Ardougne", "Kandarin", TeleportKind.SPELL, 51, Spellbook.STANDARD, "quest_plague_city"),
        t("spell_watchtower", "Watchtower Teleport", "Watchtower / Yanille", "Kandarin", TeleportKind.SPELL, 58, Spellbook.STANDARD, "quest_watchtower"),
        t("spell_trollheim", "Trollheim Teleport", "Trollheim", "Troll Country", TeleportKind.SPELL, 61, Spellbook.STANDARD, "quest_eadgars_ruse"),
        t("spell_ape_atoll", "Ape Atoll Teleport", "Ape Atoll", "Karamja", TeleportKind.SPELL, 64, Spellbook.STANDARD, "quest_ape_atoll"),
        t("spell_kourend", "Kourend Castle Teleport", "Kourend Castle", "Kourend", TeleportKind.SPELL, 69, Spellbook.STANDARD, "quest_client_kourend"),
        t("spell_civitas", "Civitas illa Fortis Teleport", "Civitas illa Fortis", "Varlamore", TeleportKind.SPELL, 54, Spellbook.STANDARD, "quest_children_sun"),
        t("spell_boat", "Teleport to Boat", "Owned boat mooring point", "Sailing", TeleportKind.SPELL, 67, Spellbook.STANDARD, "quest_pandemonium"),

        t("ancient_paddewwa", "Paddewwa Teleport", "Edgeville Dungeon", "Misthalin", TeleportKind.SPELL, 54, Spellbook.ANCIENT, "quest_desert_treasure"),
        t("ancient_senntisten", "Senntisten Teleport", "Digsite", "Misthalin", TeleportKind.SPELL, 60, Spellbook.ANCIENT, "quest_desert_treasure"),
        t("ancient_kharyrll", "Kharyrll Teleport", "Canifis", "Morytania", TeleportKind.SPELL, 66, Spellbook.ANCIENT, "quest_desert_treasure"),
        t("ancient_lassar", "Lassar Teleport", "Ice Mountain", "Asgarnia", TeleportKind.SPELL, 72, Spellbook.ANCIENT, "quest_desert_treasure"),
        t("ancient_dareeyak", "Dareeyak Teleport", "Western Wilderness", "Wilderness", TeleportKind.SPELL, 78, Spellbook.ANCIENT, "quest_desert_treasure", true),
        t("ancient_carrallanger", "Carrallanger Teleport", "Graveyard of Shadows", "Wilderness", TeleportKind.SPELL, 84, Spellbook.ANCIENT, "quest_desert_treasure", true),
        t("ancient_annakarl", "Annakarl Teleport", "Demonic Ruins", "Wilderness", TeleportKind.SPELL, 90, Spellbook.ANCIENT, "quest_desert_treasure", true),
        t("ancient_ghorrock", "Ghorrock Teleport", "Frozen Waste Plateau", "Wilderness", TeleportKind.SPELL, 96, Spellbook.ANCIENT, "quest_desert_treasure", true),

        t("lunar_moonclan", "Moonclan Teleport", "Moonclan", "Fremennik", TeleportKind.SPELL, 69, Spellbook.LUNAR, "quest_lunar_diplomacy"),
        t("lunar_ourania", "Ourania Teleport", "Ourania Altar", "Kandarin", TeleportKind.SPELL, 71, Spellbook.LUNAR, "quest_lunar_diplomacy"),
        t("lunar_waterbirth", "Waterbirth Teleport", "Waterbirth Island", "Fremennik", TeleportKind.SPELL, 72, Spellbook.LUNAR, "quest_lunar_diplomacy"),
        t("lunar_barbarian", "Barbarian Teleport", "Barbarian Outpost", "Kandarin", TeleportKind.SPELL, 75, Spellbook.LUNAR, "quest_lunar_diplomacy"),
        t("lunar_khazard", "Khazard Teleport", "Port Khazard", "Kandarin", TeleportKind.SPELL, 78, Spellbook.LUNAR, "quest_lunar_diplomacy"),
        t("lunar_fishing", "Fishing Guild Teleport", "Fishing Guild", "Kandarin", TeleportKind.SPELL, 85, Spellbook.LUNAR, "quest_lunar_diplomacy"),
        t("lunar_catherby", "Catherby Teleport", "Catherby", "Kandarin", TeleportKind.SPELL, 87, Spellbook.LUNAR, "quest_lunar_diplomacy"),
        t("lunar_ice_plateau", "Ice Plateau Teleport", "Ice Plateau", "Wilderness", TeleportKind.SPELL, 89, Spellbook.LUNAR, "quest_lunar_diplomacy", true),

        t("arceuus_library", "Arceuus Library Teleport", "Arceuus Library", "Kourend", TeleportKind.SPELL, 6, Spellbook.ARCEUUS),
        t("arceuus_draynor", "Draynor Manor Teleport", "Draynor Manor", "Misthalin", TeleportKind.SPELL, 17, Spellbook.ARCEUUS),
        t("arceuus_battlefront", "Battlefront Teleport", "Battlefront", "Kourend", TeleportKind.SPELL, 23, Spellbook.ARCEUUS),
        t("arceuus_mind", "Mind Altar Teleport", "Mind Altar", "Asgarnia", TeleportKind.SPELL, 28, Spellbook.ARCEUUS),
        t("arceuus_respawn", "Respawn Teleport", "Respawn point", "Varies", TeleportKind.SPELL, 34, Spellbook.ARCEUUS),
        t("arceuus_salve", "Salve Graveyard Teleport", "Salve Graveyard", "Morytania", TeleportKind.SPELL, 40, Spellbook.ARCEUUS, "quest_priest_peril"),
        t("arceuus_fenkenstrain", "Fenkenstrain's Castle Teleport", "Fenkenstrain's Castle", "Morytania", TeleportKind.SPELL, 48, Spellbook.ARCEUUS, "quest_creature_fenkenstrain"),
        t("arceuus_west_ardougne", "West Ardougne Teleport", "West Ardougne", "Kandarin", TeleportKind.SPELL, 61, Spellbook.ARCEUUS, "quest_biohazard"),
        t("arceuus_harmony", "Harmony Island Teleport", "Harmony Island", "Morytania", TeleportKind.SPELL, 65, Spellbook.ARCEUUS, "quest_great_brain_robbery"),
        t("arceuus_cemetery", "Cemetery Teleport", "Forgotten Cemetery", "Wilderness", TeleportKind.SPELL, 71, Spellbook.ARCEUUS, dangerous = true),
        t("arceuus_barrows", "Barrows Teleport", "Barrows", "Morytania", TeleportKind.SPELL, 83, Spellbook.ARCEUUS, "quest_priest_peril"),
        t("arceuus_ape", "Ape Atoll Dungeon Teleport", "Ape Atoll Dungeon", "Karamja", TeleportKind.SPELL, 90, Spellbook.ARCEUUS, "quest_monkey_madness_ii"),
    )

    val tablets = listOf(
        "Varrock" to "Misthalin", "Lumbridge" to "Misthalin", "Falador" to "Asgarnia",
        "Camelot" to "Kandarin", "Ardougne" to "Kandarin", "Watchtower" to "Kandarin",
        "Rimmington" to "Asgarnia", "Taverley" to "Asgarnia", "Pollnivneach" to "Desert",
        "Rellekka" to "Fremennik", "Brimhaven" to "Karamja", "Yanille" to "Kandarin",
        "Kourend Castle" to "Kourend", "Civitas illa Fortis" to "Varlamore",
        "Barrows" to "Morytania", "Salve Graveyard" to "Morytania",
        "Fenkenstrain's Castle" to "Morytania", "West Ardougne" to "Kandarin",
        "Harmony Island" to "Morytania", "Cemetery" to "Wilderness",
        "Boat" to "Owned boat mooring point", "House" to "Player-owned house",
        "Volcanic Mine" to "Fossil Island", "Wilderness crabs" to "Wilderness",
    ).map { (name, region) ->
        val key = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        t("tab_$key", "$name teleport tablet", name, region, TeleportKind.TABLET, cap = "tab_$key", dangerous = region == "Wilderness")
    }

    val jewellery = listOf(
        group("jew_games", "Games necklace", "Burthorpe / Barbarian Outpost / Corporeal Beast / Tears of Guthix / Wintertodt / Guardians of the Rift", "Multiple"),
        group("jew_dueling", "Ring of dueling", "Emir's Arena / Castle Wars / Ferox Enclave", "Multiple"),
        group("jew_passage", "Necklace of passage", "Wizards' Tower / Outpost / Eagle's Eyrie", "Multiple"),
        group("jew_burning", "Burning amulet", "Chaos Temple / Bandit Camp / Lava Maze", "Wilderness", true),
        group("jew_combat", "Combat bracelet", "Warriors' Guild / Champions' Guild / Monastery / Ranging Guild", "Multiple"),
        group("jew_skills", "Skills necklace", "Fishing / Mining / Crafting / Cooking / Woodcutting / Farming Guilds", "Multiple"),
        group("jew_wealth", "Ring of wealth", "Grand Exchange / Falador Park / Miscellania / Dondakan", "Multiple"),
        group("jew_glory", "Amulet of glory", "Edgeville / Karamja / Draynor / Al Kharid", "Multiple"),
        group("jew_slayer", "Slayer ring", "Slayer Tower / Fremennik Slayer Dungeon / Tarn's Lair / Stronghold Slayer Cave / Dark Beasts", "Multiple"),
        group("jew_digsite", "Digsite pendant", "Digsite / Fossil Island / Lithkren", "Multiple"),
        group("jew_camulet", "Camulet", "Enakhra's Temple / inside temple", "Desert"),
        group("jew_desert", "Desert amulet", "Nardah / Kalphite Cave", "Desert"),
        group("jew_elements", "Ring of the elements", "Air / Water / Earth / Fire altars", "Multiple"),
        group("jew_shadows", "Ring of shadows", "DT2 vestige locations", "Multiple"),
        group("jew_ates", "Pendant of ates", "Aldarin / Darkfrost / Ralos' Rise / Sunset Coast / Tlati", "Varlamore"),
        group("jew_eye", "Eye amulet", "Temple of the Eye", "Misthalin"),
        group("jew_sailor", "Sailors' amulet", "Pandemonium and boat destinations", "Sailing"),
    )

    val specialItems = listOf(
        group("item_ectophial", "Ectophial", "Ectofuntus", "Morytania"),
        group("item_seed_pod", "Royal seed pod", "Grand Tree", "Kandarin"),
        group("item_xeric", "Xeric's talisman", "Xeric's Look-out / Glade / Inferno / Heart / Honour", "Kourend"),
        group("item_rada", "Rada's blessing", "Kourend Woodland / Mount Karuulm", "Kourend"),
        group("item_drakan", "Drakan's medallion", "Ver Sinhaza / Darkmeyer / Meiyerditch / Slepe", "Morytania"),
        group("item_lyre", "Enchanted lyre", "Rellekka / Waterbirth / Neitiznot / Jatizso", "Fremennik"),
        group("item_crystal", "Teleport crystal", "Lletya / Prifddinas", "Tirannwn"),
        group("item_pharaoh", "Pharaoh's sceptre", "Jalsavrah / Jaleustrophos / Jaldraocht / Jaltevas", "Desert"),
        group("item_quetzal", "Quetzal whistle", "Fortis / Hunter Guild / Outer Fortis", "Varlamore"),
        group("item_skull", "Skull sceptre", "Stronghold of Security", "Misthalin"),
        group("item_chronicle", "Chronicle", "Champions' Guild", "Misthalin"),
        group("item_explorer", "Explorer's ring", "Falador cabbage patch", "Asgarnia"),
        group("item_mythical", "Mythical cape", "Myths' Guild", "Kandarin"),
        group("item_construction", "Construction cape", "POH portals", "Multiple"),
        group("item_crafting", "Crafting cape", "Crafting Guild", "Asgarnia"),
        group("item_farming", "Farming cape", "Farming Guild", "Kourend"),
        group("item_music", "Music cape", "Falo the Bard", "Kandarin"),
        group("item_achievement", "Achievement diary cape", "Diary masters", "Multiple"),
        group("item_max", "Max cape", "Max Guild teleports", "Multiple"),
        group("item_fishing", "Fishing cape", "Fishing Guild / Otto's Grotto", "Kandarin"),
        group("item_hunter", "Hunter cape", "Chinchompa areas / Hunter Guild", "Multiple"),
        group("item_quest_cape", "Quest point cape", "Legends' Guild", "Kandarin"),
        group("item_minigame", "Minigame teleport", "Unlocked minigames", "Multiple"),
        group("item_grand_seed", "Grand seed pod", "Grand Tree", "Kandarin"),
        group("item_dorgesh_sphere", "Dorgesh-kaan sphere", "Dorgesh-Kaan", "Misthalin"),
        group("item_goblin_sphere", "Goblin village sphere", "Goblin Village", "Asgarnia"),
        group("item_mud_sphere", "Plain of mud sphere", "Goblin Cave", "Kandarin"),
        group("item_blue_rum", "Blue rum", "Trouble Brewing", "Mos Le'Harmless"),
        group("item_red_rum", "Red rum", "Trouble Brewing", "Mos Le'Harmless"),
        group("item_karamja_gloves", "Karamja gloves 3/4", "Shilo gem mine / Duradel", "Karamja"),
        group("item_kandarin_headgear", "Kandarin headgear 3/4", "Sherlock", "Kandarin"),
        group("item_morytania_legs", "Morytania legs", "Ectofuntus / Burgh de Rott / Darkmeyer", "Morytania"),
        group("item_sea_boots", "Fremennik sea boots", "Rellekka / Jatizso / Neitiznot", "Fremennik"),
        group("item_ghommals", "Ghommal's hilt / avernic defender", "God Wars Dungeon / Mor Ul Rek", "Multiple"),
        group("item_fairy_ring", "Fairy ring access", "Fairy ring network", "Multiple"),
        group("item_spirit_tree", "Spirit tree access", "Spirit tree network", "Multiple"),
        group("item_ardougne_cloak", "Ardougne cloak", "Ardougne Monastery / farm", "Kandarin"),
        group("item_western_banner", "Western banner 3/4", "Piscatoris", "Kandarin"),
        group("item_kharedst", "Kharedst's memoirs / Book of the dead", "Five Kourend memoir destinations", "Kourend"),
        group("item_hallowed", "Hallowed crystal shard", "Darkmeyer", "Morytania"),
        group("item_calcified", "Calcified moth", "Cam Torum", "Varlamore"),
        group("item_magic_whistle", "Magic whistle", "Fisher King's Realm", "Kandarin"),
        group("item_volcanic", "Volcanic Mine teleport", "Volcanic Mine", "Fossil Island"),
    )

    val travelNetworks = listOf(
        group("travel_charter", "Charter ships", "Charter port network", "Multiple"),
        group("travel_balloon", "Balloon transport system", "Balloon network", "Multiple"),
        group("travel_glider", "Gnome glider network", "Gnome glider destinations", "Multiple"),
        group("travel_minecart", "Lovakengj minecart network", "Kourend minecart destinations", "Kourend"),
        group("travel_shilo_cart", "Shilo Village cart", "Brimhaven ↔ Shilo Village", "Karamja"),
        group("travel_mycelium", "Mycelium transport system", "Fossil Island mushroom network", "Fossil Island"),
        group("travel_quetzal", "Quetzal transport system", "Varlamore quetzal network", "Varlamore"),
        group("travel_abyss", "Abyss access", "Runecrafting altar network", "Multiple"),
        group("travel_lever", "Deserted Keep lever route", "Ardougne / Edgeville lever to deep Wilderness", "Wilderness", true),
        group("travel_carpet", "Magic carpet network", "Kharidian Desert carpet routes", "Desert"),
        group("travel_fairy", "Fairy ring network", "Fairy ring destinations", "Multiple"),
        group("travel_spirit", "Spirit tree network", "Spirit tree destinations", "Multiple"),
    )

    val teleportScrolls = listOf(
        "Nardah" to "Desert",
        "Digsite" to "Misthalin",
        "Feldip Hills" to "Feldip Hills",
        "Lunar Isle" to "Fremennik",
        "Mort'ton" to "Morytania",
        "Pest Control" to "Void Knights' Outpost",
        "Piscatoris" to "Kandarin",
        "Tai Bwo Wannai" to "Karamja",
        "Iorwerth Camp" to "Tirannwn",
        "Mos Le'Harmless" to "Mos Le'Harmless",
        "Lumberyard" to "Misthalin",
        "Zul-Andra" to "Tirannwn",
        "Key master" to "Cerberus' Lair",
        "Revenant cave" to "Wilderness",
        "Watson" to "Kourend",
        "Guthixian temple" to "Kandarin",
        "Spider cave" to "Morytania",
        "Colossal wyrm" to "Varlamore",
        "Chasm of Fire" to "Kourend",
    ).map { (name, region) ->
        val key = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        group(
            "scroll_$key",
            "$name teleport scroll",
            name,
            region,
            dangerous = region == "Wilderness",
        )
    }

    val pohFacilities = listOf(
        group("poh_access", "House access", "Configured house portal", "POH", kind = TeleportKind.POH),
        group("poh_glory", "Mounted amulet of glory", "Edgeville / Karamja / Draynor / Al Kharid", "Multiple", kind = TeleportKind.POH),
        group("poh_jewellery", "Ornate jewellery box", "Games / dueling / combat / skills / wealth / glory destinations", "Multiple", kind = TeleportKind.POH),
        group("poh_xeric", "Mounted Xeric's talisman", "Xeric destinations", "Kourend", kind = TeleportKind.POH),
        group("poh_digsite", "Mounted digsite pendant", "Digsite / Fossil Island / Lithkren", "Multiple", kind = TeleportKind.POH),
        group("poh_spirit_tree", "Spirit tree", "Spirit tree network", "Multiple", kind = TeleportKind.POH),
        group("poh_fairy_ring", "Fairy ring", "Fairy ring network", "Multiple", kind = TeleportKind.POH),
        group("poh_obelisk", "Obelisk", "Wilderness obelisk network", "Wilderness", true, TeleportKind.POH),
    )

    val nexusDestinations: List<TeleportOption> = spellbooks
        .filter { it.kind == TeleportKind.SPELL && it.id != "spell_poh" }
        .map {
            it.copy(
                id = "poh_nexus_${it.id}",
                name = "Nexus: ${it.destination}",
                kind = TeleportKind.POH,
                spellbook = null,
                capability = null,
                note = "House teleport → Portal Nexus",
            )
        }
        .plus(
            listOf(
                t("poh_nexus_troll_stronghold", "Nexus: Troll Stronghold", "Troll Stronghold", "Troll Country", TeleportKind.POH, 66, cap = null),
                t("poh_nexus_weiss", "Nexus: Weiss", "Weiss", "Troll Country", TeleportKind.POH, 66, cap = null),
            ),
        )

    val all: List<TeleportOption> =
        spellbooks + tablets + jewellery + specialItems + travelNetworks + teleportScrolls +
            pohFacilities + nexusDestinations

    val capabilityGroups: List<Pair<String, List<TeleportOption>>> = listOf(
        "Player & unlocks" to spellbooks.filter { it.capability != null },
        "Teleport tablets" to tablets,
        "Jewellery" to jewellery,
        "Special items & capes" to specialItems,
        "Travel networks" to travelNetworks,
        "Teleport scrolls" to teleportScrolls,
        "POH facilities" to pohFacilities,
    )

    val regions: List<String> = all.map { it.region }.distinct().sorted()

    private fun t(
        id: String,
        name: String,
        destination: String,
        region: String,
        kind: TeleportKind,
        magic: Int = 1,
        book: Spellbook? = null,
        cap: String? = null,
        dangerous: Boolean = false,
        note: String = "",
    ) = TeleportOption(id, name, destination, region, kind, magic, book, cap, dangerous, note)

    private fun group(
        id: String,
        name: String,
        destination: String,
        region: String,
        dangerous: Boolean = false,
        kind: TeleportKind = if (id.startsWith("jew_")) TeleportKind.JEWELLERY else TeleportKind.ITEM,
    ) = t(id, name, destination, region, kind, cap = id, dangerous = dangerous)
}

package io.github.taxledgr.runecompanion.features

/**
 * Route order follows the current OSRS Wiki "Shooting Stars" fastest-route
 * tables, with Star Miners' feed names used as keys.
 * Source: https://oldschool.runescape.wiki/w/Shooting_Stars#Landing_sites
 * Reviewed 24 July 2026.
 */
object StarTravelCatalog {
    val guides: Map<String, StarTravelGuide> = listOf(
        // Asgarnia
        g(
            "North Dwarven Mine entrance",
            r("Lassar Teleport", "Teleport to Ice Mountain and run south-east to the northern mine entrance.", "ancient_lassar"),
            r("Combat bracelet", "Teleport to Edgeville Monastery and run south-west.", "jew_combat"),
            r("Skull sceptre", "Teleport to the Stronghold of Security and run west to Ice Mountain.", "item_skull"),
        ),
        g(
            "East Falador bank",
            r("Skills necklace", "Teleport to the Mining Guild; the star is by the eastern Falador bank / guild entrance.", "jew_skills"),
            r("Ring of wealth", "Teleport to Falador Park and run south to the east bank.", "jew_wealth"),
            r("Falador Teleport", "Teleport to Falador and run south-east to the bank.", "spell_falador|tab_falador"),
        ),
        g(
            "West Falador mine",
            r("Falador Teleport + wall shortcut", "Run west and climb the crumbling wall, then continue south-west to the mine.", "spell_falador|tab_falador", agility = 5),
            r("Skills necklace", "Teleport to the Crafting Guild and run north-west.", "jew_skills"),
            r("Crafting cape", "Teleport to the Crafting Guild and run north-west.", "item_crafting"),
            r("Balloon transport", "Take the balloon to the Crafting Guild and run north-west.", "travel_balloon"),
        ),
        g(
            "Taverley house portal",
            r("Taverley house teleport", "Arrive at the Taverley house portal; the star is beside the White Wolf Tunnel approach.", "house:taverley"),
            r("Games necklace", "Teleport to Burthorpe and run south-west into Taverley.", "jew_games"),
            r("Combat bracelet", "Teleport to the Warriors' Guild and run south.", "jew_combat"),
        ),
        g(
            "Crafting guild",
            r("Skills necklace", "Teleport directly to the Crafting Guild.", "jew_skills", requirements = "Brown apron or Hard Falador Diary"),
            r("Crafting cape", "Teleport directly inside the Crafting Guild.", "item_crafting"),
            r("Rimmington house teleport", "Teleport to the Rimmington portal and run north.", "house:rimmington"),
            r("Balloon transport", "Take the balloon to the Crafting Guild.", "travel_balloon"),
            r("Ring of the elements", "Teleport to the Air Altar and run west.", "jew_elements"),
        ),
        g(
            "Rimmington mine",
            r("Rimmington house teleport", "Teleport to the Rimmington house portal and run south-east to the mine.", "house:rimmington"),
            r("Minigame teleport", "Teleport to the Port Sarim Rat Pits and run west.", "item_minigame"),
            r("Explorer's ring", "Teleport to the cabbage patch and run south-west past Port Sarim.", "item_explorer"),
            r("Ring of the elements", "Teleport to the Air Altar and run south.", "jew_elements"),
        ),

        // Crandor and Karamja
        g(
            "South Crandor",
            r("Achievement diary cape", "Teleport to TzHaar-Mej, exit the volcano, then climb to Crandor and run south.", "item_achievement", requirements = "Dragon Slayer I access"),
            r("BLP fairy ring", "Use BLP, exit the volcano, climb to Crandor and run south.", "network_fairy", requirements = "Dragon Slayer I access"),
            r("Fight Pit minigame teleport", "Exit Karamja volcano, climb to Crandor and run south.", "item_minigame", requirements = "Dragon Slayer I access"),
            r("Amulet of glory", "Teleport to Karamja, enter the volcano, climb to Crandor and run south.", "jew_glory", requirements = "Dragon Slayer I access"),
        ),
        g(
            "North Crandor",
            r("Achievement diary cape", "Teleport to TzHaar-Mej, exit the volcano and climb to the northern Crandor mine.", "item_achievement", requirements = "Dragon Slayer I access"),
            r("BLP fairy ring", "Use BLP, exit the volcano and climb to northern Crandor.", "network_fairy", requirements = "Dragon Slayer I access"),
            r("Fight Pit minigame teleport", "Exit Karamja volcano and climb to northern Crandor.", "item_minigame", requirements = "Dragon Slayer I access"),
            r("Amulet of glory", "Teleport to Karamja, enter the volcano and climb to northern Crandor.", "jew_glory", requirements = "Dragon Slayer I access"),
        ),
        g(
            "Brimhaven northwest gold mine",
            r("BJR fairy ring + whistle", "Use BJR, blow the magic whistle, then leave the realm and run to the north Brimhaven mine.", "network_fairy", requirements = "Magic whistle"),
            r("Brimhaven house teleport", "Teleport to the Brimhaven house portal and run north.", "house:brimhaven"),
            r("Spirit tree", "Teleport to the Brimhaven spirit tree and run north-west.", "network_spirit"),
            r("Amulet of glory", "Teleport to Karamja and run west through Brimhaven.", "jew_glory"),
        ),
        g(
            "Southwest of Brimhaven Poh",
            r("Brimhaven house teleport", "Teleport to the Brimhaven house portal and run south-west.", "house:brimhaven"),
            r("Tai Bwo Wannai teleport", "Teleport to Tai Bwo Wannai and run north-west.", "scroll_tai_bwo_wannai"),
        ),
        g(
            "Nature Altar mine north of Shilo",
            r("Achievement diary cape", "Teleport to Kaleb Paramaya and run north from Shilo.", "item_achievement"),
            r("Tai Bwo Wannai teleport", "Teleport to Tai Bwo Wannai and run south-east toward the Nature Altar mine.", "scroll_tai_bwo_wannai"),
            r("CKR fairy ring", "Use CKR and run south-east.", "network_fairy"),
            r("Nature Altar via Abyss", "Enter the Nature Altar through the Abyss, exit and run south.", "travel_abyss"),
        ),
        g(
            "Shilo Village gem mine",
            r("Karamja gloves 3/4", "Teleport to the Shilo gem mine.", "item_karamja_gloves"),
            r("Achievement diary cape", "Teleport to Kaleb Paramaya and run to the mine.", "item_achievement"),
            r("Shilo cart", "Take the cart from Brimhaven to Shilo Village and run to the gem mine.", "travel_shilo_cart", requirements = "10 coins"),
        ),

        // Feldip Hills and Isle of Souls
        g(
            "Feldip Hills (aks fairy ring)",
            r("AKS fairy ring", "Use fairy ring AKS; the star is in the nearby Feldip hunter area.", "network_fairy"),
        ),
        g(
            "Rantz cave",
            r("AKS fairy ring", "Use fairy ring AKS and run north-east to Rantz's cave.", "network_fairy"),
        ),
        g(
            "Corsair Cove bank",
            r("Charter ship", "Charter from Rimmington to Corsair Cove and run to the bank.", "travel_charter"),
            r("Spirit tree", "Teleport to the Feldip Hills spirit tree and run south.", "network_spirit"),
            r("Hunter cape", "Teleport to the carnivorous chinchompa area and run south.", "item_hunter"),
            r("Mythical cape", "Teleport to the Myths' Guild and run east into Corsair Cove.", "item_mythical"),
        ),
        g(
            "Corsair Resource Area",
            r("Spirit tree", "Teleport to Feldip Hills and run south-west into the resource area.", "network_spirit"),
            r("Mythical cape", "Teleport to the Myths' Guild and run north-west.", "item_mythical"),
            r("Hunter cape", "Teleport to carnivorous chinchompas and run south-west.", "item_hunter"),
        ),
        g(
            "Myths' Guild",
            r("Mythical cape", "Teleport directly to the Myths' Guild.", "item_mythical", requirements = "Dragon Slayer II"),
            r("Spirit tree", "Teleport to Feldip Hills and run south to the guild.", "network_spirit", requirements = "Dragon Slayer II"),
        ),
        g(
            "Soul Wars south mine",
            r("Soul Wars minigame teleport", "Teleport to Soul Wars and run south to the mine.", "item_minigame"),
            r("Amulet of glory", "Teleport to Edgeville, enter the Soul Wars portal and run south.", "jew_glory"),
            r("Ring of dueling", "Teleport to Ferox Enclave, use the dungeon route to the Isle of Souls, then run south.", "jew_dueling"),
        ),

        // Fossil Island and Mos Le'Harmless
        g(
            "Fossil Island rune rocks",
            r("Volcanic Mine teleport", "Teleport to Volcanic Mine and run west to the Fossil Island mine.", "item_volcanic|tab_volcanic_mine"),
            r("Digsite pendant", "Teleport to the Digsite, take the barge to Fossil Island, then use the mushroom network toward the mine.", "jew_digsite", requirements = "Bone Voyage"),
        ),
        g(
            "Fossil Island Volcanic Mine entrance",
            r("Volcanic Mine teleport", "Teleport directly to the Volcanic Mine entrance.", "item_volcanic|tab_volcanic_mine"),
            r("Digsite pendant + mushrooms", "Teleport to the House on the Hill and take the mycelium transport to Verdant Valley.", "jew_digsite|travel_mycelium", requirements = "Bone Voyage"),
        ),
        g(
            "Mos Le'Harmless west bank",
            r("Mos Le'Harmless teleport", "Use the scroll or master scroll book and run to the west bank.", "scroll_mos_le_harmless"),
            r("Charter ship", "Charter to Mos Le'Harmless and run west.", "travel_charter", requirements = "Cabin Fever"),
            r("Harmony Island Teleport", "Teleport to Harmony Island, take the boat to Mos Le'Harmless and run west.", "arceuus_harmony"),
            r("Ectophial", "Teleport to the Ectofuntus, travel to Port Phasmatys, then take the Mos Le'Harmless boat.", "item_ectophial", requirements = "Cabin Fever"),
        ),

        // Fremennik and Lunar Isle
        g(
            "Rellekka mine",
            r("Fremennik sea boots", "Teleport to Rellekka and run north-east to the mine.", "item_sea_boots"),
            r("Enchanted lyre", "Teleport to Rellekka and run north-east.", "item_lyre"),
            r("Rellekka house teleport", "Teleport to the Rellekka portal and run north.", "house:rellekka"),
            r("Achievement diary cape", "Teleport to Thorodin and run into Rellekka.", "item_achievement"),
        ),
        g(
            "Keldagrim entrance mine",
            r("DKS fairy ring", "Use fairy ring DKS and run east to the Keldagrim entrance mine.", "network_fairy"),
            r("Slayer ring", "Teleport to the Fremennik Slayer Dungeon and run east.", "jew_slayer"),
        ),
        g(
            "Miscellania mine (cip fairy ring)",
            r("CIP fairy ring", "Use fairy ring CIP and run to the Miscellania mine.", "network_fairy", requirements = "The Fremennik Trials"),
            r("Ring of wealth", "Teleport to Miscellania and run to the mine.", "jew_wealth", requirements = "Throne of Miscellania"),
            r("Fremennik sea boots", "Teleport to Rellekka, take the Miscellania boat, then run to the mine.", "item_sea_boots"),
        ),
        g(
            "Jatizso mine entrance",
            r("Enchanted lyre", "Teleport to Jatizso and run to the mine entrance.", "item_lyre", requirements = "The Fremennik Isles"),
            r("Fremennik sea boots", "Teleport to Rellekka, take the Jatizso boat and run to the mine.", "item_sea_boots", requirements = "The Fremennik Isles"),
            r("Rellekka house teleport", "Teleport to Rellekka, take the Jatizso boat and run to the mine.", "house:rellekka", requirements = "The Fremennik Isles"),
        ),
        g(
            "Neitiznot south of rune rock",
            r("Enchanted lyre", "Teleport to Neitiznot and run to the central-isles mine south of the rune rock.", "item_lyre", requirements = "The Fremennik Isles"),
            r("Fremennik sea boots", "Teleport to Rellekka, take the Neitiznot boat and run to the mine.", "item_sea_boots", requirements = "The Fremennik Isles"),
            r("Rellekka house teleport", "Teleport to Rellekka, take the Neitiznot boat and run to the mine.", "house:rellekka", requirements = "The Fremennik Isles"),
        ),
        g(
            "Lunar Isle mine entrance",
            r("Moonclan Teleport", "Teleport to Moonclan and run north-east to the mine entrance.", "lunar_moonclan"),
            r("Lunar Home Teleport", "Home teleport to Lunar Isle and run north-east.", "home_lunar"),
            r("Lunar Isle teleport scroll", "Teleport to Lunar Isle and run north-east.", "scroll_lunar_isle"),
        ),

        // Great Kourend
        g(
            "Hosidius mine",
            r("Hosidius house teleport", "Teleport to the Hosidius house portal and run north-east to the mine.", "house:hosidius"),
            r("Tithe Farm minigame teleport", "Teleport to Tithe Farm and run north-west.", "item_minigame"),
            r("Xeric's Glade", "Use Xeric's talisman to the Glade and run east.", "item_xeric"),
        ),
        g(
            "Port Piscarilius mine in Kourend",
            r("Book of the dead", "Use The Fisher's Flute teleport and run to the Port Piscarilius mine.", "item_kharedst"),
            r("Lovakengj minecart", "Take the minecart to Port Piscarilius and run to the mine.", "travel_minecart"),
            r("Kourend Castle Teleport", "Teleport to Kourend Castle and run north-east to Port Piscarilius.", "spell_kourend"),
        ),
        g(
            "Shayzien mine south of Kourend Castle",
            r("Kourend Castle Teleport", "Teleport to Kourend Castle and run south-west to the Shayzien mine.", "spell_kourend"),
            r("Xeric's Heart", "Use Xeric's talisman to the Heart and run south.", "item_xeric"),
        ),
        g(
            "Arceuus dense essence mine",
            r("Arceuus Home Teleport", "Home teleport to the Dark Altar and run north to the dense essence mine.", "home_arceuus"),
            r("CIS fairy ring", "Use fairy ring CIS and run north-west.", "network_fairy"),
            r("Book of the dead", "Use A Dark Disposition and run north to the mine.", "item_kharedst"),
        ),
        g(
            "Lovakite mine",
            r("Xeric's Inferno", "Use Xeric's talisman to the Inferno and run east to the lovakite mine.", "item_xeric"),
        ),
        g(
            "South Lovakengj bank",
            r("Book of the dead", "Use Jewellery of Jubilation and run to the south Lovakengj bank.", "item_kharedst"),
            r("Xeric's Inferno", "Use Xeric's talisman to the Inferno and run north-west.", "item_xeric"),
            r("Lovakengj minecart", "Take the minecart to Lovakengj and run south.", "travel_minecart"),
        ),

        // Kandarin
        g(
            "Catherby bank",
            r("Catherby Teleport", "Teleport to Catherby; the star is beside the bank.", "lunar_catherby"),
            r("Camelot Teleport", "Teleport to Camelot and run south-east to Catherby.", "spell_camelot|tab_camelot"),
        ),
        g(
            "Yanille bank",
            r("Watchtower Teleport to Yanille", "Use the hard-diary Yanille destination and run north to the bank.", "spell_watchtower|tab_watchtower", requirements = "Hard Ardougne Diary destination"),
            r("Nightmare Zone minigame teleport", "Teleport to Nightmare Zone and run south-east into Yanille.", "item_minigame"),
            r("Yanille house teleport", "Teleport to the Yanille house portal and run east to the bank.", "house:yanille"),
            r("Watchtower Teleport", "Teleport to the Watchtower, climb down and run east into Yanille.", "spell_watchtower|tab_watchtower", requirements = "Watchtower"),
        ),
        g(
            "Port Khazard mine",
            r("Ardougne cloak", "Teleport to the monastery and run south-east to Port Khazard mine.", "item_ardougne_cloak"),
            r("Minigame teleport", "Teleport to Fishing Trawler or Nightmare Zone and run to the mine.", "item_minigame"),
            r("Watchtower Teleport", "Teleport to the Watchtower and run south-east.", "spell_watchtower|tab_watchtower"),
        ),
        g(
            "South of Legends' Guild",
            r("Quest point cape", "Teleport to the Legends' Guild and run just south.", "item_quest_cape"),
            r("Ardougne Teleport", "Teleport to East Ardougne and run north-east to the guild mine.", "spell_ardougne|tab_ardougne"),
            r("BLR fairy ring", "Use fairy ring BLR and run west.", "network_fairy"),
        ),
        g(
            "Coal Trucks west of Seers'",
            r("Combat bracelet", "Teleport to the Ranging Guild and run north-west to the coal trucks.", "jew_combat"),
            r("Fishing Guild teleport", "Use a fishing cape or skills necklace and run west.", "item_fishing|jew_skills"),
            r("ALS fairy ring", "Use fairy ring ALS and run north.", "network_fairy"),
            r("Seers' Camelot Teleport", "Use the hard-diary Seers' destination and run west.", "spell_camelot|tab_camelot", requirements = "Hard Kandarin Diary destination"),
            r("Camelot Teleport", "Teleport to Camelot and run north-west.", "spell_camelot|tab_camelot"),
        ),
        g(
            "Ardougne Monastery",
            r("Ardougne cloak", "Teleport to the monastery; the star is beside the south-east Ardougne mine.", "item_ardougne_cloak"),
            r("DJP fairy ring", "Use fairy ring DJP and run north-west.", "network_fairy"),
        ),

        // Kebos
        g(
            "Kebos Swamp mine",
            r("Farming Guild teleport", "Use a skills necklace or farming cape and run south into Kebos Swamp.", "jew_skills|item_farming"),
            r("CIR fairy ring", "Use fairy ring CIR and run south-east.", "network_fairy"),
        ),
        g(
            "Mount Karuulm mine",
            r("Rada's blessing 3/4", "Teleport to Mount Karuulm and run west to the mine.", "item_rada"),
            r("CIR fairy ring", "Use fairy ring CIR and run north-east up Mount Karuulm.", "network_fairy"),
        ),
        g(
            "Mount Karuulm bank",
            r("Rada's blessing 3/4", "Teleport to Mount Karuulm; the star is beside the bank.", "item_rada"),
            r("CIR fairy ring", "Use fairy ring CIR and climb north-east to the bank.", "network_fairy"),
        ),
        g(
            "Chambers of Xeric bank",
            r("Xeric's Honour", "Use Xeric's talisman to teleport directly to Mount Quidamortem.", "item_xeric"),
            r("Lovakengj minecart", "Take the minecart toward Mount Quidamortem, then use the mountain guide.", "travel_minecart"),
            r("BLS fairy ring", "Use fairy ring BLS and take the mountain guide to Mount Quidamortem.", "network_fairy"),
        ),

        // Kharidian Desert
        g(
            "Al Kharid mine",
            r("Ring of the elements", "Teleport to the Fire Altar and run north-west to the mine.", "jew_elements"),
            r("Ring of dueling", "Teleport to Emir's Arena and run west to the mine.", "jew_dueling"),
        ),
        g(
            "Al Kharid bank",
            r("Amulet of glory", "Teleport to Al Kharid; the star is beside the bank.", "jew_glory"),
            r("Gnome glider", "Glide to Kar-Hewo and run north-west to the bank.", "travel_glider"),
            r("Ring of dueling", "Teleport to Emir's Arena and run north-west.", "jew_dueling"),
        ),
        g(
            "Nw of Uzer (Eagle's Eyrie)",
            r("Necklace of passage", "Teleport to Eagle's Eyrie and run south-east toward Uzer mine.", "jew_passage"),
            r("Magic carpet", "Take a carpet from Shantay Pass to Uzer and run north-west.", "travel_carpet"),
        ),
        g(
            "Desert Quarry mine",
            r("Camulet", "Teleport to Enakhra's mine entrance and run to the quarry.", "jew_camulet"),
            r("Pharaoh's sceptre", "Teleport to Jaldraocht and run south-west to the quarry.", "item_pharaoh"),
            r("Unkah ferry", "Take the ferry from Al Kharid to the Ruins of Unkah and run east.", "always", requirements = "Coins"),
        ),
        g(
            "Agility Pyramid mine",
            r("Pharaoh's sceptre", "Teleport to Jalsavrah, exit Sophanem through the eastern wall and run north-east.", "item_pharaoh"),
            r("Nardah teleport", "Teleport to Nardah and run south-west to the pyramid mine.", "scroll_nardah"),
            r("Desert amulet", "Teleport to Nardah and run south-west.", "jew_desert"),
        ),
        g(
            "Nardah bank",
            r("Nardah teleport", "Teleport to Nardah; the star is beside the bank.", "scroll_nardah"),
            r("Desert amulet", "Teleport to Nardah and run to the bank.", "jew_desert"),
            r("DLQ fairy ring", "Use fairy ring DLQ and run south into Nardah.", "network_fairy"),
        ),
        g(
            "North of Al Kharid PvP Arena",
            r("Ring of the elements", "Teleport to the Fire Altar, pass through the east gate and run north.", "jew_elements"),
            r("Ring of dueling", "Teleport to Emir's Arena and run north.", "jew_dueling"),
        ),

        // Misthalin
        g(
            "East Lumbridge Swamp mine",
            r("Ring of the elements", "Teleport to the Water Altar and run east.", "jew_elements"),
            r("Achievement diary cape", "Teleport to Hatius and run south into the swamp.", "item_achievement"),
            r("Lumbridge Teleport", "Teleport to Lumbridge and run south-east.", "spell_lumbridge|tab_lumbridge"),
            r("Lumbridge Home Teleport", "Home teleport and run south-east.", "home_lumbridge"),
        ),
        g(
            "West Lumbridge Swamp mine",
            r("Ring of the elements", "Teleport to the Water Altar and run west.", "jew_elements"),
            r("Zanaris fairy ring", "Travel to Zanaris, exit through the Lumbridge Swamp shed and run west.", "network_fairy"),
            r("Achievement diary cape", "Teleport to Twiggy O'Korn and run south-east.", "item_achievement"),
            r("BIQ grapple shortcut", "Use BIQ, grapple across and run north-east to the mine.", "network_fairy", agility = 42, requirements = "Mith grapple; crossbow; 40 Ranged; 40 Strength"),
            r("Necklace of passage", "Teleport to the Wizards' Tower and run east into the swamp.", "jew_passage"),
            r("Amulet of glory", "Teleport to Draynor and run south-east.", "jew_glory"),
            r("Lumbridge Teleport", "Teleport to Lumbridge and run south-west.", "spell_lumbridge|tab_lumbridge"),
        ),
        g(
            "Draynor Village",
            r("Achievement diary cape", "Teleport to Twiggy O'Korn; the star is nearby.", "item_achievement"),
            r("Amulet of glory", "Teleport directly to Draynor Village.", "jew_glory"),
        ),
        g(
            "Varrock east bank",
            r("Achievement diary cape", "Teleport to Toby and run east to the bank.", "item_achievement"),
            r("Varrock Teleport", "Teleport to Varrock and run east to the bank.", "spell_varrock|tab_varrock"),
        ),
        g(
            "Southeast Varrock mine",
            r("Senntisten Teleport", "Teleport to the Digsite and run west to the south-east Varrock mine.", "ancient_senntisten"),
            r("Chronicle", "Teleport to the Champions' Guild and run north-east.", "item_chronicle"),
            r("Combat bracelet", "Teleport to the Champions' Guild and run north-east.", "jew_combat"),
        ),
        g(
            "Champions' Guild mine",
            r("Chronicle", "Teleport to the Champions' Guild and run west to the mine.", "item_chronicle"),
            r("Combat bracelet", "Teleport to the Champions' Guild and run west.", "jew_combat"),
        ),

        // Morytania
        g(
            "Canifis bank",
            r("Kharyrll Teleport", "Teleport to Canifis; the star is beside the bank.", "ancient_kharyrll"),
            r("CKS fairy ring", "Use fairy ring CKS and run north into Canifis.", "network_fairy"),
            r("Salve Graveyard Teleport", "Teleport to the graveyard and run north-east into Canifis.", "arceuus_salve|tab_salve_graveyard"),
        ),
        g(
            "Burgh de Rott bank",
            r("Morytania legs 3/4", "Teleport to Burgh de Rott; the star is beside the bank.", "item_morytania_legs"),
            r("Mort'ton teleport", "Teleport to Mort'ton and run south to Burgh de Rott.", "scroll_mort_ton"),
            r("Shades of Mort'ton minigame teleport", "Teleport to Mort'ton and run south.", "item_minigame"),
        ),
        g(
            "Abandoned Mine west of Burgh",
            r("Morytania legs 3/4", "Teleport to Burgh de Rott and run west to the Abandoned Mine.", "item_morytania_legs"),
            r("Mort'ton teleport", "Teleport to Mort'ton and run south-west.", "scroll_mort_ton"),
            r("Shades of Mort'ton minigame teleport", "Teleport to Mort'ton and run south-west.", "item_minigame"),
        ),
        g(
            "Theatre of Blood bank",
            r("Drakan's medallion", "Teleport to Ver Sinhaza; the star is beside the Theatre bank.", "item_drakan"),
            r("Andras' boat", "Travel from the Ectofuntus to Slepe, then continue to Ver Sinhaza.", "always", requirements = "A Taste of Hope access"),
        ),
        g(
            "Darkmeyer ess. mine entrance",
            r("Hallowed crystal shard", "Teleport to Darkmeyer and run to the Daeyalt essence mine entrance.", "item_hallowed"),
            r("Drakan's medallion: Darkmeyer", "Teleport to Darkmeyer and run to the mine entrance.", "item_drakan", requirements = "Sins of the Father"),
            r("Ver Sinhaza route", "Teleport to Ver Sinhaza, use the Vyrewatch mine route, then hop the wall to Daeyalt.", "item_drakan", requirements = "Vyrewatch outfit; Sins of the Father"),
        ),

        // Piscatoris and Gnome Stronghold
        g(
            "Piscatoris (akq fairy ring)",
            r("Piscatoris teleport", "Use a teleport scroll or master scroll book and run to the mine.", "scroll_piscatoris"),
            r("AKQ fairy ring", "Use fairy ring AKQ; the star is at the nearby mine.", "network_fairy"),
            r("Western banner 3/4", "Teleport to the Piscatoris colony and run to the mine.", "item_western_banner"),
        ),
        g(
            "West of Grand Tree",
            r("Grand / royal seed pod", "Teleport to the Grand Tree and run west.", "item_grand_seed|item_seed_pod"),
            r("Achievement diary cape", "Teleport to the Western Provinces master and run west.", "item_achievement"),
            r("Spirit tree", "Teleport to the Gnome Stronghold and run west of the Grand Tree.", "network_spirit"),
            r("Balloon transport", "Take the balloon to the Grand Tree and run west.", "travel_balloon"),
            r("Gnome glider", "Glide to the Grand Tree and run west.", "travel_glider"),
        ),
        g(
            "Gnome Stronghold spirit tree",
            r("Spirit tree", "Teleport to Gnome Stronghold; the star is near the bank and spirit tree.", "network_spirit"),
            r("Slayer ring", "Teleport to the Stronghold Slayer Cave and run north-west.", "jew_slayer"),
            r("Achievement diary cape", "Teleport to the Western Provinces master and run south.", "item_achievement"),
            r("Grand / royal seed pod", "Teleport to the Grand Tree and run south.", "item_grand_seed|item_seed_pod"),
        ),

        // Tirannwn
        g(
            "Isafdar runite rocks",
            r("Teleport crystal: Lletya", "Teleport to Lletya, leave through the trees and run west to the Isafdar mine.", "item_crystal", requirements = "Underground Pass"),
        ),
        g(
            "Arandar mine north of Lletya",
            r("Iorwerth camp teleport", "Teleport to Iorwerth Camp and run north-east to Arandar mine.", "scroll_iorwerth_camp"),
            r("Necklace of passage", "Teleport to the Outpost, cross Arandar and run south to the mine.", "jew_passage", requirements = "Underground Pass"),
            r("Teleport crystal: Prifddinas", "Teleport to Prifddinas, leave south-east and run to Arandar mine.", "item_crystal", requirements = "Song of the Elves"),
        ),
        g(
            "Lletya",
            r("Teleport crystal: Lletya", "Teleport directly to Lletya; the star is nearby.", "item_crystal", requirements = "Mourning's End Part I"),
        ),
        g(
            "Prifddinas Zalcano entrance",
            r("Teleport crystal: Prifddinas", "Teleport to Prifddinas and run to the Trahaearn mine / Zalcano entrance.", "item_crystal", requirements = "Song of the Elves"),
            r("Spirit tree: Prifddinas", "Teleport to Prifddinas and run north-west to Trahaearn.", "network_spirit", requirements = "Song of the Elves"),
            r("Prifddinas house teleport", "Teleport to the Prifddinas house portal and run to Trahaearn.", "house:prifddinas", requirements = "Song of the Elves"),
        ),
        g(
            "Mynydd nw of Prifddinas",
            r("Charter ship: Prifddinas", "Charter to Prifddinas and run north-west to Mynydd mine.", "travel_charter", requirements = "Song of the Elves"),
            r("Spirit tree: Prifddinas", "Teleport to Prifddinas and run north-west out to Mynydd.", "network_spirit", requirements = "Song of the Elves"),
            r("Prifddinas house teleport", "Teleport to the Prifddinas portal and run north-west.", "house:prifddinas", requirements = "Song of the Elves"),
            r("Teleport crystal: Prifddinas", "Teleport to Prifddinas and run north-west.", "item_crystal", requirements = "Song of the Elves"),
        ),

        // Varlamore
        g(
            "Varlamore colosseum entrance bank",
            r("Ring of dueling", "Teleport to Fortis Colosseum and run to the east bank.", "jew_dueling"),
            r("Quetzal: Fortis Colosseum", "Fly to the Colosseum and run to the east bank.", "network_quetzal"),
            r("Civitas illa Fortis Teleport", "Teleport to Civitas and run east to the bank.", "spell_civitas|tab_civitas_illa_fortis"),
        ),
        g(
            "Varlamore South East mine",
            r("Quetzal: Colossal Wyrm", "Fly to Colossal Wyrm Remains and run south-east to Stonecutter Outpost.", "network_quetzal"),
            r("AJP fairy ring", "Use fairy ring AJP and run north-west to the mine.", "network_fairy"),
        ),
        g(
            "Mine north-west of hunter guild",
            r("Pendant of ates: Ralos' Rise", "Teleport to Ralos' Rise, climb down the nearby rocks, then run south-east.", "jew_ates", agility = 47),
            r("Quetzal: Cam Torum", "Fly to Cam Torum entrance and run south-east to the mining site.", "network_quetzal"),
            r("Calcified moth", "Teleport to Cam Torum and leave toward the mining site.", "item_calcified"),
        ),
        g(
            "Aldarin mine",
            r("Quetzal: Aldarin", "Fly to Aldarin and run to Mistrock mine.", "network_quetzal"),
            r("CKQ fairy ring", "Use fairy ring CKQ and run north-east to the mine.", "network_fairy"),
            r("Aldarin house teleport", "Teleport to the Aldarin house portal and run to Mistrock.", "house:aldarin"),
        ),
        g(
            "Salvager Overlook",
            r("Quetzal: Salvager Overlook", "Fly directly to Salvager Overlook and run to the mine.", "network_quetzal"),
            r("Pendant of ates: Twilight Temple", "Teleport to Twilight Temple and run north-west to the overlook.", "jew_ates"),
        ),
        g(
            "Custodia Mountains",
            r("Quetzal: Auburnvale", "Fly to Auburnvale, then run west and north into the Custodia Mountains.", "network_quetzal"),
            r("BLS fairy ring", "Use fairy ring BLS and run south-west into the mountains.", "network_fairy"),
        ),

        // Wilderness
        g(
            "Mage of Zamorak mine (lvl 7 Wildy)",
            r("Achievement diary cape", "Teleport to the Lesser Fanatic and run to the south Wilderness mine.", "item_achievement", dangerous = true),
            r("Amulet of glory", "Teleport to Edgeville, cross the ditch and run north-east.", "jew_glory", dangerous = true),
            r("Ring of dueling", "Teleport to Ferox Enclave and run south-west.", "jew_dueling", dangerous = true),
        ),
        g(
            "Skeleton mine (lvl 10 Wildy)",
            r("Burning amulet: Bandit Camp", "Teleport to Bandit Camp and run south to the skeleton mine.", "jew_burning", dangerous = true),
            r("Mind Altar Teleport", "Teleport to the Mind Altar, cross the ditch and run north.", "arceuus_mind", dangerous = true),
            r("Dareeyak Teleport", "Teleport into the Wilderness and run south-east.", "ancient_dareeyak", dangerous = true),
            r("Combat bracelet", "Teleport to Edgeville Monastery, cross the ditch and run north-west.", "jew_combat", dangerous = true),
        ),
        g(
            "Hobgoblin mine (lvl 30 Wildy)",
            r("Wilderness obelisk", "Use the level 35 or 27 obelisk and run to Bandit Camp mine.", "network_obelisk", dangerous = true),
            r("Cemetery Teleport", "Teleport to the Forgotten Cemetery and run east.", "arceuus_cemetery|tab_cemetery", dangerous = true),
            r("Ring of dueling", "Teleport to Ferox Enclave and run north-west.", "jew_dueling", dangerous = true),
            r("Burning amulet", "Teleport to Bandit Camp or Lava Maze and run to the hobgoblin mine.", "jew_burning", dangerous = true),
        ),
        g(
            "Lava maze runite mine (lvl 46 Wildy)",
            r("Wilderness obelisk", "Use the level 44 obelisk and run south-east to the rune rocks.", "network_obelisk", dangerous = true),
            r("Ice Plateau Teleport", "Teleport to Ice Plateau and run east to the Lava Maze mine.", "lunar_ice_plateau", dangerous = true),
            r("Ghorrock Teleport", "Teleport to the Frozen Waste Plateau and run south-east.", "ancient_ghorrock", dangerous = true),
            r("Burning amulet: Lava Maze", "Teleport to Lava Maze and run north-west to the rune rocks.", "jew_burning", dangerous = true),
        ),
        g(
            "Wilderness Resource Area",
            r("Deserted Keep lever", "Use the Ardougne or Edgeville lever, cut the web and run east to the Resource Area.", "travel_lever", requirements = "Knife/slashing weapon; 7,500 coins unless diary exemption", dangerous = true),
        ),
        g(
            "Mage Arena bank (lvl 56 Wildy)",
            r("Deserted Keep lever", "Use the Ardougne or Edgeville lever, cut the webs and run north-west to Mage Arena bank.", "travel_lever", requirements = "Knife/slashing weapon", dangerous = true),
        ),
        g(
            "Pirates' Hideout (lvl 53 Wildy)",
            r("Deserted Keep lever", "Use the lever, cut the web and run west to the Pirates' Hideout mine.", "travel_lever", requirements = "Knife/slashing weapon", dangerous = true),
            r("Ice Plateau Teleport", "Teleport to Ice Plateau and run north-east.", "lunar_ice_plateau", dangerous = true),
            r("Ghorrock Teleport", "Teleport to the Frozen Waste Plateau and run north-east.", "ancient_ghorrock", dangerous = true),
        ),
    ).associateBy { it.locationName }

    fun guideFor(locationName: String): StarTravelGuide? = guides[locationName]

    private fun g(name: String, vararg routes: StarTravelRoute) =
        StarTravelGuide(name, routes.toList())

    private fun r(
        method: String,
        steps: String,
        sources: String = "",
        agility: Int? = null,
        requirements: String = "",
        dangerous: Boolean = false,
    ) = StarTravelRoute(
        method = method,
        steps = steps,
        sourceKeys = sources.split("|").filter { it.isNotBlank() },
        agilityLevel = agility,
        requirements = requirements.split(";").map(String::trim).filter(String::isNotBlank),
        dangerous = dangerous,
    )
}

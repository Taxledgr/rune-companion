package io.github.taxledgr.runecompanion.data

/**
 * The complete landing-site catalogue currently published by Star Miners.
 * Names intentionally match the feed's calledLocation values.
 *
 * Source: https://map.starminers.site/Polygons.js
 */
object StarLocationCatalog {
    val areas: Map<String, List<String>> = linkedMapOf(
        "Asgarnia" to listOf(
            "Rimmington mine",
            "Crafting guild",
            "West Falador mine",
            "East Falador bank",
            "North Dwarven Mine entrance",
            "Taverley house portal",
        ),
        "Karamja" to listOf(
            "Brimhaven northwest gold mine",
            "Southwest of Brimhaven Poh",
            "Nature Altar mine north of Shilo",
            "Shilo Village gem mine",
            "North Crandor",
            "South Crandor",
        ),
        "Feldip Hills" to listOf(
            "Corsair Cove bank",
            "Corsair Resource Area",
            "Myths' Guild",
            "Feldip Hills (aks fairy ring)",
            "Rantz cave",
            "Soul Wars south mine",
        ),
        "Fossil & Mos Le'Harmless" to listOf(
            "Fossil Island Volcanic Mine entrance",
            "Fossil Island rune rocks",
            "Mos Le'Harmless west bank",
        ),
        "Fremennik" to listOf(
            "Keldagrim entrance mine",
            "Rellekka mine",
            "Jatizso mine entrance",
            "Neitiznot south of rune rock",
            "Miscellania mine (cip fairy ring)",
            "Lunar Isle mine entrance",
        ),
        "Great Kourend" to listOf(
            "Hosidius mine",
            "Port Piscarilius mine in Kourend",
            "Shayzien mine south of Kourend Castle",
            "South Lovakengj bank",
            "Lovakite mine",
            "Arceuus dense essence mine",
        ),
        "Kandarin" to listOf(
            "Yanille bank",
            "Port Khazard mine",
            "Ardougne Monastery",
            "South of Legends' Guild",
            "Catherby bank",
            "Coal Trucks west of Seers'",
        ),
        "Kebos" to listOf(
            "Mount Karuulm bank",
            "Mount Karuulm mine",
            "Kebos Swamp mine",
            "Chambers of Xeric bank",
        ),
        "Kharidian Desert" to listOf(
            "North of Al Kharid PvP Arena",
            "Al Kharid mine",
            "Al Kharid bank",
            "Nw of Uzer (Eagle's Eyrie)",
            "Nardah bank",
            "Agility Pyramid mine",
            "Desert Quarry mine",
        ),
        "Misthalin" to listOf(
            "Varrock east bank",
            "Southeast Varrock mine",
            "Champions' Guild mine",
            "Draynor Village",
            "West Lumbridge Swamp mine",
            "East Lumbridge Swamp mine",
        ),
        "Morytania" to listOf(
            "Darkmeyer ess. mine entrance",
            "Theatre of Blood bank",
            "Canifis bank",
            "Burgh de Rott bank",
            "Abandoned Mine west of Burgh",
        ),
        "Piscatoris & Gnome" to listOf(
            "West of Grand Tree",
            "Gnome Stronghold spirit tree",
            "Piscatoris (akq fairy ring)",
        ),
        "Tirannwn" to listOf(
            "Lletya",
            "Isafdar runite rocks",
            "Prifddinas Zalcano entrance",
            "Arandar mine north of Lletya",
            "Mynydd nw of Prifddinas",
        ),
        "Wilderness" to listOf(
            "Mage of Zamorak mine (lvl 7 Wildy)",
            "Skeleton mine (lvl 10 Wildy)",
            "Hobgoblin mine (lvl 30 Wildy)",
            "Lava maze runite mine (lvl 46 Wildy)",
            "Pirates' Hideout (lvl 53 Wildy)",
            "Mage Arena bank (lvl 56 Wildy)",
            "Wilderness Resource Area",
        ),
        "Varlamore" to listOf(
            "Varlamore South East mine",
            "Mine north-west of hunter guild",
            "Varlamore colosseum entrance bank",
            "Salvager Overlook",
            "Aldarin mine",
            "Custodia Mountains",
        ),
    )

    val allNames: List<String> = areas.values.flatten()
}

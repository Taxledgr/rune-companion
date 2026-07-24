package io.github.taxledgr.runecompanion.data

data class StarMapPoint(
    val locationName: String,
    val latitude: Int,
    val longitude: Int,
    val region: String,
) {
    val imageX: Int get() = longitude - StarMapCatalog.MAP_LEFT
    val imageY: Int get() = StarMapCatalog.MAP_TOP - latitude
}

/**
 * Landing-site marker coordinates published by Star Miners.
 *
 * Source: https://map.starminers.site/Polygons.js
 * Map bounds: https://map.starminers.site/coordinates.js
 * Reviewed 25 July 2026.
 */
object StarMapCatalog {
    const val MAP_WIDTH = 8_712
    const val MAP_HEIGHT = 4_912
    const val MAP_LEFT = -263
    const val MAP_TOP = 4_952
    const val MAP_BASE_URL = "https://map.starminers.site/"
    const val MAP_IMAGE_URL =
        "https://map.starminers.site/images/Old_School_RuneScape_world_map3.png"

    private val regionByLocation = StarLocationCatalog.areas
        .flatMap { (region, locations) -> locations.map { it to region } }
        .toMap()

    val points: Map<String, StarMapPoint> by lazy {
        COORDINATES
            .trimIndent()
            .lineSequence()
            .filter(String::isNotBlank)
            .associate { row ->
                val (name, latitude, longitude) = row.split('|')
                name to StarMapPoint(
                    locationName = name,
                    latitude = latitude.toInt(),
                    longitude = longitude.toInt(),
                    region = regionByLocation.getValue(name),
                )
            }
    }

    fun pointFor(locationName: String): StarMapPoint? = points[locationName]

    fun previewHtml(point: StarMapPoint): String {
        val scale = 0.58
        val scaledWidth = MAP_WIDTH * scale
        val scaledHeight = MAP_HEIGHT * scale
        val scaledX = point.imageX * scale
        val scaledY = point.imageY * scale
        val location = point.locationName.htmlEscaped()
        val region = point.region.htmlEscaped()
        return """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
              <style>
                * { box-sizing: border-box; }
                html, body { width: 100%; height: 100%; margin: 0; overflow: hidden; background: #07131c; }
                #map { position: relative; width: 100%; height: 100%; overflow: hidden; background: #102331; }
                #world {
                  position: absolute;
                  z-index: 1;
                  width: ${scaledWidth}px;
                  height: ${scaledHeight}px;
                  max-width: none;
                  left: calc(50% - ${scaledX}px);
                  top: calc(50% - ${scaledY}px);
                }
                #shade {
                  position: absolute;
                  z-index: 2;
                  inset: 0;
                  box-shadow: inset 0 0 32px 12px rgba(3, 12, 18, .7);
                }
                #pin {
                  position: absolute;
                  z-index: 5;
                  left: 50%;
                  top: 50%;
                  width: 30px;
                  height: 30px;
                  transform: translate(-50%, -50%);
                  border: 3px solid #ffe078;
                  border-radius: 50%;
                  background: rgba(7, 19, 28, .82);
                  box-shadow: 0 0 0 7px rgba(244, 201, 93, .38), 0 3px 10px #000;
                }
                #pin::after {
                  content: "✦";
                  position: absolute;
                  left: 4px;
                  top: -1px;
                  color: #ffe078;
                  font: 700 20px sans-serif;
                }
                #label {
                  position: absolute;
                  z-index: 6;
                  left: 8px;
                  right: 8px;
                  bottom: 7px;
                  padding: 5px 8px;
                  color: #fff;
                  font: 700 12px sans-serif;
                  text-align: center;
                  border: 1px solid rgba(244, 201, 93, .65);
                  border-radius: 8px;
                  background: rgba(7, 19, 28, .9);
                }
                #source {
                  position: absolute;
                  z-index: 6;
                  top: 5px;
                  right: 6px;
                  padding: 3px 5px;
                  color: #c8d6df;
                  font: 9px sans-serif;
                  border-radius: 5px;
                  background: rgba(7, 19, 28, .82);
                }
              </style>
            </head>
            <body>
              <div id="map">
                <img id="world" src="$MAP_IMAGE_URL" alt="Old School RuneScape world map">
                <div id="shade"></div>
                <div id="pin"></div>
                <div id="source">Star Miners map</div>
                <div id="label">$location • $region</div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun String.htmlEscaped(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private const val COORDINATES = """
        Rimmington mine|2276|5506
        Crafting guild|2391|5404
        West Falador mine|2617|5302
        East Falador bank|2597|5673
        North Dwarven Mine entrance|2882|5637
        Taverley house portal|2974|5229
        Brimhaven northwest gold mine|2215|4791
        Southwest of Brimhaven Poh|1981|4811
        Nature Altar mine north of Shilo|1663|5120
        Shilo Village gem mine|1549|5064
        North Crandor|2441|5091
        South Crandor|2267|5050
        Corsair Cove bank|1126|4285
        Corsair Resource Area|1210|4033
        Myths' Guild|1078|3987
        Feldip Hills (aks fairy ring)|1444|4297
        Rantz cave|1532|4473
        Soul Wars south mine|928|3184
        Fossil Island Volcanic Mine entrance|3956|8038
        Fossil Island rune rocks|3995|7906
        Mos Le'Harmless west bank|1459|7642
        Keldagrim entrance mine|3600|4764
        Rellekka mine|3650|4633
        Jatizso mine entrance|3994|3763
        Neitiznot south of rune rock|4048|3706
        Miscellania mine (cip fairy ring)|4214|4168
        Lunar Isle mine entrance|4366|3001
        Hosidius mine|3031|1918
        Port Piscarilius mine in Kourend|3678|1891
        Shayzien mine south of Kourend Castle|3496|1375
        South Lovakengj bank|3792|1186
        Lovakite mine|4072|894
        Arceuus dense essence mine|4111|1865
        Yanille bank|1811|4390
        Port Khazard mine|1975|4455
        Ardougne Monastery|2252|4408
        South of Legends' Guild|2550|4698
        Catherby bank|2855|4996
        Coal Trucks west of Seers'|2986|4350
        Mount Karuulm bank|4000|548
        Mount Karuulm mine|4003|422
        Kebos Swamp mine|3505|215
        Chambers of Xeric bank|3244|358
        North of Al Kharid PvP Arena|2395|6636
        Al Kharid mine|2445|6472
        Al Kharid bank|2042|6412
        Nw of Uzer (Eagle's Eyrie)|2030|6850
        Nardah bank|1219|6886
        Agility Pyramid mine|1150|6528
        Desert Quarry mine|1282|6097
        Varrock east bank|2777|6357
        Southeast Varrock mine|2612|6453
        Champions' Guild mine|2637|6108
        Draynor Village|2257|5865
        West Lumbridge Swamp mine|2007|6045
        East Lumbridge Swamp mine|2017|6276
        Darkmeyer ess. mine entrance|2572|7489
        Theatre of Blood bank|2194|7535
        Canifis bank|3007|7098
        Burgh de Rott bank|2208|7084
        Abandoned Mine west of Burgh|2253|6938
        West of Grand Tree|3022|3916
        Gnome Stronghold spirit tree|2859|3928
        Piscatoris (akq fairy ring)|3457|3606
        Lletya|2041|3570
        Isafdar runite rocks|2025|3390
        Prifddinas Zalcano entrance|2449|3343
        Arandar mine north of Lletya|2360|3537
        Mynydd nw of Prifddinas|2780|3103
        Mage of Zamorak mine (lvl 7 Wildy)|3260|5908
        Skeleton mine (lvl 10 Wildy)|3331|5638
        Hobgoblin mine (lvl 30 Wildy)|3820|5864
        Lava maze runite mine (lvl 46 Wildy)|4214|5755
        Pirates' Hideout (lvl 53 Wildy)|4371|5731
        Mage Arena bank (lvl 56 Wildy)|4438|5857
        Wilderness Resource Area|4348|6148
        Varlamore South East mine|1435|1800
        Mine north-west of hunter guild|1835|1035
        Varlamore colosseum entrance bank|1875|1875
        Salvager Overlook|2385|1450
        Aldarin mine|1195|840
        Custodia Mountains|2780|460
    """
}

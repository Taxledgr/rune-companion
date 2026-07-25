package io.github.taxledgr.runecompanion.features

data class QuestGuideStep(
    val id: String,
    val title: String,
    val directions: String,
    val warning: String? = null,
)

data class QuestGuide(
    val questId: String,
    val inventory: List<String>,
    val equipment: List<String>,
    val teleports: List<String>,
    val steps: List<QuestGuideStep>,
)

/**
 * Concise, original checklists for Rune Companion's tracked quests. They are designed to keep
 * the player oriented while the OSRS Wiki quick guide remains available in-app for exact dialogue
 * choices, puzzles, combat mechanics, and changes made after this app release.
 */
object QuestGuideCatalog {
    val guides: Map<String, QuestGuide> = listOf(
        guide(
            "waterfall",
            inventory = listOf(
                "Rope", "6 Air runes", "6 Water runes", "6 Earth runes",
                "Food", "5 empty inventory spaces for the reward",
            ),
            equipment = listOf(
                "Armour for the Waterfall Dungeon", "Bank all weapons, armour, runes, arrows, and logs before Glarial's Tomb",
            ),
            teleports = listOf(
                "Games necklace → Barbarian Outpost", "Tree Gnome Village access", "Skills necklace → Fishing Guild",
            ),
            steps = listOf(
                step("start", "Almera and Hudon", "Talk to Almera north-east of Baxtorian Falls, ride the raft, and finish the conversation with Hudon."),
                step("book", "Baxtorian book", "Enter the tourist house south of the falls, speak to Hadley, then search the upstairs bookcase for the Book on Baxtorian."),
                step("pebble", "Glarial's pebble", "Reach Tree Gnome Village dungeon, free Golrie with the crate key, and receive Glarial's pebble."),
                step("tomb", "Glarial's Tomb", "Bank restricted gear, use the pebble on the tombstone, then search the western chest and southern coffin for the amulet and urn.", "High-level monsters can attack; jewellery and food are allowed, but many combat items are not."),
                step("dungeon", "Enter the Waterfall Dungeon", "Return to Almera's raft. Use a rope on the rock, then on the dead tree; wear Glarial's amulet and enter the doors."),
                step("key", "Find the dungeon key", "Take the eastern route and search crates for the key, then use it on the western doors beyond the Fire Giants."),
                step("pillars", "Charge the six pillars", "Use one Air, Water, and Earth rune on each of the six pillars."),
                step("finish", "Open the chalice", "Use Glarial's amulet on the statue, then the urn on the Chalice of Eternity and collect the reward.", "Do not click the chalice first; incorrect actions can wash you out or cause damage."),
            ),
        ),
        guide(
            "tree_gnome",
            inventory = listOf("6 normal logs", "Food", "Emergency teleport"),
            equipment = listOf("Combat gear for two level-48 commanders", "Combat gear for the Khazard Warlord"),
            teleports = listOf("Ardougne cloak / Ardougne teleport", "Fairy ring CIQ", "Ring of dueling → Castle Wars"),
            steps = listOf(
                step("start", "Meet King Bolren", "Enter the Tree Gnome Maze, follow Elkoy to the centre, and agree to recover the stolen orbs."),
                step("logs", "Repair the defences", "Give Commander Montai 6 normal logs, talk to him again, and locate all three tracker gnomes."),
                step("ballista", "Fire the ballista", "Remember the first two coordinates and interpret the third tracker's clue, then enter the coordinates at the ballista until it hits."),
                step("orb_one", "Recover the first orb", "Speak to Montai, climb the crumbled wall, pass or defeat both Khazard commanders, and search the upstairs chest."),
                step("return", "Report to the king", "Return through the maze with Elkoy and give the first orb to King Bolren."),
                step("warlord", "Defeat the Khazard Warlord", "Travel north-west of the battlefield, defeat the warlord, and pick up the remaining two orbs.", "The warlord is level 112; bring food and an emergency teleport."),
                step("finish", "Restore the Spirit Tree", "Return to King Bolren and complete the final conversation."),
            ),
        ),
        guide(
            "grand_tree",
            inventory = listOf("Bark sample", "Translation book", "Hazelmere's scroll", "Lumber order", "Glough's key", "Food"),
            equipment = listOf("Combat gear for a level-172 Black demon", "Ranged or Magic gear can make the final fight safer"),
            teleports = listOf("Spirit tree → Tree Gnome Stronghold", "Fairy ring AKQ", "Gnome glider access as the quest progresses"),
            steps = listOf(
                step("start", "The dying Grand Tree", "Speak to King Narnode Shareen in the Grand Tree and take the bark sample to Hazelmere."),
                step("translate", "Translate Hazelmere's message", "Return to the king, use the translation book, and report that someone is poisoning the tree."),
                step("glough", "Question Glough", "Speak to Glough, search his home for evidence, then show it to the king."),
                step("shipyard", "Investigate Karamja", "Use the glider route to Karamja, enter the shipyard with the password, and obtain the lumber order from the foreman."),
                step("anita", "Find Glough's key", "Return to the Stronghold, speak to Anita, and take her key to Glough's chest."),
                step("journal", "Expose the invasion plan", "Read Glough's journal, arrange the twigs as TUZO, and search the cupboard for the invasion plans."),
                step("roots", "Enter the roots", "Report to the king, enter the Grand Tree roots through the mine, and confront Glough."),
                step("demon", "Defeat the Black demon", "Defeat or safely range/mage the demon, then speak to the king in the roots.", "Use safe positioning and keep an emergency teleport."),
                step("finish", "Remove the Daconia rock", "Find the Daconia rock with the king's helpers and return it to King Narnode."),
            ),
        ),
        guide(
            "fairytale_ii",
            inventory = listOf("Dramen or Lunar staff", "Secateurs", "Vial of water", "Pestle and mortar", "Food"),
            equipment = listOf("Magic-defence gear", "Combat gear if continuing beyond fairy-ring unlock"),
            teleports = listOf("Fairy rings", "Ardougne cloak", "Skills necklace → Farming Guild"),
            steps = listOf(
                step("start", "Return to Zanaris", "Speak to Martin the Master Gardener, wait for his crops if required, then speak to him again and enter Zanaris."),
                step("queen", "Investigate the missing queen", "Speak to Fairy Nuff, search the queen's room, and take the certificate to the Fairy Godfather."),
                step("decode", "Decode the certificate", "Use the certificate clues with the Fairy Chef and Fairy Fixit to identify the fairy-ring sequence."),
                step("rings", "Follow the fairy-ring trail", "Use the required ring codes in order to reach the hideout and speak with Fairy Nuff."),
                step("potion", "Prepare the cure", "Collect the requested ingredients, make the magic essence potion, and bring it to the queen."),
                step("godfather", "Confront the Godfather", "Return to Zanaris and advance the plot with the Fairy Godfather and resistance."),
                step("unlock", "Secure fairy-ring access", "Complete the required conversation so fairy rings remain usable without finishing every later combat section.", "The quest can unlock fairy rings before full completion; keep your progress clear."),
                step("finish", "Complete the resistance sequence", "Follow the final hideout and combat instructions, then report back to finish the quest."),
            ),
        ),
        guide(
            "desert_treasure",
            inventory = listOf("Multiple lockpicks", "Shantay passes", "Food and prayer restoration", "Spiked boots", "Cake or chocolate cake", "Tinderbox"),
            equipment = listOf("Four separate boss loadouts", "Warm clothing for the ice path", "Anti-poison protection"),
            teleports = listOf("Digsite pendant", "Desert amulet / Nardah", "Games necklace", "Ancient Magicks area routes"),
            steps = listOf(
                step("start", "Meet the archaeologist", "Speak to the archaeologist at the Bedabin Camp, deliver the etched notes, and investigate the Digsite expert's translation."),
                step("mirrors", "Learn about the four diamonds", "Return to the archaeologist and then meet Eblis to reveal the four ancient mirrors."),
                step("blood", "Recover the Blood diamond", "Complete the Canifis tavern and Meiyerditch route, defeat Dessous, and take the diamond."),
                step("ice", "Recover the Ice diamond", "Prepare warm gear and spiked boots, cross Trollweiss Mountain, defeat Kamil, and rescue the child.", "Cold and stat drain continue during this section."),
                step("shadow", "Recover the Shadow diamond", "Navigate the shadow dungeon, use lockpicks and prayer supplies, defeat Damis through both forms, and collect the diamond."),
                step("smoke", "Recover the Smoke diamond", "Enter the Smoke Dungeon with face protection, light the torches, open the chest, and defeat Fareed.", "Wear ice gloves if your weapon would otherwise be unequipped."),
                step("pyramid", "Enter the pyramid", "Place all four diamonds at the pyramid, then descend through each floor while avoiding traps."),
                step("finish", "Unlock Ancient Magicks", "Reach the altar room and finish the conversation with Azzanadra."),
            ),
        ),
        guide(
            "lunar_diplomacy",
            inventory = listOf("Coins", "Tinderbox", "Pestle and mortar", "Needle and thread", "Pickaxe", "Hammer", "Food"),
            equipment = listOf("Seal of passage", "Combat is not the focus; keep weight low for travel"),
            teleports = listOf("Games necklace → Barbarian Outpost", "Enchanted lyre / Rellekka", "Moonclan teleport after unlock"),
            steps = listOf(
                step("start", "Join the diplomatic mission", "Speak to Lokar Searunner at Rellekka, then Captain Bentley to sail to Lunar Isle."),
                step("seal", "Keep your seal equipped", "Speak to the Oneiromancer, Meteora, Baba Yaga, and other Moonclan residents while wearing the seal of passage."),
                step("ceremony", "Gather ceremonial items", "Collect or craft each Lunar ceremonial piece using the mines, suqah materials, and local NPC instructions."),
                step("potion", "Make the waking-sleep potion", "Gather the guam, marrentill, ground tooth, and special vial ingredients; prepare the potion as instructed."),
                step("dream", "Enter the dream", "Return to the Oneiromancer with the ceremonial outfit, potion, and kindling, then enter the dream world."),
                step("tests", "Complete the dream tests", "Finish the race, dice, number, platform, and combat-free dream challenges; re-enter through the brazier if needed."),
                step("self", "Defeat Me", "Complete the final dream encounter using the provided mechanics rather than normal combat gear."),
                step("finish", "Learn Lunar spells", "Speak with the Oneiromancer after the dream to finish and unlock the Lunar spellbook."),
            ),
        ),
        guide(
            "recipe_disaster",
            inventory = listOf("Eye of newt", "Rotten tomato", "Greenman's ale", "Ashes", "Fruit blast ingredients", "Many subquest-specific items"),
            equipment = listOf("Combat gear for Culinaromancer's final gauntlet", "Ice gloves", "Food and prayer restoration"),
            teleports = listOf("Lumbridge Teleport", "Games necklace", "Fairy rings", "Multiple regional teleports for subquests"),
            steps = listOf(
                step("intro", "Another Cook's Quest", "Help the Lumbridge Cook prepare the special feast and enter the banquet room."),
                step("freeze", "Free the council", "Inspect every frozen guest to unlock their matching subquest; complete them in any practical order."),
                step("dwarf", "Free the Mountain Dwarf", "Recreate the rock cake through the Falador and White Wolf Mountain sequence."),
                step("goblin", "Free the Goblin generals", "Consult the Goblin Cook and create the correctly coloured spicy dish."),
                step("pirate", "Free Pirate Pete", "Make the stuffed snake using the diving and crab-collection sequence."),
                step("guide", "Free the Lumbridge Guide", "Complete the ingredient quiz and prepare the cake without carrying forbidden ingredients."),
                step("evil_dave", "Free Evil Dave", "Use stews and coloured spices in Evil Dave's basement until the correct spicy stew is identified."),
                step("skrach", "Free Skrach Uglogwee", "Travel through Feldip Hills and Karamja to prepare the chompy-and-jubbly dish."),
                step("awowogei", "Free King Awowogei", "Return to Ape Atoll and prepare the stuffed snake with the required nuts, banana, and red vine worm."),
                step("finale", "Defeat the Culinaromancer", "Enter the portal with food and appropriate gear, defeat all six bosses in sequence, and claim the final reward.", "Death is instanced; check the current Wiki guide before risking valuable equipment."),
            ),
        ),
        guide(
            "mm2",
            inventory = listOf("Greegree", "M'speak amulet", "Royal seed pod", "Stamina potions", "Food", "Prayer restoration"),
            equipment = listOf("Combat gear for tortured and demonic gorillas", "Ranged gear for Glough", "Lightweight gear for the platform section"),
            teleports = listOf("Royal seed pod", "Fairy ring CLR", "Spirit tree → Gnome Stronghold", "Ape Atoll teleport"),
            steps = listOf(
                step("start", "Report to King Narnode", "Speak to King Narnode, then Garkor on Ape Atoll to begin the investigation."),
                step("kruk", "Infiltrate Kruk's dungeon", "Use your greegree and complete either the agility or tank route through Kruk's dungeon."),
                step("platform", "Sabotage the airship platform", "Navigate the multi-level platform, avoid guards, and place the explosives at every marked support."),
                step("gorillas", "Investigate the laboratory", "Enter the underground laboratory, fight the required gorillas, and gather evidence of Glough's plan."),
                step("assault", "Prepare the Gnome assault", "Return to Garkor and King Narnode, then board the airship for the final attack."),
                step("glough_one", "Glough phase one", "Fight Glough in the first room, using safe distance and protection prayers."),
                step("glough_two", "Glough phases two and three", "Move through the remaining rooms, manage knockback and special attacks, and finish Glough.", "Check current safe-position mechanics before the fight; they can change."),
                step("finish", "Return to the king", "Speak to King Narnode to complete the quest and unlock demonic gorillas and the seed pod."),
            ),
        ),
        guide(
            "ds2",
            inventory = listOf("Digsite pendant", "Pickaxe", "Hammer", "Axe", "Machete", "Food", "Antifire and prayer supplies"),
            equipment = listOf("Ranged setup for Vorkath", "Dragonfire protection", "Final-fight ranged setup with Ruby/Diamond bolts recommended"),
            teleports = listOf("Digsite pendant", "Games necklace → Burthorpe", "Myths' Guild routes after completion", "House and bank teleports"),
            steps = listOf(
                step("start", "Join the Myths' Guild expedition", "Speak to Alec Kincade at the Myths' Guild, then meet Dallas Jones at the Karamja ruins."),
                step("library", "Explore the ancient library", "Solve the Lithkren vault and map puzzle with Dallas and Jardric."),
                step("fremennik", "Recruit the Fremennik", "Follow the Rellekka, Lunar Isle, and Varlamore clues to secure an experienced ship crew."),
                step("ship", "Build and launch the ship", "Gather the required materials, repair the ship at Fossil Island, and sail toward Ungael."),
                step("vorkath", "Defeat Vorkath", "Cross Ungael, use antifire and poison protection, and defeat the quest version of Vorkath.", "Learn acid, fireball, and zombified-spawn responses before entering."),
                step("dragonkin", "Discover the dragonkin plan", "Investigate the laboratory and return with the evidence to assemble the fleet."),
                step("fleet", "Defend the fleet", "Move between ships, repel dragons, repair damage, and reach the flagship."),
                step("galvek", "Defeat Galvek", "Fight through Galvek's elemental phases, avoid one-hit fireballs, and manage terrain hazards.", "This is a high-risk boss encounter; confirm current mechanics and recovery rules in the Wiki guide."),
                step("finish", "Found the Myths' Guild", "Return to the guild expedition leaders and complete the final dialogue."),
            ),
        ),
        guide(
            "sote",
            inventory = listOf("Mourner gear", "Crystal", "Pickaxe", "Axe", "Tinderbox", "Food", "Prayer restoration"),
            equipment = listOf("Magic gear for Fragment of Seren", "High-healing food and emergency teleport", "Lightweight gear for Underground Pass routes"),
            teleports = listOf("Teleport crystal → Lletya", "Ardougne teleport", "Iorwerth camp teleport", "House and bank teleports"),
            steps = listOf(
                step("start", "The elven rebellion", "Speak with Edmond in East Ardougne, then meet the rebel leaders and travel to Lletya."),
                step("clans", "Win over the elven clans", "Visit each clan representative across Tirannwn and complete their requested task or puzzle."),
                step("library", "Enter the Grand Library", "Reach the library beneath Prifddinas and solve the light-and-mirror puzzle floor by floor."),
                step("seren", "Learn Seren's history", "Complete the memory sequence and return to the rebels with the truth."),
                step("assault", "Assault the Underground Pass", "Lead the rebel attack through the pass and defeat the required Iorwerth forces."),
                step("fragment", "Defeat the Fragment of Seren", "Use Magic, manage healing around the scripted special attacks, and defeat the fragment.", "The fragment's major special can be lethal; check the current HP and healing strategy before starting."),
                step("city", "Restore Prifddinas", "Complete the final crystal sequence and conversations to restore the city."),
                step("finish", "Enter Prifddinas", "Finish the ceremony and confirm the quest completion unlocks."),
            ),
        ),
        guide(
            "sins_father",
            inventory = listOf("Ivandis flail / Blisterwood flail", "Emerald", "Knife", "Chisel", "Food", "Prayer restoration"),
            equipment = listOf("Magic-defence melee gear", "Blisterwood flail", "Emergency teleport"),
            teleports = listOf("Drakan's medallion", "Ectophial", "Mort'ton minigame teleport", "Fairy ring CKS"),
            steps = listOf(
                step("start", "Rejoin the Myreque", "Speak to Veliaf at the Icyene Graveyard and follow the Myreque plan into Darkmeyer."),
                step("disguise", "Create a Vyre disguise", "Gather the required clothing and materials, then use the disguise to move through Darkmeyer."),
                step("tasks", "Earn trust in Darkmeyer", "Complete the assigned citizen and noble tasks while maintaining the disguise."),
                step("blisterwood", "Make the Blisterwood flail", "Reach the Blisterwood tree, obtain a branch, and combine it with the flail components."),
                step("laboratory", "Investigate the laboratory", "Follow Safalaan and the Myreque through the laboratory and uncover Vanstrom's plan."),
                step("vanstrom", "Defeat Vanstrom Klause", "Use the Blisterwood flail, face away from the darkness stare, avoid blood attacks, and manage the lightning phase.", "Lightning and special attacks can kill quickly; confirm current mechanics before entering."),
                step("finish", "Return to the Myreque", "Complete the aftermath conversations and receive the final unlocks."),
            ),
        ),
        guide(
            "beneath_sands",
            inventory = listOf("Waterskins", "Knife", "Tinderbox", "Food", "Antipoison", "Prayer restoration"),
            equipment = listOf("Melee and Ranged options", "Desert heat protection", "Emergency teleport"),
            teleports = listOf("Pharaoh's sceptre", "Desert amulet", "Nardah teleport", "Pollnivneach teleport"),
            steps = listOf(
                step("start", "Meet Maisa", "Speak to Maisa near the desert camp and investigate the plague affecting Sophanem."),
                step("tomb", "Search the ancient tomb", "Enter the tomb, solve its traps and puzzles, and recover the requested evidence."),
                step("scarabas", "Track the Scarabas", "Follow the clues across the desert and report to the relevant Sophanem leaders."),
                step("champions", "Prepare the champions", "Recruit and equip the allies needed for the confrontation."),
                step("bosses", "Defeat the guardians", "Fight the quest bosses using the recommended styles, protection prayers, and antipoison."),
                step("amascut", "Confront Amascut's plan", "Complete the final tomb sequence and survive the scripted encounter.", "Check current boss mechanics before risking expensive gear."),
                step("finish", "Return to Maisa", "Complete the final conversations to unlock Tombs of Amascut access."),
            ),
        ),
        guide(
            "secrets_north",
            inventory = listOf("Lockpick", "Rope", "Food", "Prayer restoration", "Stamina potions"),
            equipment = listOf("Ranged and Magic gear", "Ancient staff or autocast option", "Emergency teleport"),
            teleports = listOf("Games necklace → Wintertodt", "Fairy ring DKS", "Rellekka / Weiss routes", "Ghorrock teleport"),
            steps = listOf(
                step("start", "Investigate the assassin", "Meet the Mahjarrat allies, examine the crime scene, and follow the northern trail."),
                step("hideout", "Search the hideout", "Use Agility, Thieving, and Hunter to navigate the hideout and collect every clue."),
                step("cipher", "Solve the evidence puzzle", "Arrange the gathered evidence and decode the message to identify the destination."),
                step("ghorrock", "Reach Ghorrock", "Travel through the frozen north and enter the prison complex with the required supplies."),
                step("muspah", "Defeat the Phantom Muspah", "Switch between Ranged and Magic as needed, manage prayer shield phases, and avoid floor hazards.", "Confirm current Muspah mechanics and death recovery before the fight."),
                step("reveal", "Uncover the conspiracy", "Complete the post-fight investigation and final confrontation."),
                step("finish", "Report back", "Finish the dialogue chain to complete the quest and unlock repeat Muspah encounters."),
            ),
        ),
    ).associateBy(QuestGuide::questId)

    fun forQuest(id: String): QuestGuide? = guides[id]

    private fun guide(
        id: String,
        inventory: List<String>,
        equipment: List<String>,
        teleports: List<String>,
        steps: List<QuestGuideStep>,
    ) = QuestGuide(id, inventory, equipment, teleports, steps)

    private fun step(
        id: String,
        title: String,
        directions: String,
        warning: String? = null,
    ) = QuestGuideStep(id, title, directions, warning)

    fun checklistId(questId: String, group: String, index: Int): String =
        "$questId:$group:$index"

    fun stepId(questId: String, stepId: String): String = "$questId:step:$stepId"
}

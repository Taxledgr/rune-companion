package io.github.taxledgr.runecompanion.features

import android.content.Context
import io.github.taxledgr.runecompanion.toolkit.HiscoreSummary
import io.github.taxledgr.runecompanion.toolkit.ActivityScore
import io.github.taxledgr.runecompanion.toolkit.SkillScore
import org.json.JSONArray
import org.json.JSONObject

class FeaturePreferences(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): FeatureData {
        val primary = preferences.getString(KEY_DATA, null)
        decodeOrNull(primary)?.let { data ->
            ensureRecoveryCopy(requireNotNull(primary))
            return data
        }

        val recovery = preferences.getString(KEY_DATA_RECOVERY, null)
        decodeOrNull(recovery)?.let { data ->
            preferences.edit().putString(KEY_DATA, recovery).apply()
            return data
        }
        return FeatureData()
    }

    fun save(data: FeatureData) {
        val encoded = encode(data).toString()
        val current = preferences.getString(KEY_DATA, null)
        preferences.edit().apply {
            if (current != null && current != encoded && decodeOrNull(current) != null) {
                putString(KEY_DATA_RECOVERY, current)
            }
            putString(KEY_DATA, encoded)
        }.apply()
    }

    private fun encode(data: FeatureData) = JSONObject().apply {
        put("schemaVersion", CURRENT_SCHEMA_VERSION)
        put("accounts", data.accounts.jsonArray { account ->
            JSONObject().apply {
                put("username", account.username)
                put("autoRefresh", account.autoRefresh)
                put("snapshots", account.snapshots.jsonArray { snapshot ->
                    JSONObject().apply {
                        put("capturedAt", snapshot.capturedAtEpochMillis)
                        put("summary", snapshot.summary.toJson())
                    }
                })
            }
        })
        put("selectedAccount", data.selectedAccount ?: JSONObject.NULL)
        put("goals", data.goals.jsonArray { it.toJson() })
        put("bankedXp", data.bankedXp.jsonArray { it.toJson() })
        put("geAlerts", data.geAlerts.jsonArray { it.toJson() })
        put("portfolio", data.portfolio.jsonArray { it.toJson() })
        put("farmPatches", data.farmPatches.jsonArray { it.toJson() })
        put("slayerCards", data.slayerCards.jsonArray { it.toJson() })
        put("sessions", data.sessions.jsonArray { it.toJson() })
        put("collectionGoals", data.collectionGoals.jsonArray { it.toJson() })
        put("routines", data.routines.jsonArray { it.toJson() })
        put("combatAchievements", data.combatAchievements.jsonArray { it.toJson() })
        put("loadouts", data.loadouts.jsonArray { it.toJson() })
        put("bossReadinessPlans", data.bossReadinessPlans.jsonArray { it.toJson() })
        put("itineraryStops", data.itineraryStops.jsonArray { it.toJson() })
        put("gearUpgrades", data.gearUpgrades.jsonArray { it.toJson() })
        put("lootLedger", data.lootLedger.jsonArray { it.toJson() })
        put("counterGoals", data.counterGoals.jsonArray { it.toJson() })
        put("completedProgressIds", JSONArray(data.completedProgressIds.toList()))
        put("supplyLocker", data.supplyLocker.jsonArray { it.toJson() })
        put("wildernessRisk", data.wildernessRisk.jsonArray { it.toJson() })
        put("customTeleports", data.customTeleports.jsonArray { it.toJson() })
        put("teleportProfile", JSONObject().apply {
            put("spellbooks", JSONArray(data.teleportProfile.spellbooks.map { it.name }))
            put("capabilities", JSONArray(data.teleportProfile.capabilities.toList()))
            put("pohLocation", data.teleportProfile.pohLocation)
            put("pohDestinations", JSONArray(data.teleportProfile.pohDestinations.toList()))
        })
    }

    private fun decode(root: JSONObject): FeatureData {
        val schemaVersion = root.optInt("schemaVersion", 1)
        require(schemaVersion in 1..CURRENT_SCHEMA_VERSION) {
            "Unsupported Rune Companion data version $schemaVersion"
        }
        val accounts = root.array("accounts") { item ->
            AccountProfile(
                username = item.optString("username"),
                autoRefresh = item.optBoolean("autoRefresh", true),
                snapshots = item.array("snapshots") { snapshot ->
                    StatSnapshot(
                        capturedAtEpochMillis = snapshot.optLong("capturedAt"),
                        summary = snapshot.optJSONObject("summary").toSummary(),
                    )
                },
            )
        }
        val teleport = root.optJSONObject("teleportProfile")
        return FeatureData(
            accounts = accounts,
            selectedAccount = root.optStringOrNull("selectedAccount"),
            goals = root.array("goals") { it.toGoal() },
            bankedXp = root.array("bankedXp") { it.toBankedXp() },
            geAlerts = root.array("geAlerts") { it.toGeAlert() },
            portfolio = root.array("portfolio") { it.toPortfolio() },
            farmPatches = root.array("farmPatches") { it.toFarmPatch() },
            slayerCards = root.array("slayerCards") { it.toSlayerCard() },
            sessions = root.array("sessions") { it.toSession() },
            collectionGoals = root.array("collectionGoals") { it.toCollectionGoal() },
            routines = root.array("routines") { it.toRoutine() },
            combatAchievements = root.array("combatAchievements") { it.toCombatAchievement() },
            loadouts = root.array("loadouts") { it.toLoadout() },
            bossReadinessPlans = root.array("bossReadinessPlans") { it.toBossReadinessPlan() },
            itineraryStops = root.array("itineraryStops") { it.toItineraryStop() },
            gearUpgrades = root.array("gearUpgrades") { it.toGearUpgrade() },
            lootLedger = root.array("lootLedger") { it.toLootLedgerEntry() },
            counterGoals = root.array("counterGoals") { it.toCounterGoal() },
            completedProgressIds = root.stringSet("completedProgressIds"),
            supplyLocker = root.array("supplyLocker") { it.toSupplyLockerItem() },
            wildernessRisk = root.array("wildernessRisk") { it.toWildernessRiskItem() },
            customTeleports = root.array("customTeleports") { it.toCustomTeleport() },
            teleportProfile = TeleportCapabilityProfile(
                spellbooks = teleport?.stringSet("spellbooks")
                    ?.mapNotNull { name -> Spellbook.entries.firstOrNull { it.name == name } }
                    ?.toSet()
                    ?.ifEmpty { setOf(Spellbook.STANDARD) }
                    ?: setOf(Spellbook.STANDARD),
                capabilities = teleport?.stringSet("capabilities")
                    ?: setOf("home_lumbridge"),
                pohLocation = teleport?.optString("pohLocation")
                    ?.ifBlank { "Rimmington" }
                    ?: "Rimmington",
                pohDestinations = teleport?.stringSet("pohDestinations") ?: emptySet(),
            ),
        )
    }

    private fun decodeOrNull(raw: String?): FeatureData? =
        raw?.let { runCatching { decode(JSONObject(it)) }.getOrNull() }

    private fun ensureRecoveryCopy(primary: String) {
        if (preferences.getString(KEY_DATA_RECOVERY, null) == null) {
            preferences.edit().putString(KEY_DATA_RECOVERY, primary).apply()
        }
    }

    private fun HiscoreSummary.toJson() = JSONObject().apply {
        put("player", player)
        put("skills", skills.jsonArray { skill ->
            JSONObject()
                .put("name", skill.name)
                .put("rank", skill.rank)
                .put("level", skill.level)
                .put("xp", skill.xp)
        })
        put("activities", activities.jsonArray { activity ->
            JSONObject()
                .put("name", activity.name)
                .put("rank", activity.rank)
                .put("score", activity.score)
        })
    }

    private fun JSONObject?.toSummary(): HiscoreSummary {
        if (this == null) return HiscoreSummary("", emptyList())
        return HiscoreSummary(
            player = optString("player"),
            skills = array("skills") { skill ->
                SkillScore(
                    name = skill.optString("name"),
                    rank = skill.optInt("rank", -1),
                    level = skill.optInt("level", 1),
                    xp = skill.optLong("xp"),
                )
            },
            activities = array("activities") { activity ->
                ActivityScore(
                    name = activity.optString("name"),
                    rank = activity.optInt("rank", -1),
                    score = activity.optLong("score"),
                )
            },
        )
    }

    private fun SkillGoal.toJson() = JSONObject().put("id", id).put("skill", skill)
        .put("targetLevel", targetLevel).put("xpPerAction", xpPerAction)
    private fun JSONObject.toGoal() = SkillGoal(
        optString("id"), optString("skill"), optInt("targetLevel"), optDouble("xpPerAction"),
    )

    private fun BankedXpEntry.toJson() = JSONObject().put("id", id).put("item", item)
        .put("skill", skill).put("quantity", quantity).put("xpEach", xpEach)
    private fun JSONObject.toBankedXp() = BankedXpEntry(
        optString("id"), optString("item"), optString("skill"), optInt("quantity"), optDouble("xpEach"),
    )

    private fun GeAlert.toJson() = JSONObject().put("id", id).put("itemId", itemId)
        .put("itemName", itemName).put("targetPrice", targetPrice)
        .put("alertWhenAbove", alertWhenAbove).put("latestPrice", latestPrice ?: JSONObject.NULL)
        .put("targetWasMet", targetWasMet)
    private fun JSONObject.toGeAlert() = GeAlert(
        optString("id"), optInt("itemId"), optString("itemName"), optLong("targetPrice"),
        optBoolean("alertWhenAbove"), optLongOrNull("latestPrice"), optBoolean("targetWasMet"),
    )

    private fun PortfolioEntry.toJson() = JSONObject().put("id", id).put("itemId", itemId)
        .put("itemName", itemName).put("quantity", quantity).put("buyPrice", buyPrice)
        .put("latestPrice", latestPrice ?: JSONObject.NULL)
    private fun JSONObject.toPortfolio() = PortfolioEntry(
        optString("id"), optInt("itemId"), optString("itemName"), optInt("quantity"),
        optLong("buyPrice"), optLongOrNull("latestPrice"),
    )

    private fun FarmPatch.toJson() = JSONObject().put("id", id).put("patch", patch)
        .put("crop", crop).put("readyAt", readyAtEpochMillis).put("note", note)
    private fun JSONObject.toFarmPatch() = FarmPatch(
        optString("id"), optString("patch"), optString("crop"), optLong("readyAt"), optString("note"),
    )

    private fun SlayerCard.toJson() = JSONObject().put("id", id).put("monster", monster)
        .put("weakness", weakness).put("locations", locations)
        .put("requiredItems", requiredItems).put("notes", notes)
    private fun JSONObject.toSlayerCard() = SlayerCard(
        optString("id"), optString("monster"), optString("weakness"),
        optString("locations"), optString("requiredItems"), optString("notes"),
    )

    private fun ActivitySession.toJson() = JSONObject().put("id", id).put("activity", activity)
        .put("kills", kills).put("durationMinutes", durationMinutes)
        .put("lootValue", lootValue).put("supplyCost", supplyCost).put("createdAt", createdAtEpochMillis)
    private fun JSONObject.toSession() = ActivitySession(
        optString("id"), optString("activity"), optInt("kills"), optInt("durationMinutes"),
        optLong("lootValue"), optLong("supplyCost"), optLong("createdAt"),
    )

    private fun CollectionGoal.toJson() = JSONObject().put("id", id).put("item", item)
        .put("denominator", dropRateDenominator).put("attempts", attempts).put("obtained", obtained)
        .put("sourceActivity", sourceActivity ?: JSONObject.NULL)
    private fun JSONObject.toCollectionGoal() = CollectionGoal(
        optString("id"), optString("item"), optInt("denominator"), optInt("attempts"),
        optBoolean("obtained"), optStringOrNull("sourceActivity"),
    )

    private fun Routine.toJson() = JSONObject().put("id", id).put("title", title)
        .put("weekly", weekly).put("lastCompleted", lastCompletedEpochDay ?: JSONObject.NULL)
        .put("streak", streak)
    private fun JSONObject.toRoutine() = Routine(
        optString("id"), optString("title"), optBoolean("weekly"),
        optLongOrNull("lastCompleted"), optInt("streak"),
    )

    private fun CombatAchievementPlan.toJson() = JSONObject().put("id", id).put("task", task)
        .put("tier", tier).put("completed", completed)
    private fun JSONObject.toCombatAchievement() = CombatAchievementPlan(
        optString("id"), optString("task"), optString("tier"), optBoolean("completed"),
    )

    private fun LoadoutTemplate.toJson() = JSONObject().put("id", id).put("name", name)
        .put("inventory", inventory).put("equipment", equipment).put("notes", notes)
    private fun JSONObject.toLoadout() = LoadoutTemplate(
        optString("id"), optString("name"), optString("inventory"),
        optString("equipment"), optString("notes"),
    )

    private fun BossReadinessPlan.toJson() = JSONObject()
        .put("id", id)
        .put("bossId", bossId)
        .put("confirmedChecks", JSONArray(confirmedChecks.toList()))
        .put("notes", notes)
    private fun JSONObject.toBossReadinessPlan() = BossReadinessPlan(
        optString("id"),
        optString("bossId"),
        stringSet("confirmedChecks"),
        optString("notes"),
    )

    private fun ItineraryStop.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("region", region)
        .put("teleport", teleport)
        .put("completed", completed)
    private fun JSONObject.toItineraryStop() = ItineraryStop(
        optString("id"),
        optString("title"),
        optString("region"),
        optString("teleport"),
        optBoolean("completed"),
    )

    private fun GearUpgradePlan.toJson() = JSONObject()
        .put("id", id)
        .put("style", style)
        .put("currentItem", currentItem)
        .put("targetItemId", targetItemId)
        .put("targetItemName", targetItemName)
        .put("targetPrice", targetPrice ?: JSONObject.NULL)
        .put("budget", budget)
        .put("benefit", benefit)
        .put("obtained", obtained)
    private fun JSONObject.toGearUpgrade() = GearUpgradePlan(
        optString("id"),
        optString("style"),
        optString("currentItem"),
        optInt("targetItemId"),
        optString("targetItemName"),
        optLongOrNull("targetPrice"),
        optLong("budget"),
        optString("benefit"),
        optBoolean("obtained"),
    )

    private fun LootLedgerEntry.toJson() = JSONObject()
        .put("id", id)
        .put("activity", activity)
        .put("itemId", itemId)
        .put("itemName", itemName)
        .put("quantity", quantity)
        .put("unitValue", unitValue)
        .put("createdAt", createdAtEpochMillis)
    private fun JSONObject.toLootLedgerEntry() = LootLedgerEntry(
        optString("id"),
        optString("activity"),
        optInt("itemId"),
        optString("itemName"),
        optInt("quantity"),
        optLong("unitValue"),
        optLong("createdAt"),
    )

    private fun PublicCounterGoal.toJson() = JSONObject()
        .put("id", id)
        .put("account", account)
        .put("activity", activity)
        .put("startValue", startValue)
        .put("targetValue", targetValue)
    private fun JSONObject.toCounterGoal() = PublicCounterGoal(
        optString("id"),
        optString("account"),
        optString("activity"),
        optLong("startValue"),
        optLong("targetValue"),
    )

    private fun SupplyLockerItem.toJson() = JSONObject()
        .put("id", id)
        .put("itemId", itemId)
        .put("itemName", itemName)
        .put("quantity", quantity)
        .put("lowAt", lowAt)
        .put("unitValue", unitValue)
    private fun JSONObject.toSupplyLockerItem() = SupplyLockerItem(
        optString("id"),
        optInt("itemId"),
        optString("itemName"),
        optInt("quantity"),
        optInt("lowAt"),
        optLong("unitValue"),
    )

    private fun WildernessRiskItem.toJson() = JSONObject()
        .put("id", id)
        .put("itemId", itemId)
        .put("itemName", itemName)
        .put("quantity", quantity)
        .put("unitValue", unitValue)
        .put("protected", protected)
    private fun JSONObject.toWildernessRiskItem() = WildernessRiskItem(
        optString("id"),
        optInt("itemId"),
        optString("itemName"),
        optInt("quantity"),
        optLong("unitValue"),
        optBoolean("protected"),
    )

    private fun CustomTeleport.toJson() = JSONObject().put("id", id).put("name", name)
        .put("destination", destination).put("region", region).put("dangerous", dangerous)
    private fun JSONObject.toCustomTeleport() = CustomTeleport(
        optString("id"), optString("name"), optString("destination"),
        optString("region"), optBoolean("dangerous"),
    )

    private fun <T> List<T>.jsonArray(block: (T) -> JSONObject) =
        JSONArray().also { array -> forEach { array.put(block(it)) } }

    private fun <T> JSONObject.array(key: String, block: (JSONObject) -> T): List<T> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                runCatching { block(array.getJSONObject(index)) }.getOrNull()?.let(::add)
            }
        }
    }

    private fun JSONObject.stringSet(key: String): Set<String> {
        val array = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (index in 0 until array.length()) add(array.optString(index))
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    companion object {
        const val PREFERENCES_NAME = "rune_companion_features"
        private const val KEY_DATA = "feature_data"
        private const val KEY_DATA_RECOVERY = "feature_data_recovery"
        private const val CURRENT_SCHEMA_VERSION = 3
    }
}

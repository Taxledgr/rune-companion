package io.github.taxledgr.runecompanion.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.taxledgr.runecompanion.toolkit.HiscoreClient
import io.github.taxledgr.runecompanion.toolkit.PriceClient
import io.github.taxledgr.runecompanion.toolkit.PriceSearchItem
import io.github.taxledgr.runecompanion.toolkit.PriceWatchItem
import io.github.taxledgr.runecompanion.toolkit.ToolkitPreferences
import io.github.taxledgr.runecompanion.widget.RuneCompanionWidget
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeatureViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = FeaturePreferences(application)
    private val toolkitPreferences = ToolkitPreferences(application)
    private val backupManager = AppBackupManager(application)
    private val hiscoreClient = HiscoreClient()
    private val priceClient = PriceClient()
    private val _state = MutableStateFlow(FeatureState(initializing = true))
    val state: StateFlow<FeatureState> = _state.asStateFlow()
    private val persistenceQueue = Channel<FeatureData>(Channel.CONFLATED)
    private var foregroundRefreshJob: Job? = null
    private var diskReloadJob: Job? = null
    private var priceRefreshJob: Job? = null
    private var priceSearchJob: Job? = null
    private var latestPriceQuery = ""
    private val refreshingAccounts = mutableSetOf<String>()
    private val pendingMutations = mutableListOf<(FeatureData) -> FeatureData>()
    private var appInForeground = false
    private var initialized = false
    private var dataVersion = 0L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (data in persistenceQueue) {
                preferences.save(data)
                RuneCompanionWidget.updateAll(getApplication())
            }
        }
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { loadPersistedData() }
            val data = pendingMutations.fold(loaded) { current, mutation ->
                mutation(current)
            }
            pendingMutations.clear()
            _state.update { it.copy(data = data, initializing = false) }
            initialized = true
            if (data != loaded) persistenceQueue.trySend(data)
            syncSchedulers(data)
            if (appInForeground) startForegroundRefresh()
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        if (appInForeground == inForeground) return
        appInForeground = inForeground
        foregroundRefreshJob?.cancel()
        foregroundRefreshJob = null
        if (!inForeground || !initialized) return
        reloadFromDisk()
    }

    private fun startForegroundRefresh() {
        if (!appInForeground || !initialized || foregroundRefreshJob?.isActive == true) return
        refreshStaleAccounts()
        refreshPrices()
        foregroundRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(FOREGROUND_REFRESH_MILLIS)
                refreshStaleAccounts()
                refreshPrices()
            }
        }
    }

    fun addAccount(username: String) {
        val clean = username.trim().take(12)
        if (clean.isBlank()) return
        val current = _state.value.data
        if (current.accounts.any { it.username.equals(clean, true) }) {
            selectAccount(clean)
            refreshAccount(clean)
            return
        }
        updateData {
            it.copy(
                accounts = it.accounts + AccountProfile(clean),
                selectedAccount = clean,
            )
        }
        FeatureRefreshScheduler.sync(getApplication(), enabled = true)
        refreshAccount(clean)
    }

    fun removeAccount(username: String) {
        updateData { data ->
            val accounts = data.accounts.filterNot { it.username.equals(username, true) }
            data.copy(
                accounts = accounts,
                selectedAccount = if (data.selectedAccount.equals(username, true)) {
                    accounts.firstOrNull()?.username
                } else {
                    data.selectedAccount
                },
            )
        }
        FeatureRefreshScheduler.sync(
            getApplication(),
            _state.value.data.accounts.any { it.autoRefresh },
        )
    }

    fun selectAccount(username: String) =
        updateData { it.copy(selectedAccount = username) }

    fun refreshAllAccounts() {
        _state.value.data.accounts.filter { it.autoRefresh }.forEach {
            refreshAccount(it.username)
        }
    }

    private fun refreshStaleAccounts() {
        val now = System.currentTimeMillis()
        _state.value.data.accounts
            .filter { it.needsRefresh(now, FOREGROUND_REFRESH_MILLIS) }
            .forEach { refreshAccount(it.username) }
    }

    fun refreshAccount(username: String) {
        val refreshKey = username.trim().lowercase()
        if (refreshKey.isBlank() || !refreshingAccounts.add(refreshKey)) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                runCatching { hiscoreClient.lookup(username) }
                    .onSuccess { summary ->
                        val snapshot = StatSnapshot(System.currentTimeMillis(), summary)
                        updateData { data ->
                            data.copy(accounts = data.accounts.map { account ->
                                if (!account.username.equals(username, true)) {
                                    account
                                } else {
                                    account.copy(
                                        snapshots = compactSnapshots(
                                            account.snapshots + snapshot,
                                        ),
                                    )
                                }
                            })
                        }
                        _state.update {
                            it.copy(message = "Updated ${summary.player}")
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                message = error.message ?: "Could not refresh hiscores",
                            )
                        }
                    }
            } finally {
                refreshingAccounts.remove(refreshKey)
                _state.update { it.copy(loading = refreshingAccounts.isNotEmpty()) }
            }
        }
    }

    fun addGoal(skill: String, target: Int, xpPerAction: Double) {
        if (skill.isBlank() || target !in 2..126 || xpPerAction <= 0) return
        updateData {
            it.copy(goals = it.goals + SkillGoal(id(), skill.trim(), target, xpPerAction))
        }
    }

    fun deleteGoal(id: String) = updateData {
        it.copy(goals = it.goals.filterNot { item -> item.id == id })
    }

    fun addBankedXp(item: String, skill: String, quantity: Int, xpEach: Double) {
        if (item.isBlank() || skill.isBlank() || quantity <= 0 || xpEach <= 0) return
        updateData {
            it.copy(
                bankedXp = it.bankedXp + BankedXpEntry(
                    id(), item.trim(), skill.trim(), quantity, xpEach,
                ),
            )
        }
    }

    fun deleteBankedXp(id: String) = updateData {
        it.copy(bankedXp = it.bankedXp.filterNot { item -> item.id == id })
    }

    fun searchPrices(query: String) {
        val cleanQuery = query.trim()
        latestPriceQuery = cleanQuery
        priceSearchJob?.cancel()
        if (cleanQuery.length < 2) {
            _state.update { it.copy(priceSearchResults = emptyList()) }
            return
        }
        priceSearchJob = viewModelScope.launch {
            delay(PRICE_SEARCH_DEBOUNCE_MS)
            runCatching { priceClient.search(cleanQuery) }
                .onSuccess { results ->
                    if (cleanQuery != latestPriceQuery) return@onSuccess
                    _state.update { it.copy(priceSearchResults = results) }
                }
                .onFailure { error ->
                    if (cleanQuery != latestPriceQuery) return@onFailure
                    _state.update {
                        it.copy(message = error.message ?: "Price search failed")
                    }
                }
        }
    }

    fun addGeAlert(item: PriceSearchItem, targetPrice: Long, above: Boolean) {
        if (targetPrice <= 0) return
        updateData {
            it.copy(
                geAlerts = it.geAlerts + GeAlert(
                    id(), item.id, item.name, targetPrice, above,
                ),
            )
        }
        _state.update { it.copy(priceSearchResults = emptyList()) }
        GeAlertScheduler.sync(getApplication(), enabled = true)
        refreshPrices()
    }

    fun deleteGeAlert(id: String) {
        updateData {
            it.copy(geAlerts = it.geAlerts.filterNot { item -> item.id == id })
        }
        GeAlertScheduler.sync(getApplication(), _state.value.data.geAlerts.isNotEmpty())
    }

    fun addPortfolio(item: PriceSearchItem, quantity: Int, buyPrice: Long) {
        if (quantity <= 0 || buyPrice <= 0) return
        updateData {
            it.copy(
                portfolio = it.portfolio + PortfolioEntry(
                    id(), item.id, item.name, quantity, buyPrice,
                ),
            )
        }
        _state.update { it.copy(priceSearchResults = emptyList()) }
        refreshPrices()
    }

    fun deletePortfolio(id: String) = updateData {
        it.copy(portfolio = it.portfolio.filterNot { item -> item.id == id })
    }

    fun refreshPrices() {
        val data = _state.value.data
        val items = (data.geAlerts.map { PriceWatchItem(it.itemId, it.itemName) } +
            data.portfolio.map { PriceWatchItem(it.itemId, it.itemName) })
            .distinctBy { it.id }
        if (items.isEmpty() || priceRefreshJob?.isActive == true) return
        priceRefreshJob = viewModelScope.launch {
            runCatching { priceClient.latest(items) }
                .onSuccess { latest ->
                    val byId = latest.associateBy { it.id }
                    updateData {
                        it.copy(
                            geAlerts = it.geAlerts.map { alert ->
                                val price = byId[alert.itemId]?.high
                                    ?: byId[alert.itemId]?.low
                                alert.copy(
                                    latestPrice = price,
                                    targetWasMet = price?.let { current ->
                                        if (alert.alertWhenAbove) current >= alert.targetPrice
                                        else current <= alert.targetPrice
                                    } ?: false,
                                )
                            },
                            portfolio = it.portfolio.map { entry ->
                                entry.copy(
                                    latestPrice = byId[entry.itemId]?.high
                                        ?: byId[entry.itemId]?.low,
                                )
                            },
                        )
                    }
                }
        }
    }

    fun addFarmPatch(patch: String, crop: String, minutes: Int, note: String) {
        if (patch.isBlank() || crop.isBlank() || minutes <= 0) return
        updateData {
            it.copy(
                farmPatches = it.farmPatches + FarmPatch(
                    id(), patch.trim(), crop.trim(),
                    System.currentTimeMillis() + minutes * 60_000L, note.trim(),
                ),
            )
        }
    }

    fun deleteFarmPatch(id: String) = updateData {
        it.copy(farmPatches = it.farmPatches.filterNot { item -> item.id == id })
    }

    fun addSlayerCard(
        monster: String,
        weakness: String,
        locations: String,
        requiredItems: String,
        notes: String,
    ) {
        if (monster.isBlank()) return
        updateData {
            it.copy(
                slayerCards = it.slayerCards + SlayerCard(
                    id(), monster.trim(), weakness.trim(), locations.trim(),
                    requiredItems.trim(), notes.trim(),
                ),
            )
        }
    }

    fun deleteSlayerCard(id: String) = updateData {
        it.copy(slayerCards = it.slayerCards.filterNot { item -> item.id == id })
    }

    fun addSession(
        activity: String,
        kills: Int,
        minutes: Int,
        loot: Long,
        supplies: Long,
    ) {
        if (activity.isBlank() || kills < 0 || minutes <= 0) return
        updateData {
            it.copy(
                sessions = it.sessions + ActivitySession(
                    id(), activity.trim(), kills, minutes, loot, supplies,
                    System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteSession(id: String) = updateData {
        it.copy(sessions = it.sessions.filterNot { item -> item.id == id })
    }

    fun addCollectionGoal(item: String, denominator: Int, sourceActivity: String? = null) {
        if (item.isBlank() || denominator <= 0) return
        updateData {
            it.copy(
                collectionGoals = it.collectionGoals +
                    CollectionGoal(
                        id(), item.trim(), denominator, 0, false,
                        sourceActivity?.trim()?.takeIf { value -> value.isNotBlank() },
                    ),
            )
        }
    }

    fun adjustCollection(id: String, amount: Int) = updateData {
        it.copy(collectionGoals = it.collectionGoals.map { goal ->
            if (goal.id == id) goal.copy(attempts = (goal.attempts + amount).coerceAtLeast(0))
            else goal
        })
    }

    fun setCollectionAttempts(id: String, attempts: Int) = updateData {
        it.copy(collectionGoals = it.collectionGoals.map { goal ->
            if (goal.id == id) goal.copy(attempts = attempts.coerceAtLeast(0)) else goal
        })
    }

    fun toggleCollectionObtained(id: String) = updateData {
        it.copy(collectionGoals = it.collectionGoals.map { goal ->
            if (goal.id == id) goal.copy(obtained = !goal.obtained) else goal
        })
    }

    fun deleteCollectionGoal(id: String) = updateData {
        it.copy(collectionGoals = it.collectionGoals.filterNot { item -> item.id == id })
    }

    fun addRoutine(title: String, weekly: Boolean) {
        if (title.isBlank()) return
        updateData {
            it.copy(routines = it.routines + Routine(id(), title.trim(), weekly))
        }
    }

    fun completeRoutine(id: String) {
        val today = LocalDate.now().toEpochDay()
        updateData {
            it.copy(routines = it.routines.map { routine ->
                if (routine.id != id || routine.lastCompletedEpochDay == today) {
                    routine
                } else {
                    val interval = if (routine.weekly) 7 else 1
                    val continued = routine.lastCompletedEpochDay?.let {
                        today - it <= interval
                    } ?: false
                    routine.copy(
                        lastCompletedEpochDay = today,
                        streak = if (continued) routine.streak + 1 else 1,
                    )
                }
            })
        }
    }

    fun deleteRoutine(id: String) = updateData {
        it.copy(routines = it.routines.filterNot { item -> item.id == id })
    }

    fun addCombatAchievement(task: String, tier: String) {
        if (task.isBlank()) return
        updateData {
            it.copy(
                combatAchievements = it.combatAchievements +
                    CombatAchievementPlan(id(), task.trim(), tier.trim(), false),
            )
        }
    }

    fun toggleCombatAchievement(id: String) = updateData {
        it.copy(combatAchievements = it.combatAchievements.map { task ->
            if (task.id == id) task.copy(completed = !task.completed) else task
        })
    }

    fun deleteCombatAchievement(id: String) = updateData {
        it.copy(combatAchievements = it.combatAchievements.filterNot { item -> item.id == id })
    }

    fun addLoadout(name: String, inventory: String, equipment: String, notes: String) {
        if (name.isBlank()) return
        updateData {
            it.copy(
                loadouts = it.loadouts + LoadoutTemplate(
                    id(), name.trim(), inventory.trim(), equipment.trim(), notes.trim(),
                ),
            )
        }
    }

    fun deleteLoadout(id: String) = updateData {
        it.copy(loadouts = it.loadouts.filterNot { item -> item.id == id })
    }

    fun saveBossReadinessPlan(bossId: String, notes: String) {
        if (bossId.isBlank()) return
        updateData { data ->
            val existing = data.bossReadinessPlans.firstOrNull { it.bossId == bossId }
            data.copy(
                bossReadinessPlans = if (existing == null) {
                    data.bossReadinessPlans + BossReadinessPlan(
                        id = id(),
                        bossId = bossId,
                        notes = notes.trim(),
                    )
                } else {
                    data.bossReadinessPlans.map {
                        if (it.id == existing.id) it.copy(notes = notes.trim()) else it
                    }
                },
            )
        }
    }

    fun toggleBossReadinessCheck(planId: String, check: String) = updateData { data ->
        data.copy(
            bossReadinessPlans = data.bossReadinessPlans.map { plan ->
                if (plan.id != planId) return@map plan
                val checks = if (check in plan.confirmedChecks) {
                    plan.confirmedChecks - check
                } else {
                    plan.confirmedChecks + check
                }
                plan.copy(confirmedChecks = checks)
            },
        )
    }

    fun toggleBossReadinessCheckForBoss(bossId: String, check: String) =
        updateData { data ->
            val existing = data.bossReadinessPlans.firstOrNull { it.bossId == bossId }
            data.copy(
                bossReadinessPlans = if (existing == null) {
                    data.bossReadinessPlans + BossReadinessPlan(
                        id = id(),
                        bossId = bossId,
                        confirmedChecks = setOf(check),
                    )
                } else {
                    data.bossReadinessPlans.map { plan ->
                        if (plan.id != existing.id) return@map plan
                        val checks = if (check in plan.confirmedChecks) {
                            plan.confirmedChecks - check
                        } else {
                            plan.confirmedChecks + check
                        }
                        plan.copy(confirmedChecks = checks)
                    }
                },
            )
        }

    fun deleteBossReadinessPlan(id: String) = updateData {
        it.copy(bossReadinessPlans = it.bossReadinessPlans.filterNot { plan -> plan.id == id })
    }

    fun addItineraryStop(title: String, region: String, teleport: String) {
        if (title.isBlank()) return
        updateData { data ->
            val resolvedRegion = region.trim().ifBlank { ExpansionCatalog.guessRegion(title) }
            data.copy(
                itineraryStops = data.itineraryStops + ItineraryStop(
                    id = id(),
                    title = title.trim(),
                    region = resolvedRegion,
                    teleport = teleport.trim().ifBlank { bestConfiguredTeleport(data, resolvedRegion) },
                ),
            )
        }
    }

    fun buildItineraryFromSaved() = updateData { data ->
        val generated = buildList {
            data.farmPatches.forEach { patch ->
                val region = ExpansionCatalog.guessRegion(patch.patch)
                add(
                    ItineraryStop(
                        id = id(),
                        title = "${patch.patch} • ${patch.crop}",
                        region = region,
                        teleport = bestConfiguredTeleport(data, region),
                    ),
                )
            }
            data.routines.forEach { routine ->
                val region = ExpansionCatalog.guessRegion(routine.title)
                add(
                    ItineraryStop(
                        id = id(),
                        title = routine.title,
                        region = region,
                        teleport = bestConfiguredTeleport(data, region),
                    ),
                )
            }
        }
        val existingNames = data.itineraryStops.map { it.title.lowercase() }.toSet()
        data.copy(
            itineraryStops = optimiseStops(
                data.itineraryStops + generated.filter { it.title.lowercase() !in existingNames },
            ),
        )
    }

    fun optimiseItinerary() = updateData {
        it.copy(itineraryStops = optimiseStops(it.itineraryStops))
    }

    fun toggleItineraryStop(id: String) = updateData { data ->
        data.copy(
            itineraryStops = data.itineraryStops.map {
                if (it.id == id) it.copy(completed = !it.completed) else it
            },
        )
    }

    fun deleteItineraryStop(id: String) = updateData {
        it.copy(itineraryStops = it.itineraryStops.filterNot { stop -> stop.id == id })
    }

    fun addGearUpgrade(
        style: String,
        currentItem: String,
        targetItem: PriceSearchItem,
        budget: Long,
        benefit: String,
    ) {
        viewModelScope.launch {
            val price = latestUnitValue(targetItem)
            updateData {
                it.copy(
                    gearUpgrades = it.gearUpgrades + GearUpgradePlan(
                        id = id(),
                        style = style.trim().ifBlank { "Any style" },
                        currentItem = currentItem.trim(),
                        targetItemId = targetItem.id,
                        targetItemName = targetItem.name,
                        targetPrice = price,
                        budget = budget.coerceAtLeast(0),
                        benefit = benefit.trim(),
                    ),
                )
            }
            _state.update { it.copy(priceSearchResults = emptyList()) }
        }
    }

    fun toggleGearUpgradeObtained(id: String) = updateData { data ->
        data.copy(
            gearUpgrades = data.gearUpgrades.map {
                if (it.id == id) it.copy(obtained = !it.obtained) else it
            },
        )
    }

    fun deleteGearUpgrade(id: String) = updateData {
        it.copy(gearUpgrades = it.gearUpgrades.filterNot { plan -> plan.id == id })
    }

    fun addLootLedgerEntry(activity: String, item: PriceSearchItem, quantity: Int) {
        if (activity.isBlank() || quantity <= 0) return
        viewModelScope.launch {
            val value = latestUnitValue(item) ?: 0
            updateData {
                it.copy(
                    lootLedger = it.lootLedger + LootLedgerEntry(
                        id = id(),
                        activity = activity.trim(),
                        itemId = item.id,
                        itemName = item.name,
                        quantity = quantity,
                        unitValue = value,
                        createdAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
            _state.update { it.copy(priceSearchResults = emptyList()) }
        }
    }

    fun deleteLootLedgerEntry(id: String) = updateData {
        it.copy(lootLedger = it.lootLedger.filterNot { entry -> entry.id == id })
    }

    fun addCounterGoal(activity: String, targetValue: Long) {
        val data = _state.value.data
        val profile = data.accounts.firstOrNull {
            it.username.equals(data.selectedAccount, ignoreCase = true)
        } ?: return
        val current = profile.latest?.summary?.activities
            ?.firstOrNull { it.name.equals(activity, ignoreCase = true) }
            ?.score
            ?.coerceAtLeast(0)
            ?: return
        if (targetValue <= current) return
        updateData {
            it.copy(
                counterGoals = it.counterGoals + PublicCounterGoal(
                    id = id(),
                    account = profile.username,
                    activity = activity,
                    startValue = current,
                    targetValue = targetValue,
                ),
            )
        }
    }

    fun deleteCounterGoal(id: String) = updateData {
        it.copy(counterGoals = it.counterGoals.filterNot { goal -> goal.id == id })
    }

    fun toggleProgress(id: String) = updateData { data ->
        val completed = if (id in data.completedProgressIds) {
            data.completedProgressIds - id
        } else {
            data.completedProgressIds + id
        }
        data.copy(completedProgressIds = completed)
    }

    fun addSupplyLockerItem(
        item: PriceSearchItem,
        quantity: Int,
        lowAt: Int,
    ) {
        if (quantity < 0 || lowAt < 0) return
        viewModelScope.launch {
            val value = latestUnitValue(item) ?: 0
            updateData { data ->
                val existing = data.supplyLocker.firstOrNull { it.itemId == item.id }
                data.copy(
                    supplyLocker = if (existing == null) {
                        data.supplyLocker + SupplyLockerItem(
                            id(), item.id, item.name, quantity, lowAt, value,
                        )
                    } else {
                        data.supplyLocker.map {
                            if (it.id == existing.id) {
                                it.copy(quantity = quantity, lowAt = lowAt, unitValue = value)
                            } else {
                                it
                            }
                        }
                    },
                )
            }
            _state.update { it.copy(priceSearchResults = emptyList()) }
        }
    }

    fun adjustSupplyLockerItem(id: String, amount: Int) = updateData { data ->
        data.copy(
            supplyLocker = data.supplyLocker.map {
                if (it.id == id) it.copy(quantity = (it.quantity + amount).coerceAtLeast(0)) else it
            },
        )
    }

    fun deleteSupplyLockerItem(id: String) = updateData {
        it.copy(supplyLocker = it.supplyLocker.filterNot { item -> item.id == id })
    }

    fun loadMarketHistory(item: PriceSearchItem, timestep: String = "24h") {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    marketItem = item,
                    marketHistory = emptyList(),
                    marketHistoryLoading = true,
                    message = null,
                    priceSearchResults = emptyList(),
                )
            }
            runCatching { priceClient.history(item.id, timestep) }
                .onSuccess { history ->
                    _state.update {
                        it.copy(marketHistory = history, marketHistoryLoading = false)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            marketHistoryLoading = false,
                            message = error.message ?: "Price history unavailable",
                        )
                    }
                }
        }
    }

    fun addWildernessRiskItem(
        item: PriceSearchItem,
        quantity: Int,
        protected: Boolean,
    ) {
        if (quantity <= 0) return
        viewModelScope.launch {
            val value = latestUnitValue(item) ?: 0
            updateData {
                it.copy(
                    wildernessRisk = it.wildernessRisk + WildernessRiskItem(
                        id(), item.id, item.name, quantity, value, protected,
                    ),
                )
            }
            _state.update { it.copy(priceSearchResults = emptyList()) }
        }
    }

    fun toggleWildernessRiskProtected(id: String) = updateData { data ->
        data.copy(
            wildernessRisk = data.wildernessRisk.map {
                if (it.id == id) it.copy(protected = !it.protected) else it
            },
        )
    }

    fun deleteWildernessRiskItem(id: String) = updateData {
        it.copy(wildernessRisk = it.wildernessRisk.filterNot { item -> item.id == id })
    }

    fun addCustomTeleport(
        name: String,
        destination: String,
        region: String,
        dangerous: Boolean,
    ) {
        if (name.isBlank() || destination.isBlank()) return
        updateData {
            it.copy(
                customTeleports = it.customTeleports + CustomTeleport(
                    id(),
                    name.trim(),
                    destination.trim(),
                    region.trim().ifBlank { "Custom" },
                    dangerous,
                ),
            )
        }
    }

    fun deleteCustomTeleport(id: String) = updateData {
        it.copy(customTeleports = it.customTeleports.filterNot { item -> item.id == id })
    }

    fun toggleSpellbook(spellbook: Spellbook) = updateData { data ->
        val current = data.teleportProfile.spellbooks
        val updated = if (spellbook in current) current - spellbook else current + spellbook
        data.copy(teleportProfile = data.teleportProfile.copy(spellbooks = updated))
    }

    fun toggleTeleportCapability(capability: String) = updateData { data ->
        val current = data.teleportProfile.capabilities
        val updated = if (capability in current) current - capability else current + capability
        data.copy(teleportProfile = data.teleportProfile.copy(capabilities = updated))
    }

    fun setPohLocation(location: String) = updateData {
        it.copy(teleportProfile = it.teleportProfile.copy(pohLocation = location.trim()))
    }

    fun togglePohDestination(destinationId: String) = updateData { data ->
        val current = data.teleportProfile.pohDestinations
        val updated = if (destinationId in current) current - destinationId
        else current + destinationId
        data.copy(teleportProfile = data.teleportProfile.copy(pohDestinations = updated))
    }

    suspend fun exportBackup(passphrase: String): String =
        withContext(Dispatchers.Default) {
            backupManager.exportEncrypted(passphrase)
        }

    suspend fun importBackup(payload: String, passphrase: String) {
        withContext(Dispatchers.IO) {
            backupManager.importEncrypted(payload, passphrase)
        }
        val restored = preferences.load()
        val restoreGeneration = _state.value.restoreGeneration + 1
        _state.value = FeatureState(
            data = restored,
            message = "Backup imported and reloaded.",
            restoreGeneration = restoreGeneration,
        )
        GeAlertScheduler.sync(getApplication(), restored.geAlerts.isNotEmpty())
        FeatureRefreshScheduler.sync(
            getApplication(),
            restored.accounts.any { it.autoRefresh },
        )
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun reloadFromDisk() {
        diskReloadJob?.cancel()
        val versionAtStart = dataVersion
        diskReloadJob = viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { loadPersistedData() }
            if (dataVersion == versionAtStart) {
                _state.update { it.copy(data = data, initializing = false) }
            }
            initialized = true
            syncSchedulers(_state.value.data)
            startForegroundRefresh()
        }
    }

    private fun loadPersistedData(): FeatureData {
        val loaded = preferences.load()
        val migrated = loaded.withTrackedPlayer(toolkitPreferences.load().trackedPlayer)
        if (migrated != loaded) preferences.save(migrated)
        return migrated
    }

    private fun updateData(block: (FeatureData) -> FeatureData) {
        if (!initialized) {
            pendingMutations += block
            return
        }
        var updated = _state.value.data
        _state.update { state ->
            state.copy(data = block(state.data).also { updated = it })
        }
        dataVersion += 1
        persistenceQueue.trySend(updated)
    }

    fun selectAccountForProfile(username: String?) {
        val selected = username?.takeIf { candidate ->
            _state.value.data.accounts.any {
                it.username.equals(candidate, ignoreCase = true)
            }
        }
        updateData { it.copy(selectedAccount = selected) }
    }

    private fun syncSchedulers(data: FeatureData) {
        GeAlertScheduler.sync(getApplication(), data.geAlerts.isNotEmpty())
        FeatureRefreshScheduler.sync(
            getApplication(),
            data.accounts.any { it.autoRefresh },
        )
    }

    private fun id(): String = UUID.randomUUID().toString()

    private suspend fun latestUnitValue(item: PriceSearchItem): Long? =
        runCatching {
            priceClient.latest(listOf(PriceWatchItem(item.id, item.name)))
                .firstOrNull()
                ?.let { it.high ?: it.low }
        }.getOrNull()

    private fun optimiseStops(stops: List<ItineraryStop>): List<ItineraryStop> {
        val regionOrder = listOf(
            "Misthalin",
            "Asgarnia",
            "Kandarin",
            "Fremennik",
            "Fossil Island",
            "Kourend & Kebos",
            "Morytania",
            "Wilderness",
            "Other",
        )
        return stops.sortedWith(
            compareBy<ItineraryStop> { it.completed }
                .thenBy {
                    regionOrder.indexOf(it.region).takeIf { index -> index >= 0 }
                        ?: regionOrder.size
                }
                .thenBy { it.title },
        )
    }

    private fun bestConfiguredTeleport(data: FeatureData, region: String): String {
        val profile = data.accounts.firstOrNull {
            it.username.equals(data.selectedAccount, ignoreCase = true)
        }
        val magicLevel = profile?.magicLevel() ?: 1
        return TeleportCatalog.all
            .asSequence()
            .filter { !it.dangerous }
            .filter { option ->
                option.region.contains(region, ignoreCase = true) ||
                    region.contains(option.region, ignoreCase = true)
            }
            .firstOrNull { it.isAvailable(data.teleportProfile, magicLevel) }
            ?.let { "${it.name} → ${it.destination}" }
            ?: ExpansionCatalog.suggestedTeleport(region)
    }

    private companion object {
        const val FOREGROUND_REFRESH_MILLIS = 10 * 60_000L
        const val PRICE_SEARCH_DEBOUNCE_MS = 300L
    }
}

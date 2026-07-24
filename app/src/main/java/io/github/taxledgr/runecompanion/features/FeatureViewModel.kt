package io.github.taxledgr.runecompanion.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.taxledgr.runecompanion.toolkit.HiscoreClient
import io.github.taxledgr.runecompanion.toolkit.PriceClient
import io.github.taxledgr.runecompanion.toolkit.PriceSearchItem
import io.github.taxledgr.runecompanion.toolkit.PriceWatchItem
import io.github.taxledgr.runecompanion.widget.RuneCompanionWidget
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FeatureViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = FeaturePreferences(application)
    private val backupManager = AppBackupManager(application)
    private val hiscoreClient = HiscoreClient()
    private val priceClient = PriceClient()
    private val _state = MutableStateFlow(FeatureState(data = preferences.load()))
    val state: StateFlow<FeatureState> = _state.asStateFlow()

    init {
        GeAlertScheduler.sync(application, _state.value.data.geAlerts.isNotEmpty())
        FeatureRefreshScheduler.sync(application, _state.value.data.accounts.any { it.autoRefresh })
        viewModelScope.launch {
            while (isActive) {
                delay(FOREGROUND_REFRESH_MILLIS)
                refreshAllAccounts()
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

    fun refreshAccount(username: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { hiscoreClient.lookup(username) }
                .onSuccess { summary ->
                    val snapshot = StatSnapshot(System.currentTimeMillis(), summary)
                    updateData { data ->
                        data.copy(accounts = data.accounts.map { account ->
                            if (!account.username.equals(username, true)) {
                                account
                            } else {
                                account.copy(
                                    snapshots = compactSnapshots(account.snapshots + snapshot),
                                )
                            }
                        })
                    }
                    _state.update {
                        it.copy(loading = false, message = "Updated ${summary.player}")
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            message = error.message ?: "Could not refresh hiscores",
                        )
                    }
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
        if (query.trim().length < 2) {
            _state.update { it.copy(priceSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { priceClient.search(query) }
                .onSuccess { results ->
                    _state.update { it.copy(loading = false, priceSearchResults = results) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, message = error.message ?: "Price search failed")
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
        if (items.isEmpty()) return
        viewModelScope.launch {
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

    fun addCollectionGoal(item: String, denominator: Int) {
        if (item.isBlank() || denominator <= 0) return
        updateData {
            it.copy(
                collectionGoals = it.collectionGoals +
                    CollectionGoal(id(), item.trim(), denominator, 0, false),
            )
        }
    }

    fun adjustCollection(id: String, amount: Int) = updateData {
        it.copy(collectionGoals = it.collectionGoals.map { goal ->
            if (goal.id == id) goal.copy(attempts = (goal.attempts + amount).coerceAtLeast(0))
            else goal
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

    fun exportBackup(passphrase: String): String = backupManager.exportEncrypted(passphrase)

    fun importBackup(payload: String, passphrase: String) {
        backupManager.importEncrypted(payload, passphrase)
        _state.value = FeatureState(
            data = preferences.load(),
            message = "Backup imported. Restart the app to reload Stars, Timers, and Settings.",
        )
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun reloadFromDisk() {
        _state.update { it.copy(data = preferences.load()) }
    }

    private fun updateData(block: (FeatureData) -> FeatureData) {
        _state.update { state -> state.copy(data = block(state.data)) }
        preferences.save(_state.value.data)
        RuneCompanionWidget.updateAll(getApplication())
    }

    private fun id(): String = UUID.randomUUID().toString()

    private companion object {
        const val FOREGROUND_REFRESH_MILLIS = 10 * 60_000L
    }
}

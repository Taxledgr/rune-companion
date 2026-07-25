package io.github.taxledgr.runecompanion.personalization

import io.github.taxledgr.runecompanion.alerts.StarFilterSettings
import io.github.taxledgr.runecompanion.overlay.OverlayModule
import io.github.taxledgr.runecompanion.overlay.OverlaySettings

data class ActivityStarFilters(
    val hideDangerousWorlds: Boolean = true,
    val excludedLocations: Set<String> = emptySet(),
) {
    fun applyTo(settings: StarFilterSettings) = settings.copy(
        hideDangerousWorlds = hideDangerousWorlds,
        excludedLocations = excludedLocations,
    )

    companion object {
        fun from(settings: StarFilterSettings) = ActivityStarFilters(
            hideDangerousWorlds = settings.hideDangerousWorlds,
            excludedLocations = settings.excludedLocations,
        )
    }
}

data class ActivityProfile(
    val id: String,
    val name: String,
    val symbol: String,
    val personalization: PersonalizationSettings,
    val overlay: OverlaySettings,
    val selectedAccount: String? = null,
    val starFilters: ActivityStarFilters = ActivityStarFilters(),
) {
    fun normalized() = copy(
        id = id.trim().take(MAX_ID_LENGTH),
        name = name.trim().take(MAX_NAME_LENGTH).ifBlank { "Activity" },
        symbol = symbol.trim().take(2).ifBlank { "◆" },
        personalization = personalization.normalized(),
        overlay = overlay.normalized(),
        selectedAccount = selectedAccount?.trim()?.takeIf(String::isNotBlank),
        starFilters = starFilters.copy(
            excludedLocations = starFilters.excludedLocations
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet(),
        ),
    )

    companion object {
        const val MAX_NAME_LENGTH = 28
        private const val MAX_ID_LENGTH = 80
    }
}

data class ActivityProfileState(
    val profiles: List<ActivityProfile>,
    val activeProfileId: String,
) {
    val activeProfile: ActivityProfile
        get() = profiles.first { it.id == activeProfileId }

    fun normalized(): ActivityProfileState {
        val unique = profiles
            .map(ActivityProfile::normalized)
            .filter { it.id.isNotBlank() }
            .distinctBy(ActivityProfile::id)
            .take(MAX_PROFILES)
            .ifEmpty {
                listOf(ActivityProfileTemplate.SHOOTING_STARS.profile())
            }
        return copy(
            profiles = unique,
            activeProfileId = activeProfileId.takeIf { id ->
                unique.any { it.id == id }
            } ?: unique.first().id,
        )
    }

    fun select(profileId: String): ActivityProfileState =
        if (profiles.any { it.id == profileId }) {
            copy(activeProfileId = profileId).normalized()
        } else {
            this
        }

    fun updateActive(transform: (ActivityProfile) -> ActivityProfile): ActivityProfileState =
        copy(
            profiles = profiles.map { profile ->
                if (profile.id == activeProfileId) transform(profile).normalized() else profile
            },
        ).normalized()

    fun addCopy(id: String, name: String): ActivityProfileState {
        if (profiles.size >= MAX_PROFILES || id.isBlank()) return this
        val copy = activeProfile.copy(
            id = id,
            name = name,
            symbol = "◆",
        ).normalized()
        return copy(
            profiles = profiles + copy,
            activeProfileId = copy.id,
        ).normalized()
    }

    fun renameActive(name: String): ActivityProfileState =
        updateActive { it.copy(name = name) }

    fun deleteActive(): ActivityProfileState {
        if (profiles.size <= 1) return this
        val activeIndex = profiles.indexOfFirst { it.id == activeProfileId }
        val remaining = profiles.filterNot { it.id == activeProfileId }
        val nextIndex = activeIndex.coerceIn(0, remaining.lastIndex)
        return ActivityProfileState(
            profiles = remaining,
            activeProfileId = remaining[nextIndex].id,
        ).normalized()
    }

    companion object {
        const val MAX_PROFILES = 12

        fun fromLegacy(
            personalization: PersonalizationSettings,
            overlay: OverlaySettings,
            selectedAccount: String?,
            filters: StarFilterSettings,
        ): ActivityProfileState {
            val normalizedPersonalization = personalization.normalized()
            val matchingTemplate = ActivityProfileTemplate.entries.firstOrNull {
                it.personalization == normalizedPersonalization
            }
            val templates = ActivityProfileTemplate.entries.map { template ->
                template.profile(
                    selectedAccount = selectedAccount,
                    starFilters = ActivityStarFilters.from(filters),
                )
            }.toMutableList()
            val activeId = if (matchingTemplate != null) {
                val index = templates.indexOfFirst { it.id == matchingTemplate.id }
                templates[index] = templates[index].copy(
                    personalization = normalizedPersonalization,
                    overlay = overlay.normalized(),
                )
                matchingTemplate.id
            } else {
                val current = ActivityProfile(
                    id = "my-setup",
                    name = "My setup",
                    symbol = "◆",
                    personalization = normalizedPersonalization,
                    overlay = overlay.normalized(),
                    selectedAccount = selectedAccount,
                    starFilters = ActivityStarFilters.from(filters),
                )
                templates.add(0, current)
                current.id
            }
            return ActivityProfileState(templates, activeId).normalized()
        }
    }
}

enum class ActivityProfileTemplate(
    val id: String,
    val profileName: String,
    val symbol: String,
    val personalization: PersonalizationSettings,
    val overlay: OverlaySettings,
) {
    SHOOTING_STARS(
        id = "shooting-stars",
        profileName = "Shooting Stars",
        symbol = "✦",
        personalization = ExperiencePreset.SHOOTING_STARS.settings,
        overlay = OverlaySettings(
            enabledModules = setOf(
                OverlayModule.STARS,
                OverlayModule.PLAYER,
                OverlayModule.SKILL_GOALS,
                OverlayModule.TELEPORTS,
            ),
            selectedModule = OverlayModule.STARS,
        ),
    ),
    SKILLING(
        id = "skilling",
        profileName = "Skilling",
        symbol = "XP",
        personalization = ExperiencePreset.SKILLING.settings,
        overlay = OverlaySettings(
            enabledModules = setOf(
                OverlayModule.PLAYER,
                OverlayModule.SKILL_GOALS,
                OverlayModule.BANKED_XP,
                OverlayModule.TIMERS,
            ),
            selectedModule = OverlayModule.PLAYER,
        ),
    ),
    QUESTING(
        id = "questing",
        profileName = "Questing",
        symbol = "Q",
        personalization = ExperiencePreset.QUESTING.settings,
        overlay = OverlaySettings(
            enabledModules = setOf(
                OverlayModule.QUESTS_DIARIES,
                OverlayModule.TELEPORTS,
                OverlayModule.CHECKLIST,
            ),
            selectedModule = OverlayModule.QUESTS_DIARIES,
        ),
    ),
    SLAYER(
        id = "slayer",
        profileName = "Slayer",
        symbol = "⚔",
        personalization = ExperiencePreset.SLAYER.settings,
        overlay = OverlaySettings(
            enabledModules = setOf(
                OverlayModule.SLAYER,
                OverlayModule.TRIP,
                OverlayModule.SLAYER_CARDS,
                OverlayModule.LOADOUTS,
                OverlayModule.SUPPLIES,
            ),
            selectedModule = OverlayModule.SLAYER,
        ),
    ),
    BOSSING(
        id = "bossing",
        profileName = "Bossing",
        symbol = "B",
        personalization = ExperiencePreset.BOSSING.settings,
        overlay = OverlaySettings(
            enabledModules = setOf(
                OverlayModule.BOSS_READINESS,
                OverlayModule.TRIP,
                OverlayModule.LOADOUTS,
                OverlayModule.SUPPLIES,
                OverlayModule.SESSIONS,
            ),
            selectedModule = OverlayModule.BOSS_READINESS,
        ),
    ),
    ;

    fun profile(
        selectedAccount: String? = null,
        starFilters: ActivityStarFilters = ActivityStarFilters(),
    ) = ActivityProfile(
        id = id,
        name = profileName,
        symbol = symbol,
        personalization = personalization,
        overlay = overlay,
        selectedAccount = selectedAccount,
        starFilters = starFilters,
    ).normalized()
}

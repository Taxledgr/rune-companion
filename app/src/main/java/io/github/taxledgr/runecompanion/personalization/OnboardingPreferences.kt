package io.github.taxledgr.runecompanion.personalization

import android.content.Context

class OnboardingPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    /**
     * Existing installs already made a primary-activity choice, so the guide is only automatic
     * on a genuinely fresh installation. It remains available later through Customize.
     */
    fun shouldShowOnFirstLaunch(): Boolean {
        if (preferences.getBoolean(KEY_COMPLETE, false)) return false
        val existingProfiles = appContext.getSharedPreferences(
            ActivityProfilePreferences.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).all.isNotEmpty()
        if (existingProfiles) {
            complete()
            return false
        }
        return true
    }

    fun complete() {
        preferences.edit().putBoolean(KEY_COMPLETE, true).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "rune_companion_onboarding"
        private const val KEY_COMPLETE = "completed"
    }
}

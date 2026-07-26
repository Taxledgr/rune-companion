package io.github.taxledgr.runecompanion.util

import io.github.taxledgr.runecompanion.BuildConfig

object AppUserAgent {
    val value: String =
        "Rune Companion/${BuildConfig.VERSION_NAME} (github.com/Taxledgr/rune-companion)"
}

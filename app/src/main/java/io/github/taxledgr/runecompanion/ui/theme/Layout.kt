package io.github.taxledgr.runecompanion.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.personalization.LayoutDensity

data class RuneLayoutMetrics(
    val screenPadding: Dp,
    val sectionSpacing: Dp,
    val cardPadding: Dp,
    val itemSpacing: Dp,
)

fun LayoutDensity.metrics(): RuneLayoutMetrics = when (this) {
    LayoutDensity.COMPACT -> RuneLayoutMetrics(
        screenPadding = 12.dp,
        sectionSpacing = 8.dp,
        cardPadding = 12.dp,
        itemSpacing = 6.dp,
    )
    LayoutDensity.COMFORTABLE -> RuneLayoutMetrics(
        screenPadding = 18.dp,
        sectionSpacing = 12.dp,
        cardPadding = 16.dp,
        itemSpacing = 10.dp,
    )
    LayoutDensity.LARGE -> RuneLayoutMetrics(
        screenPadding = 24.dp,
        sectionSpacing = 16.dp,
        cardPadding = 20.dp,
        itemSpacing = 12.dp,
    )
}

val LocalRuneLayout = staticCompositionLocalOf {
    LayoutDensity.COMFORTABLE.metrics()
}

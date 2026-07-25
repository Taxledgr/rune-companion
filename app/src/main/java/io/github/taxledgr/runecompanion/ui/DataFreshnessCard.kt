package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.github.taxledgr.runecompanion.ui.theme.LocalRuneLayout
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import io.github.taxledgr.runecompanion.util.reportAge
import java.time.Duration
import java.time.Instant

@Composable
internal fun DataFreshnessCard(
    source: String,
    updatedAt: Instant?,
    expectedRefreshMinutes: Int,
    refreshing: Boolean,
    cached: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    val layout = LocalRuneLayout.current
    val stale = updatedAt?.let {
        Duration.between(it, Instant.now()).toMinutes() >
            expectedRefreshMinutes.coerceAtLeast(1) * STALE_MULTIPLIER
    } ?: true
    val status = when {
        refreshing -> "Refreshing…"
        cached -> "Cached data"
        error != null && updatedAt != null -> "Last-known-good data"
        error != null -> "Unavailable"
        stale -> "Update recommended"
        else -> "Current"
    }
    val detail = when {
        error != null && updatedAt != null ->
            "The latest request failed. Showing data from ${reportAge(updatedAt)}."
        error != null -> error
        updatedAt != null -> "Updated ${reportAge(updatedAt)} from $source."
        else -> "Waiting for the first update from $source."
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (cached || error != null || stale) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(layout.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$source • $status", fontWeight = FontWeight.Bold)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!cached && error == null && !stale) {
                        RuneCyan
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!refreshing && (cached || error != null || stale)) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

private const val STALE_MULTIPLIER = 3

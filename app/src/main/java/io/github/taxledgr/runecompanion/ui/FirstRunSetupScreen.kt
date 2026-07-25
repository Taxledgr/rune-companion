package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.github.taxledgr.runecompanion.personalization.ActivityProfileTemplate
import io.github.taxledgr.runecompanion.ui.theme.LocalRuneLayout

@Composable
fun FirstRunSetupScreen(
    onChoose: (ActivityProfileTemplate) -> Unit,
) {
    val layout = LocalRuneLayout.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
    ) {
        item {
            ScreenHeader(
                eyebrow = "QUICK SETUP • STEP 1 OF 1",
                title = "What are you doing most?",
                subtitle = "Choose a starting layout. Nothing is locked: every tab, shortcut, " +
                    "overlay section, size, and activity profile can be changed later.",
            )
        }
        items(ActivityProfileTemplate.entries) { template ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChoose(template) },
            ) {
                Column(
                    modifier = Modifier.padding(layout.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
                ) {
                    Text(
                        "${template.symbol}  ${template.profileName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        template.personalization.startFeatureId?.let {
                            "Starts in ${template.profileName} tools with its useful shortcuts ready."
                        } ?: "Starts on ${template.personalization.startTab.label}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onChoose(template) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Use ${template.profileName}")
                    }
                }
            }
        }
        item {
            Text(
                "Next: add a public-Hiscores username, choose your available teleports, and " +
                    "fine-tune the layout in More → Customize experience.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package io.github.taxledgr.runecompanion.ui

import android.os.SystemClock
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import io.github.taxledgr.runecompanion.features.CustomTeleport
import io.github.taxledgr.runecompanion.features.FeatureData
import io.github.taxledgr.runecompanion.QualityTestActivity
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.personalization.ActivityProfileTemplate
import io.github.taxledgr.runecompanion.personalization.CustomizationMode
import io.github.taxledgr.runecompanion.personalization.LayoutDensity
import io.github.taxledgr.runecompanion.personalization.PersonalizationSettings
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.ui.theme.RuneCompanionTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class QualityUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<QualityTestActivity>()

    @Before
    fun settlePhysicalDeviceSecurityScan() {
        if (FIRST_DEVICE_TEST.compareAndSet(false, true)) {
            // Samsung/Norton may briefly foreground its install scan after a new
            // isolated APK is installed. Relaunching the empty test host after
            // that one-time scan prevents it from stealing the first test root.
            SystemClock.sleep(1_000)
            compose.activityRule.scenario.recreate()
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun customizationPreviewsThenSavesAdvancedCompactLayout() {
        var saved = PersonalizationSettings()
        val profiles = ActivityProfileState(
            profiles = listOf(ActivityProfileTemplate.SHOOTING_STARS.profile()),
            activeProfileId = ActivityProfileTemplate.SHOOTING_STARS.id,
        )
        compose.setContent {
            RuneCompanionTheme {
                PersonalizationScreen(
                    settings = saved,
                    activityProfiles = profiles,
                    onSettingsChanged = { saved = it },
                    onActivityProfileSelected = {},
                    onActivityProfileCreated = {},
                    onActivityProfileRenamed = {},
                    onActivityProfileDeleted = {},
                )
            }
        }

        compose.onNodeWithText("LIVE PREVIEW").assertIsDisplayed()
        compose.onNodeWithText("Advanced").performClick()
        compose.onAllNodes(hasScrollAction())[0]
            .performScrollToNode(hasText("App layout size"))
        compose.onAllNodesWithText("Compact")[0].performClick()
        compose.onAllNodes(hasScrollAction())[0]
            .performScrollToNode(hasText("Unsaved customization"))
        compose.onNodeWithText("Unsaved customization").assertIsDisplayed()
        compose.onAllNodesWithText("Save")[0].performClick()

        compose.runOnIdle {
            assertEquals(CustomizationMode.ADVANCED, saved.customizationMode)
            assertEquals(LayoutDensity.COMPACT, saved.appDensity)
        }
    }

    @Test
    fun globalSearchFindsSavedTeleport() {
        compose.setContent {
            RuneCompanionTheme {
                GlobalSearchScreen(
                    featureData = FeatureData(
                        customTeleports = listOf(
                            CustomTeleport(
                                "1",
                                "Moonclan portal",
                                "Lunar Isle",
                                "Fremennik",
                                false,
                            ),
                        ),
                    ),
                    toolkitState = ToolkitState(),
                    onClose = {},
                    onNavigate = { _, _ -> },
                    onOpenWiki = {},
                )
            }
        }

        compose.onNodeWithText("What do you need?").performTextInput("Moonclan")
        compose.onNodeWithText("Moonclan portal").assertIsDisplayed()
    }

    @Test
    fun tripOverlayEditorHidesUnrelatedReminderControls() {
        compose.setContent {
            RuneCompanionTheme {
                TimersScreen(
                    state = ToolkitState(),
                    notificationPermissionGranted = true,
                    onRequestNotificationPermission = {},
                    onAddReminder = { _, _, _ -> },
                    onDeleteReminder = {},
                    onSetTripLabel = {},
                    onToggleTripTimer = {},
                    onResetTripTimer = {},
                    focus = TimerEditorFocus.TRIP,
                )
            }
        }

        compose.onNodeWithText("Trip timer").assertIsDisplayed()
        compose.onAllNodesWithText("Saved reminders").assertCountEquals(0)
        compose.onNodeWithText("Boss / raid stopwatch").assertIsDisplayed()
    }

    private companion object {
        val FIRST_DEVICE_TEST = AtomicBoolean(false)
    }
}

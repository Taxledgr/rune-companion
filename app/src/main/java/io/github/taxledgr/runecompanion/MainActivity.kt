package io.github.taxledgr.runecompanion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.taxledgr.runecompanion.overlay.OverlayService
import io.github.taxledgr.runecompanion.ui.RuneCompanionApp
import io.github.taxledgr.runecompanion.ui.StarViewModel
import io.github.taxledgr.runecompanion.ui.theme.RuneCompanionTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val overlayPermission = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuneCompanionTheme {
                val starViewModel: StarViewModel = viewModel()
                val state = starViewModel.state.collectAsStateWithLifecycle()
                val permission = overlayPermission.collectAsStateWithLifecycle()
                val overlayRunning = OverlayService.running.collectAsStateWithLifecycle()
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) startOverlay(this)
                }

                RuneCompanionApp(
                    state = state.value,
                    overlayPermissionGranted = permission.value,
                    overlayRunning = overlayRunning.value,
                    onRefresh = starViewModel::refresh,
                    onGrantOverlayPermission = ::openOverlaySettings,
                    onToggleOverlay = {
                        if (overlayRunning.value) {
                            stopService(Intent(this, OverlayService::class.java))
                        } else if (!permission.value) {
                            openOverlaySettings()
                        } else if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            startOverlay(this)
                        }
                    },
                    onOpenStarMiners = ::openStarMiners,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayPermission.value = Settings.canDrawOverlays(this)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun openStarMiners() {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://map.starminers.site/")),
        )
    }
}

private fun startOverlay(context: Context) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, OverlayService::class.java),
    )
}

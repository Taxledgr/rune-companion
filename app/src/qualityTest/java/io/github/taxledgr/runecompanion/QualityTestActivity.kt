package io.github.taxledgr.runecompanion

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity

/**
 * Empty host used only by the isolated on-device Compose regression package.
 *
 * The screen/lock flags keep physical-device tests deterministic when the phone
 * dozes between runs. They are absent from debug and release builds.
 */
class QualityTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
    }
}

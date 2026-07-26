package io.github.taxledgr.runecompanion.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

object SensitiveClipboard {
    fun copyTemporarily(
        context: Context,
        label: String,
        value: String,
    ) {
        val manager = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, value).apply {
            description.extras = PersistableBundle().apply {
                putBoolean(SENSITIVE_CLIP_EXTRA, true)
            }
        }
        manager.setPrimaryClip(clip)
        Handler(Looper.getMainLooper()).postDelayed(
            {
                val current = manager.primaryClip
                    ?.getItemAt(0)
                    ?.text
                    ?.toString()
                if (current == value) manager.clearPrimaryClip()
            },
            CLEAR_CLIPBOARD_AFTER_MS,
        )
    }

    private const val CLEAR_CLIPBOARD_AFTER_MS = 2 * 60 * 1_000L
    private const val SENSITIVE_CLIP_EXTRA = "android.content.extra.IS_SENSITIVE"
}

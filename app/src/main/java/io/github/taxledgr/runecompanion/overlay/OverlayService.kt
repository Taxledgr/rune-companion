package io.github.taxledgr.runecompanion.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.taxledgr.runecompanion.MainActivity
import io.github.taxledgr.runecompanion.R
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.StarRepository
import io.github.taxledgr.runecompanion.util.reportAge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var starContainer: LinearLayout? = null
    private var statusText: TextView? = null
    private var refreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (overlayView == null) {
            showOverlay()
        }
        _running.value = true
        beginRefreshing()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        refreshJob?.cancel()
        serviceScope.cancel()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        _running.value = false
        super.onDestroy()
    }

    private fun showOverlay() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = panelBackground()
            elevation = dp(10).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = textView("✦  SHOOTING STARS", 15f, Color.rgb(244, 201, 93)).apply {
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, dp(8), 0)
        }
        header.addView(
            title,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(actionButton("↻") { refreshNow() })
        header.addView(actionButton("×") { stopSelf() })
        root.addView(header)

        statusText = textView(
            getString(R.string.overlay_loading),
            12f,
            Color.rgb(185, 201, 212),
        ).also {
            it.setPadding(0, dp(6), 0, dp(6))
            root.addView(it)
        }
        starContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }.also(root::addView)

        val params = WindowManager.LayoutParams(
            dp(310),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(14)
            y = dp(100)
        }

        makeDraggable(title, params)
        windowManager.addView(root, params)
        overlayView = root
    }

    private fun beginRefreshing() {
        if (refreshJob?.isActive == true) return
        refreshJob = serviceScope.launch {
            while (isActive) {
                loadStars()
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun refreshNow() {
        serviceScope.launch { loadStars() }
    }

    private suspend fun loadStars() {
        statusText?.setText(R.string.overlay_updating)
        runCatching { StarRepository.latest() }
            .onSuccess { feed ->
                statusText?.text = resources.getQuantityString(
                    R.plurals.overlay_live_reports,
                    feed.stars.size,
                    feed.stars.size,
                )
                renderStars(feed.stars.take(MAX_OVERLAY_STARS))
            }
            .onFailure { throwable ->
                statusText?.text = throwable.message ?: "Feed unavailable"
            }
    }

    private fun renderStars(stars: List<ShootingStar>) {
        starContainer?.apply {
            removeAllViews()
            if (stars.isEmpty()) {
                addView(textView("No active reports", 13f, Color.LTGRAY))
            } else {
                stars.forEachIndexed { index, star ->
                    if (index > 0) {
                        addView(View(this@OverlayService).apply {
                            setBackgroundColor(Color.rgb(43, 64, 78))
                        }, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(1),
                        ))
                    }
                    addView(
                        textView(
                            "W${star.world}  T${star.tier}  ${star.locationName}\n" +
                                "${reportAge(star.calledAt)} • ${star.calledBy}",
                            13f,
                            Color.WHITE,
                        ).apply {
                            setPadding(0, dp(8), 0, dp(8))
                            maxLines = 3
                        },
                    )
                }
            }
        }
    }

    private fun makeDraggable(handle: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    overlayView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handle.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun panelBackground() = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        setColor(Color.rgb(10, 25, 36))
        setStroke(dp(1), Color.rgb(68, 91, 105))
    }

    private fun actionButton(label: String, action: () -> Unit) =
        textView(label, 22f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { action() }
        }

    private fun textView(label: String, size: Float, colour: Int) = TextView(this).apply {
        text = label
        textSize = size
        setTextColor(colour)
        typeface = Typeface.create("sans", Typeface.NORMAL)
    }

    private fun startInForeground() {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rune)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.stop), stopIntent)
            .build()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            serviceType,
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "rune_companion_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "io.github.taxledgr.runecompanion.STOP_OVERLAY"
        private const val REFRESH_INTERVAL_MS = 60_000L
        private const val MAX_OVERLAY_STARS = 5

        private val _running = MutableStateFlow(false)
        val running = _running.asStateFlow()
    }
}

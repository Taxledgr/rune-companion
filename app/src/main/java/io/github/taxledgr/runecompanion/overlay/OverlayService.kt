package io.github.taxledgr.runecompanion.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import android.view.ViewConfiguration
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.taxledgr.runecompanion.MainActivity
import io.github.taxledgr.runecompanion.R
import io.github.taxledgr.runecompanion.alerts.StarAlertNotifier
import io.github.taxledgr.runecompanion.alerts.StarAlertPreferences
import io.github.taxledgr.runecompanion.alerts.StarFilterPreferences
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.StarMapCatalog
import io.github.taxledgr.runecompanion.data.StarMapPoint
import io.github.taxledgr.runecompanion.data.StarRepository
import io.github.taxledgr.runecompanion.features.FeaturePreferences
import io.github.taxledgr.runecompanion.features.RankedStarTravelRoute
import io.github.taxledgr.runecompanion.features.StarTravelCatalog
import io.github.taxledgr.runecompanion.features.StarTravelPlanner
import io.github.taxledgr.runecompanion.toolkit.ToolkitPreferences
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
import kotlin.math.abs

class OverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val alertNotifier by lazy { StarAlertNotifier(this) }
    private val alertPreferences by lazy { StarAlertPreferences(this) }
    private val filterPreferences by lazy { StarFilterPreferences(this) }
    private val featurePreferences by lazy { FeaturePreferences(this) }
    private val toolkitPreferences by lazy { ToolkitPreferences(this) }
    private val overlayPreferences by lazy { OverlayPreferences(this) }
    private lateinit var windowManager: WindowManager
    private lateinit var overlayParams: WindowManager.LayoutParams
    private var overlayView: View? = null
    private var panelView: View? = null
    private var bubbleView: TextView? = null
    private var starContainer: LinearLayout? = null
    private var starScroll: ScrollView? = null
    private var statusText: TextView? = null
    private var headerTitle: TextView? = null
    private var refreshJob: Job? = null
    private var latestStars: List<ShootingStar> = emptyList()
    private var overlaySettings = OverlaySettings()
    private var activeBadgeCount = 0
    private var expandedStarId: String? = null
    private var panelX = 0
    private var panelY = 0
    private var bubbleX: Int? = null
    private var bubbleY: Int? = null
    private val mapViews = mutableListOf<WebView>()

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

        overlaySettings = overlayPreferences.load()
        if (overlayView == null) {
            showOverlay()
        } else if (intent?.action == ACTION_RELOAD) {
            expandedStarId = null
            renderActiveModule()
        }
        _running.value = true
        beginRefreshing()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        refreshJob?.cancel()
        serviceScope.cancel()
        destroyMapViews()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        panelView = null
        bubbleView = null
        _running.value = false
        super.onDestroy()
    }

    private fun showOverlay() {
        val root = FrameLayout(this)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = panelBackground()
            elevation = dp(10).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = textView("", 14f, Color.rgb(244, 201, 93)).apply {
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, dp(8), 0)
            maxLines = 1
        }
        headerTitle = title
        header.addView(
            title,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(
            actionButton("‹", "Previous enabled section") {
                cycleModule(-1)
            },
        )
        header.addView(
            actionButton("›", "Next enabled section") {
                cycleModule(1)
            },
        )
        header.addView(
            actionButton("−", "Minimise to floating bubble") {
                minimiseToBubble()
            },
        )
        header.addView(actionButton("×", "Close Rune Companion panel") { stopSelf() })
        panel.addView(header)

        statusText = textView(
            getString(R.string.overlay_loading),
            12f,
            Color.rgb(185, 201, 212),
        ).also {
            it.setPadding(0, dp(6), 0, dp(6))
            panel.addView(it)
        }
        starContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        starScroll = ScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = true
            addView(
                starContainer,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }.also {
            panel.addView(
                it,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        val bubble = textView("✦", 22f, Color.rgb(244, 201, 93)).apply {
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            background = bubbleBackground()
            elevation = dp(12).toFloat()
            visibility = View.GONE
            contentDescription = "Restore Rune Companion panel"
        }
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(
            bubble,
            FrameLayout.LayoutParams(dp(BUBBLE_SIZE_DP), dp(BUBBLE_SIZE_DP)),
        )

        overlayParams = WindowManager.LayoutParams(
            dp(PANEL_WIDTH_DP),
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

        panelView = panel
        bubbleView = bubble
        makeDraggable(title, overlayParams)
        makeDraggable(
            handle = bubble,
            params = overlayParams,
            onTap = ::restorePanel,
            snapToEdge = true,
            onDragEnd = {
                bubbleX = overlayParams.x
                bubbleY = overlayParams.y
            },
        )
        windowManager.addView(root, overlayParams)
        overlayView = root
        renderActiveModule()
    }

    private fun beginRefreshing() {
        if (refreshJob?.isActive == true) return
        refreshJob = serviceScope.launch {
            while (isActive) {
                if (
                    OverlayModule.STARS in overlaySettings.enabledModules ||
                    alertPreferences.load().enabled
                ) {
                    loadStars()
                } else {
                    renderActiveModule()
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    private suspend fun loadStars() {
        if (overlaySettings.selectedModule == OverlayModule.STARS) {
            statusText?.setText(R.string.overlay_updating)
        }
        runCatching { StarRepository.latest() }
            .onSuccess { feed ->
                val visibleStars = feed.stars.filter(filterPreferences.load()::includes)
                latestStars = visibleStars
                renderActiveModule()
                alertNotifier.notifyForMatches(feed.stars)
            }
            .onFailure { throwable ->
                if (overlaySettings.selectedModule == OverlayModule.STARS) {
                    statusText?.text = throwable.message ?: "Feed unavailable"
                } else {
                    renderActiveModule()
                }
            }
    }

    private fun cycleModule(delta: Int) {
        val modules = overlaySettings.normalized().orderedModules
        if (modules.isEmpty()) return
        val currentIndex = modules.indexOf(overlaySettings.selectedModule).coerceAtLeast(0)
        val nextIndex = (currentIndex + delta).mod(modules.size)
        overlaySettings = overlaySettings.copy(selectedModule = modules[nextIndex]).normalized()
        overlayPreferences.save(overlaySettings)
        expandedStarId = null
        renderActiveModule()
    }

    private fun renderActiveModule() {
        val module = overlaySettings.normalized().selectedModule
        headerTitle?.text = getString(
            R.string.overlay_header_title,
            module.symbol,
            module.label.uppercase(),
        )
        if (module == OverlayModule.STARS) {
            renderStars(latestStars)
            return
        }

        expandedStarId = null
        destroyMapViews()
        val content = OverlayContentBuilder.build(
            module = module,
            toolkit = toolkitPreferences.load(),
            features = featurePreferences.load(),
            stars = latestStars,
        )
        activeBadgeCount = content.badgeCount
        statusText?.text = getString(
            R.string.overlay_module_status,
            modulePositionLabel(),
            content.summary,
        )
        updateBubble(module, activeBadgeCount)
        val visibleEntries = content.entries.take(MAX_OVERLAY_ENTRIES)
        starContainer?.apply {
            removeAllViews()
            if (visibleEntries.isEmpty()) {
                addView(textView(content.emptyMessage, 13f, Color.LTGRAY).apply {
                    setPadding(0, dp(8), 0, dp(8))
                })
            } else {
                visibleEntries.forEachIndexed { index, entry ->
                    if (index > 0) addView(contentDivider())
                    addView(contentRow(entry))
                }
                if (content.entries.size > visibleEntries.size) {
                    addView(contentDivider())
                    addView(
                        textView(
                            "+${content.entries.size - visibleEntries.size} more • " +
                                "open Rune Companion for the full list",
                            11f,
                            Color.rgb(185, 201, 212),
                        ).apply {
                            setPadding(0, dp(8), 0, dp(5))
                        },
                    )
                }
            }
        }
        updateScrollHeight(
            if (visibleEntries.size > COMPACT_ENTRY_LIMIT) {
                EXPANDED_LIST_HEIGHT_DP
            } else {
                null
            },
        )
    }

    private fun renderStars(stars: List<ShootingStar>) {
        val visibleStars = stars.take(MAX_OVERLAY_STARS)
        if (expandedStarId != null && visibleStars.none { it.overlayId() == expandedStarId }) {
            expandedStarId = null
        }
        destroyMapViews()
        activeBadgeCount = stars.size
        statusText?.text = resources.getQuantityString(
            R.plurals.overlay_star_status,
            stars.size,
            modulePositionLabel(),
            stars.size,
        )
        updateBubble(OverlayModule.STARS, activeBadgeCount)
        starContainer?.apply {
            removeAllViews()
            if (visibleStars.isEmpty()) {
                addView(textView("No active reports", 13f, Color.LTGRAY))
            } else {
                visibleStars.forEachIndexed { index, star ->
                    if (index > 0) {
                        addView(View(this@OverlayService).apply {
                            setBackgroundColor(Color.rgb(43, 64, 78))
                        }, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(1),
                        ))
                    }
                    addView(starRow(star))
                }
            }
        }
        updateScrollHeight(
            if (expandedStarId == null) null else EXPANDED_LIST_HEIGHT_DP,
        )
    }

    private fun contentRow(entry: OverlayEntry): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(5))
            addView(
                textView(entry.title, 13f, entry.tone.colour()).apply {
                    setTypeface(typeface, Typeface.BOLD)
                    maxLines = 3
                },
            )
            if (entry.detail.isNotBlank()) {
                addView(
                    textView(entry.detail, 11f, Color.rgb(185, 201, 212)).apply {
                        setPadding(0, dp(2), 0, 0)
                        maxLines = 4
                    },
                )
            }
        }

    private fun OverlayTone.colour(): Int = when (this) {
        OverlayTone.NORMAL -> Color.WHITE
        OverlayTone.GOOD -> Color.rgb(89, 211, 199)
        OverlayTone.WARNING -> Color.rgb(244, 201, 93)
        OverlayTone.DANGER -> Color.rgb(255, 126, 126)
    }

    private fun contentDivider() =
        View(this).apply {
            setBackgroundColor(Color.rgb(43, 64, 78))
        }.also { divider ->
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1),
            )
        }

    private fun updateScrollHeight(fixedHeightDp: Int?) {
        starScroll?.layoutParams = starScroll?.layoutParams?.apply {
            height = fixedHeightDp?.let(::dp) ?: LinearLayout.LayoutParams.WRAP_CONTENT
        }
        starScroll?.requestLayout()
    }

    private fun modulePositionLabel(): String {
        val modules = overlaySettings.orderedModules
        val index = modules.indexOf(overlaySettings.selectedModule).coerceAtLeast(0)
        return "${index + 1}/${modules.size.coerceAtLeast(1)}"
    }

    private fun starRow(star: ShootingStar): View {
        val id = star.overlayId()
        val expanded = expandedStarId == id
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(4))
            isClickable = true
            isFocusable = true
            contentDescription = if (expanded) {
                "Hide route for world ${star.world} ${star.locationName}"
            } else {
                "Show route for world ${star.world} ${star.locationName}"
            }
            setOnClickListener {
                expandedStarId = if (expanded) null else id
                renderStars(latestStars)
            }
            addView(
                textView(
                    "W${star.world}  T${star.tier}  ${star.locationName}  " +
                        if (expanded) "▴" else "›",
                    13f,
                    Color.WHITE,
                ).apply {
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(5), 0, dp(2))
                    maxLines = 3
                },
            )
            addView(
                textView(
                    "${reportAge(star.calledAt)} • ${star.calledBy}" +
                        if (expanded) " • tap title to hide" else " • tap for route & map",
                    11f,
                    Color.rgb(185, 201, 212),
                ).apply {
                    setPadding(0, 0, 0, dp(5))
                },
            )
            if (expanded) addExpandedRoute(star)
        }
    }

    private fun LinearLayout.addExpandedRoute(star: ShootingStar) {
        val guide = StarTravelCatalog.guideFor(star.locationName)
        val rankedRoutes = guide?.let {
            StarTravelPlanner.rank(it, featurePreferences.load())
        }.orEmpty()
        if (rankedRoutes.isEmpty()) {
            addView(textView("No route guide is available for this report.", 12f, Color.LTGRAY))
            return
        }

        rankedRoutes.forEachIndexed { index, ranked ->
            if (index > 0) {
                addView(View(this@OverlayService).apply {
                    setBackgroundColor(Color.rgb(43, 64, 78))
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1),
                ))
            }
            val heading = when (index) {
                0 -> if (ranked.available) {
                    "FASTEST AVAILABLE • ${ranked.route.method}"
                } else {
                    "FASTEST KNOWN • ${ranked.route.method}"
                }
                else -> "${index + 1}. ${ranked.route.method}"
            }
            addView(
                textView(
                    heading,
                    12f,
                    if (ranked.available) {
                        Color.rgb(89, 211, 199)
                    } else {
                        Color.rgb(244, 201, 93)
                    },
                ).apply {
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(7), 0, dp(2))
                },
            )
            addView(
                textView(ranked.route.steps, 11f, Color.WHITE).apply {
                    setPadding(0, 0, 0, dp(2))
                },
            )
            routeRequirementText(ranked)?.let { detail ->
                addView(
                    textView(
                        detail,
                        10f,
                        if (ranked.route.dangerous) {
                            Color.rgb(255, 126, 126)
                        } else {
                            Color.rgb(185, 201, 212)
                        },
                    ).apply {
                        setPadding(0, 0, 0, dp(5))
                    },
                )
            }
        }

        StarMapCatalog.pointFor(star.locationName)?.let { point ->
            addView(
                textView(
                    "EXACT LANDING SITE • ${point.region}",
                    11f,
                    Color.rgb(244, 201, 93),
                ).apply {
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(8), 0, dp(5))
                },
            )
            addView(
                mapPreview(point),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(OVERLAY_MAP_HEIGHT_DP),
                ),
            )
        }
    }

    private fun routeRequirementText(ranked: RankedStarTravelRoute): String? {
        val details = buildList {
            if (ranked.available) add("Configured for your selected profile")
            addAll(ranked.missing)
            ranked.route.agilityLevel?.let { add("$it Agility shortcut") }
            addAll(ranked.route.requirements)
            if (ranked.route.dangerous) add("WILDERNESS — risk items and check the world")
        }
        return details.takeIf { it.isNotEmpty() }?.joinToString(" • ")
    }

    private fun mapPreview(point: StarMapPoint) = WebView(this).apply {
        setBackgroundColor(Color.rgb(7, 19, 28))
        settings.javaScriptEnabled = false
        settings.loadsImagesAutomatically = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.safeBrowsingEnabled = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = WebViewClient()
        loadDataWithBaseURL(
            StarMapCatalog.MAP_BASE_URL,
            StarMapCatalog.previewHtml(point),
            "text/html",
            "UTF-8",
            null,
        )
        setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) view.performClick()
            true
        }
        mapViews += this
    }

    private fun destroyMapViews() {
        mapViews.forEach { view ->
            view.stopLoading()
            view.webViewClient = WebViewClient()
            view.destroy()
        }
        mapViews.clear()
    }

    private fun ShootingStar.overlayId(): String = "$world|$locationName|$calledAt"

    private fun makeDraggable(
        handle: View,
        params: WindowManager.LayoutParams,
        onTap: (() -> Unit)? = null,
        snapToEdge: Boolean = false,
        onDragEnd: (() -> Unit)? = null,
    ) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        handle.setOnClickListener { onTap?.invoke() }
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - touchX
                    val deltaY = event.rawY - touchY
                    if (!moved && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                        moved = true
                    }
                    if (moved) {
                        params.x = initialX + deltaX.toInt()
                        params.y = initialY + deltaY.toInt()
                        clampOverlayPosition(params)
                        overlayView?.let { windowManager.updateViewLayout(it, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        if (snapToEdge) snapBubbleToEdge(params)
                        onDragEnd?.invoke()
                    } else {
                        handle.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun minimiseToBubble() {
        val panel = panelView ?: return
        val bubble = bubbleView ?: return
        if (panel.visibility != View.VISIBLE) return
        panelX = overlayParams.x
        panelY = overlayParams.y
        panel.visibility = View.GONE
        bubble.visibility = View.VISIBLE
        overlayParams.width = dp(BUBBLE_SIZE_DP)
        overlayParams.height = dp(BUBBLE_SIZE_DP)
        val bounds = displayBounds()
        overlayParams.x = bubbleX ?: (
            bounds.width() - dp(BUBBLE_SIZE_DP) - dp(BUBBLE_EDGE_INSET_DP)
            ).coerceAtLeast(0)
        overlayParams.y = bubbleY ?: (panelY + dp(BUBBLE_DEFAULT_OFFSET_DP))
        updateBubble(overlaySettings.selectedModule, activeBadgeCount)
        snapBubbleToEdge(overlayParams)
        bubbleX = overlayParams.x
        bubbleY = overlayParams.y
    }

    private fun restorePanel() {
        val panel = panelView ?: return
        val bubble = bubbleView ?: return
        if (panel.visibility == View.VISIBLE) return
        bubble.visibility = View.GONE
        panel.visibility = View.VISIBLE
        overlayParams.width = dp(PANEL_WIDTH_DP)
        overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        overlayParams.x = panelX
        overlayParams.y = panelY
        clampOverlayPosition(overlayParams)
        overlayView?.let { windowManager.updateViewLayout(it, overlayParams) }
    }

    private fun updateBubble(module: OverlayModule, count: Int) {
        bubbleView?.apply {
            text = getString(R.string.overlay_bubble_text, module.symbol, count)
            contentDescription = "Restore ${module.label} panel"
        }
    }

    private fun clampOverlayPosition(params: WindowManager.LayoutParams) {
        val bounds = displayBounds()
        val width = when {
            params.width > 0 -> params.width
            panelView?.visibility == View.VISIBLE -> dp(PANEL_WIDTH_DP)
            else -> dp(BUBBLE_SIZE_DP)
        }
        params.x = params.x.coerceIn(0, (bounds.width() - width).coerceAtLeast(0))
        params.y = params.y.coerceIn(
            0,
            (bounds.height() - dp(MINIMUM_VISIBLE_HEIGHT_DP)).coerceAtLeast(0),
        )
    }

    private fun snapBubbleToEdge(params: WindowManager.LayoutParams) {
        val bounds = displayBounds()
        val inset = dp(BUBBLE_EDGE_INSET_DP)
        val bubbleWidth = dp(BUBBLE_SIZE_DP)
        params.x = if (params.x + bubbleWidth / 2 < bounds.width() / 2) {
            inset
        } else {
            (bounds.width() - bubbleWidth - inset).coerceAtLeast(0)
        }
        clampOverlayPosition(params)
        overlayView?.let { windowManager.updateViewLayout(it, params) }
    }

    @Suppress("DEPRECATION")
    private fun displayBounds(): android.graphics.Rect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            android.graphics.Rect(
                0,
                0,
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
            )
        }

    private fun panelBackground() = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        setColor(Color.rgb(10, 25, 36))
        setStroke(dp(1), Color.rgb(68, 91, 105))
    }

    private fun bubbleBackground() = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.rgb(10, 25, 36))
        setStroke(dp(2), Color.rgb(244, 201, 93))
    }

    private fun actionButton(
        label: String,
        description: String,
        action: () -> Unit,
    ) =
        textView(label, 22f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            contentDescription = description
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
        private const val ACTION_RELOAD = "io.github.taxledgr.runecompanion.RELOAD_OVERLAY"
        private const val REFRESH_INTERVAL_MS = 60_000L
        private const val MAX_OVERLAY_STARS = 5
        private const val MAX_OVERLAY_ENTRIES = 8
        private const val COMPACT_ENTRY_LIMIT = 5
        private const val PANEL_WIDTH_DP = 310
        private const val BUBBLE_SIZE_DP = 58
        private const val BUBBLE_EDGE_INSET_DP = 8
        private const val BUBBLE_DEFAULT_OFFSET_DP = 140
        private const val MINIMUM_VISIBLE_HEIGHT_DP = 72
        private const val EXPANDED_LIST_HEIGHT_DP = 430
        private const val OVERLAY_MAP_HEIGHT_DP = 180

        private val _running = MutableStateFlow(false)
        val running = _running.asStateFlow()

        fun reloadIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java).setAction(ACTION_RELOAD)
    }
}

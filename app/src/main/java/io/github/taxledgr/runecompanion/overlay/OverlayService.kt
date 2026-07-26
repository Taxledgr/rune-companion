package io.github.taxledgr.runecompanion.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.taxledgr.runecompanion.MainActivity
import io.github.taxledgr.runecompanion.R
import io.github.taxledgr.runecompanion.alerts.StarAlertNotifier
import io.github.taxledgr.runecompanion.alerts.StarAlertPreferences
import io.github.taxledgr.runecompanion.alerts.StarFilterPreferences
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.StarMapCatalog
import io.github.taxledgr.runecompanion.data.StarMapPoint
import io.github.taxledgr.runecompanion.data.StarRepository
import io.github.taxledgr.runecompanion.data.stableReportKey
import io.github.taxledgr.runecompanion.features.FeaturePreferences
import io.github.taxledgr.runecompanion.features.FeatureData
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.features.ExpansionCatalog
import io.github.taxledgr.runecompanion.features.QuestGuideCatalog
import io.github.taxledgr.runecompanion.features.RankedStarTravelRoute
import io.github.taxledgr.runecompanion.features.StarTravelCatalog
import io.github.taxledgr.runecompanion.features.StarTravelPlanner
import io.github.taxledgr.runecompanion.personalization.ActivityProfile
import io.github.taxledgr.runecompanion.personalization.ActivityProfilePreferences
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.personalization.ActivityProfileTemplate
import io.github.taxledgr.runecompanion.personalization.ActivityStarFilters
import io.github.taxledgr.runecompanion.personalization.LayoutDensity
import io.github.taxledgr.runecompanion.personalization.PersonalizationPreferences
import io.github.taxledgr.runecompanion.toolkit.ToolkitViewModel
import io.github.taxledgr.runecompanion.toolkit.ToolkitPreferences
import io.github.taxledgr.runecompanion.toolkit.PersistedToolkitData
import io.github.taxledgr.runecompanion.ui.StarViewModel
import io.github.taxledgr.runecompanion.util.reportAge
import io.github.taxledgr.runecompanion.util.AllowlistedResourceWebViewClient
import io.github.taxledgr.runecompanion.util.applyPrivateWebSettings
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
import kotlin.math.abs

class OverlayService :
    Service(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceViewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val alertNotifier by lazy { StarAlertNotifier(this) }
    private val alertPreferences by lazy { StarAlertPreferences(this) }
    private val filterPreferences by lazy { StarFilterPreferences(this) }
    private val featurePreferences by lazy { FeaturePreferences(this) }
    private val toolkitPreferences by lazy { ToolkitPreferences(this) }
    private val overlayPreferences by lazy { OverlayPreferences(this) }
    private val personalizationPreferences by lazy { PersonalizationPreferences(this) }
    private val activityProfilePreferences by lazy { ActivityProfilePreferences(this) }
    private val editorViewModelFactory by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private val toolkitViewModel by lazy {
        ViewModelProvider(this, editorViewModelFactory)[ToolkitViewModel::class.java]
    }
    private val featureViewModel by lazy {
        ViewModelProvider(this, editorViewModelFactory)[FeatureViewModel::class.java]
    }
    private val starViewModel by lazy {
        ViewModelProvider(this, editorViewModelFactory)[StarViewModel::class.java]
    }
    private lateinit var windowManager: WindowManager
    private lateinit var overlayParams: WindowManager.LayoutParams
    private var overlayView: View? = null
    private var panelView: View? = null
    private var bubbleView: TextView? = null
    private var editorView: ComposeView? = null
    private var profileSwitcherView: View? = null
    private var starContainer: LinearLayout? = null
    private var starScroll: ScrollView? = null
    private var statusText: TextView? = null
    private var headerTitle: TextView? = null
    private var refreshJob: Job? = null
    private var latestStars: List<ShootingStar> = emptyList()
    private var cachedFeatures = FeatureData()
    private var cachedToolkit = PersistedToolkitData()
    private var contentCacheReady = false
    private var overlaySettings = OverlaySettings()
    private var activeBadgeCount = 0
    private var expandedStarId: String? = null
    private var editorQuestId: String? = null
    private var panelX = 0
    private var panelY = 0
    private var bubbleX: Int? = null
    private var bubbleY: Int? = null
    private var profileSwitcherOpenedFromBubble = false
    private var activityProfiles = ActivityProfileState(
        profiles = listOf(ActivityProfileTemplate.SHOOTING_STARS.profile()),
        activeProfileId = ActivityProfileTemplate.SHOOTING_STARS.id,
    )
    private val mapViews = mutableListOf<WebView>()

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
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

        val previousProfileId = activityProfiles.activeProfileId
        overlaySettings = overlayPreferences.load()
        activityProfiles = loadActivityProfiles()
        if (overlayView == null) {
            showOverlay()
        } else if (intent?.action == ACTION_RELOAD) {
            expandedStarId = null
            applyOverlayAppearance(
                useStoredPlacement =
                    previousProfileId != activityProfiles.activeProfileId,
            )
            renderActiveModule()
        }
        _running.value = true
        beginRefreshing()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayView?.post {
            val root = overlayView as? FrameLayout ?: return@post
            if (editorView?.visibility == View.VISIBLE) {
                resizeEditorWindow(root)
            } else {
                applyStoredPlacement()
            }
        }
    }

    override fun onDestroy() {
        if (editorView != null) {
            cachedFeatures = featureViewModel.state.value.data
            cachedToolkit = toolkitViewModel.state.value.toPersistedData()
            captureActiveProfile()
        }
        refreshJob?.cancel()
        serviceScope.cancel()
        destroyMapViews()
        editorView?.disposeComposition()
        editorView = null
        profileSwitcherView = null
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        panelView = null
        bubbleView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceViewModelStore.clear()
        _running.value = false
        super.onDestroy()
    }

    private fun showOverlay() {
        val root = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }
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
            actionButton("−", "Minimise to floating bubble") {
                minimiseToBubble()
            },
        )
        header.addView(
            actionButton("×", "Close panel to floating bubble") {
                minimiseToBubble()
            },
        )
        panel.addView(header)

        val sectionControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sectionControls.addView(
            textView("SECTION", 10f, Color.rgb(185, 201, 212)),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        sectionControls.addView(
            actionButton("‹", "Previous enabled section") {
                cycleModule(-1)
            },
        )
        sectionControls.addView(
            actionButton("›", "Next enabled section") {
                cycleModule(1)
            },
        )
        sectionControls.addView(
            actionButton("✎", "Edit this section") {
                openEditor()
            },
        )
        sectionControls.addView(
            actionButton("◆", "Switch activity profile") {
                showProfileSwitcher(openedFromBubble = false)
            },
        )
        panel.addView(sectionControls)

        statusText = textView(
            getString(R.string.overlay_loading),
            12f,
            Color.rgb(185, 201, 212),
        ).also {
            it.setPadding(0, dp(6), 0, dp(6))
            it.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
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
            panelWidthPx(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        panelView = panel
        bubbleView = bubble
        makeDraggable(
            handle = title,
            params = overlayParams,
            snapToEdge = { overlaySettings.snapToEdge },
            onDragEnd = ::savePanelPlacement,
        )
        makeDraggable(
            handle = bubble,
            params = overlayParams,
            onTap = ::restorePanel,
            onLongPress = {
                showProfileSwitcher(openedFromBubble = true)
            },
            snapToEdge = { overlaySettings.snapToEdge },
            onDragEnd = {
                bubbleX = overlayParams.x
                bubbleY = overlayParams.y
                saveBubblePlacement()
            },
        )
        windowManager.addView(root, overlayParams)
        overlayView = root
        applyOverlayAppearance(useStoredPlacement = true)
        renderActiveModule()
        minimiseToBubble()
    }

    private fun beginRefreshing() {
        if (refreshJob?.isActive == true) return
        refreshJob = serviceScope.launch {
            while (isActive) {
                refreshContentCache()
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

    private suspend fun refreshContentCache() {
        val loaded = withContext(Dispatchers.IO) {
            toolkitPreferences.load() to featurePreferences.load()
        }
        cachedToolkit = loaded.first
        cachedFeatures = loaded.second
        contentCacheReady = true
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
        saveOverlaySettingsToActiveProfile()
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
        if (!contentCacheReady) {
            statusText?.setText(R.string.overlay_loading)
            starContainer?.removeAllViews()
            return
        }

        expandedStarId = null
        destroyMapViews()
        val content = OverlayContentBuilder.build(
            module = module,
            toolkit = cachedToolkit,
            features = cachedFeatures,
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
                    val quest = if (module == OverlayModule.QUESTS_DIARIES) {
                        ExpansionCatalog.progress.filterNot {
                            it.id in cachedFeatures.completedProgressIds
                        }.getOrNull(index)
                    } else {
                        null
                    }
                    addView(
                        contentRow(
                            entry = entry,
                            onClick = quest?.let { selected ->
                                {
                                    editorQuestId = selected.id.takeIf {
                                        selected.category == "Quest" &&
                                            QuestGuideCatalog.forQuest(it) != null
                                    }
                                    openEditor()
                                }
                            },
                        ),
                    )
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
        val pinnedStar = expandedStarId?.let { id ->
            stars.firstOrNull { it.overlayId() == id }
        }
        if (expandedStarId != null && pinnedStar == null) {
            expandedStarId = null
        }
        val visibleStars = buildList {
            pinnedStar?.let(::add)
            addAll(
                stars
                    .filterNot { it.overlayId() == pinnedStar?.overlayId() }
                    .take(MAX_OVERLAY_STARS - size),
            )
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
                addView(textView("No likely-active reports", 13f, Color.LTGRAY))
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

    private fun contentRow(
        entry: OverlayEntry,
        onClick: (() -> Unit)? = null,
    ): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(5))
            onClick?.let { action ->
                isClickable = true
                isFocusable = true
                minimumHeight = dp(48)
                contentDescription = "Open ${entry.title} step-by-step guide"
                val selectableBackground = TypedValue()
                if (
                    theme.resolveAttribute(
                        android.R.attr.selectableItemBackground,
                        selectableBackground,
                        true,
                    )
                ) {
                    setBackgroundResource(selectableBackground.resourceId)
                }
                setOnClickListener { action() }
            }
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
            minimumHeight = dp(48)
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
                        if (expanded) {
                            " • pinned through refresh • tap title to hide"
                        } else {
                            " • community report • tap to pin route"
                        },
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
            StarTravelPlanner.rank(it, cachedFeatures)
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
        applyPrivateWebSettings()
        settings.loadsImagesAutomatically = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = AllowlistedResourceWebViewClient(
            setOf(StarMapCatalog.MAP_IMAGE_URL),
        )
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

    private fun ShootingStar.overlayId(): String = stableReportKey()

    private fun makeDraggable(
        handle: View,
        params: WindowManager.LayoutParams,
        onTap: (() -> Unit)? = null,
        onLongPress: (() -> Unit)? = null,
        snapToEdge: () -> Boolean = { false },
        onDragEnd: (() -> Unit)? = null,
    ) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        var downAt = 0L
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
                    downAt = SystemClock.elapsedRealtime()
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
                        if (snapToEdge()) {
                            if (bubbleView?.visibility == View.VISIBLE) {
                                snapBubbleToEdge(params)
                            } else {
                                snapPanelToEdge(params)
                            }
                        }
                        onDragEnd?.invoke()
                    } else if (
                        onLongPress != null &&
                        SystemClock.elapsedRealtime() - downAt >=
                        ViewConfiguration.getLongPressTimeout()
                    ) {
                        onLongPress()
                    } else {
                        handle.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
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
        if (overlaySettings.snapToEdge) {
            snapBubbleToEdge(overlayParams)
        } else {
            clampOverlayPosition(overlayParams)
        }
        bubbleX = overlayParams.x
        bubbleY = overlayParams.y
        overlayView?.let { windowManager.updateViewLayout(it, overlayParams) }
    }

    private fun restorePanel() {
        val panel = panelView ?: return
        val bubble = bubbleView ?: return
        val editor = editorView
        if (editor != null && editor.visibility != View.VISIBLE) {
            restoreEditorFromBubble(editor)
            return
        }
        if (panel.visibility == View.VISIBLE) return
        bubble.visibility = View.GONE
        panel.visibility = View.VISIBLE
        overlayParams.width = panelWidthPx()
        overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        overlayParams.x = panelX
        overlayParams.y = panelY
        clampOverlayPosition(overlayParams)
        overlayView?.let { windowManager.updateViewLayout(it, overlayParams) }
    }

    private fun openEditor() {
        if (editorView != null) return
        val root = overlayView as? FrameLayout ?: return
        panelX = overlayParams.x
        panelY = overlayParams.y
        panelView?.visibility = View.GONE
        bubbleView?.visibility = View.GONE
        destroyMapViews()

        overlayParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        overlayParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        resizeEditorWindow(root)

        toolkitViewModel.setAppInForeground(true)
        featureViewModel.setAppInForeground(true)
        starViewModel.setAppInForeground(true)
        val editor = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            background = panelBackground()
            clipToOutline = true
            setContent {
                OverlayEditorContent(
                    context = this@OverlayService,
                    module = overlaySettings.selectedModule,
                    toolkitViewModel = toolkitViewModel,
                    featureViewModel = featureViewModel,
                    starViewModel = starViewModel,
                    initialOverlaySettings = overlaySettings,
                    initialPersonalizationSettings = personalizationPreferences.load(),
                    initialQuestId = editorQuestId,
                    onOverlaySettingsChanged = { settings ->
                        overlaySettings = settings.normalized()
                        saveOverlaySettingsToActiveProfile()
                    },
                    onPersonalizationChanged = { settings ->
                        personalizationPreferences.save(settings)
                        updateActiveProfile {
                            it.copy(personalization = settings)
                        }
                    },
                    onMinimise = ::minimiseEditorToBubble,
                    onClose = ::closeEditor,
                    onStopOverlay = ::stopSelf,
                )
            }
        }
        root.addView(
            editor,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        editorView = editor
    }

    private fun minimiseEditorToBubble() {
        val root = overlayView as? FrameLayout ?: return
        val editor = editorView ?: return
        val bubble = bubbleView ?: return
        if (editor.visibility != View.VISIBLE) return

        editor.visibility = View.GONE
        panelView?.visibility = View.GONE
        bubble.visibility = View.VISIBLE
        overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        overlayParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        overlayParams.width = dp(BUBBLE_SIZE_DP)
        overlayParams.height = dp(BUBBLE_SIZE_DP)
        val bounds = displayBounds()
        overlayParams.x = bubbleX ?: (
            bounds.width() - dp(BUBBLE_SIZE_DP) - dp(BUBBLE_EDGE_INSET_DP)
            ).coerceAtLeast(0)
        overlayParams.y = bubbleY ?: (panelY + dp(BUBBLE_DEFAULT_OFFSET_DP))
        updateBubble(overlaySettings.selectedModule, activeBadgeCount)
        bubble.contentDescription = "Resume the open Rune Companion guide"
        if (overlaySettings.snapToEdge) {
            snapBubbleToEdge(overlayParams)
        } else {
            clampOverlayPosition(overlayParams)
        }
        bubbleX = overlayParams.x
        bubbleY = overlayParams.y
        windowManager.updateViewLayout(root, overlayParams)
    }

    private fun restoreEditorFromBubble(editor: ComposeView) {
        val root = overlayView as? FrameLayout ?: return
        bubbleView?.visibility = View.GONE
        panelView?.visibility = View.GONE
        overlayParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        overlayParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        resizeEditorWindow(root)
        editor.visibility = View.VISIBLE
    }

    private fun resizeEditorWindow(root: FrameLayout) {
        val bounds = displayBounds()
        val editorWidth = (bounds.width() * EDITOR_WIDTH_FRACTION).toInt()
        val editorHeight = (bounds.height() * EDITOR_HEIGHT_FRACTION).toInt()
        overlayParams.width = editorWidth
        overlayParams.height = editorHeight
        overlayParams.x = ((bounds.width() - editorWidth) / 2).coerceAtLeast(0)
        overlayParams.y = ((bounds.height() - editorHeight) / 2).coerceAtLeast(0)
        windowManager.updateViewLayout(root, overlayParams)
    }

    private fun closeEditor() {
        val root = overlayView as? FrameLayout ?: return
        cachedFeatures = featureViewModel.state.value.data
        cachedToolkit = toolkitViewModel.state.value.toPersistedData()
        captureActiveProfile()
        editorView?.let { editor ->
            editor.disposeComposition()
            root.removeView(editor)
        }
        editorView = null
        editorQuestId = null
        toolkitViewModel.setAppInForeground(false)
        featureViewModel.setAppInForeground(false)
        starViewModel.setAppInForeground(false)
        bubbleView?.visibility = View.GONE
        panelView?.visibility = View.VISIBLE
        overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        overlayParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        overlayParams.width = panelWidthPx()
        overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        overlayParams.x = panelX
        overlayParams.y = panelY
        clampOverlayPosition(overlayParams)
        windowManager.updateViewLayout(root, overlayParams)
        expandedStarId = null
        renderActiveModule()
    }

    private fun showProfileSwitcher(openedFromBubble: Boolean) {
        if (editorView != null || profileSwitcherView != null) return
        val root = overlayView as? FrameLayout ?: return
        activityProfiles = loadActivityProfiles()
        profileSwitcherOpenedFromBubble = openedFromBubble
        panelView?.visibility = View.GONE
        bubbleView?.visibility = View.GONE

        val switcher = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = panelBackground()
            elevation = dp(12).toFloat()
            alpha = overlaySettings.opacityPercent / 100f
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(
            textView("ACTIVITY PROFILES", 14f, Color.rgb(244, 201, 93)).apply {
                setTypeface(typeface, Typeface.BOLD)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        heading.addView(
            actionButton("×", "Close activity profiles") {
                hideProfileSwitcher()
            },
        )
        switcher.addView(heading)
        switcher.addView(
            textView(
                "Switch the whole Rune Companion setup",
                12f,
                Color.rgb(185, 201, 212),
            ).apply {
                setPadding(0, dp(4), 0, dp(8))
            },
        )
        val profileList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        activityProfiles.profiles.forEach { profile ->
            val active = profile.id == activityProfiles.activeProfileId
            profileList.addView(
                textView(
                    "${if (active) "●" else "○"}  ${profile.symbol}  ${profile.name}",
                    15f,
                    if (active) Color.rgb(110, 218, 208) else Color.WHITE,
                ).apply {
                    minHeight = dp(48)
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    contentDescription = if (active) {
                        "${profile.name}, active activity profile"
                    } else {
                        "Switch to ${profile.name}"
                    }
                    setOnClickListener {
                        activateActivityProfile(profile.id)
                    }
                },
            )
        }
        switcher.addView(
            ScrollView(this).apply {
                isFillViewport = false
                isVerticalScrollBarEnabled = true
                addView(
                    profileList,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (displayBounds().height() * PROFILE_SWITCHER_HEIGHT_FRACTION)
                    .toInt()
                    .coerceAtMost(dp(activityProfiles.profiles.size * 48)),
            ),
        )
        root.addView(
            switcher,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        profileSwitcherView = switcher
        overlayParams.width = dp(PROFILE_SWITCHER_WIDTH_DP)
        overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        clampOverlayPosition(overlayParams)
        windowManager.updateViewLayout(root, overlayParams)
    }

    private fun hideProfileSwitcher() {
        val root = overlayView as? FrameLayout ?: return
        profileSwitcherView?.let(root::removeView)
        profileSwitcherView = null
        if (profileSwitcherOpenedFromBubble) {
            bubbleView?.visibility = View.VISIBLE
            panelView?.visibility = View.GONE
            overlayParams.width = dp(BUBBLE_SIZE_DP)
            overlayParams.height = dp(BUBBLE_SIZE_DP)
            overlayParams.x = bubbleX ?: overlayParams.x
            overlayParams.y = bubbleY ?: overlayParams.y
        } else {
            bubbleView?.visibility = View.GONE
            panelView?.visibility = View.VISIBLE
            overlayParams.width = panelWidthPx()
            overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            overlayParams.x = panelX
            overlayParams.y = panelY
        }
        clampOverlayPosition(overlayParams)
        windowManager.updateViewLayout(root, overlayParams)
    }

    private fun activateActivityProfile(profileId: String) {
        if (profileId == activityProfiles.activeProfileId) {
            hideProfileSwitcher()
            return
        }
        captureActiveProfile()
        val selected = activityProfiles.select(profileId)
        activityProfiles = selected
        activityProfilePreferences.save(selected)
        val profile = selected.activeProfile
        personalizationPreferences.save(profile.personalization)
        overlaySettings = profile.overlay.normalized()
        overlayPreferences.save(overlaySettings)
        filterPreferences.save(
            profile.starFilters.applyTo(filterPreferences.load()),
        )
        val features = cachedFeatures
        val account = profile.selectedAccount?.takeIf { username ->
            features.accounts.any { it.username.equals(username, ignoreCase = true) }
        }
        if (features.selectedAccount != account) {
            cachedFeatures = features.copy(selectedAccount = account)
            serviceScope.launch(Dispatchers.IO) {
                featurePreferences.save(cachedFeatures)
            }
        }
        expandedStarId = null
        applyStoredPlacement(updateWindow = false)
        hideProfileSwitcher()
        updateBubble(overlaySettings.selectedModule, activeBadgeCount)
        renderActiveModule()
    }

    private fun loadActivityProfiles(): ActivityProfileState =
        activityProfilePreferences.load(
            legacyPersonalization = personalizationPreferences.load(),
            legacyOverlay = overlayPreferences.load(),
            selectedAccount = cachedFeatures.selectedAccount,
            legacyFilters = filterPreferences.load(),
        )

    private fun captureActiveProfile() {
        val filters = filterPreferences.load()
        activityProfiles = activityProfiles.updateActive { active ->
            active.copy(
                personalization = personalizationPreferences.load(),
                overlay = overlaySettings,
                selectedAccount = cachedFeatures.selectedAccount,
                starFilters = ActivityStarFilters.from(filters),
            )
        }
        activityProfilePreferences.save(activityProfiles)
    }

    private fun updateActiveProfile(
        transform: (ActivityProfile) -> ActivityProfile,
    ) {
        activityProfiles = loadActivityProfiles().updateActive(transform)
        activityProfilePreferences.save(activityProfiles)
    }

    private fun saveOverlaySettingsToActiveProfile() {
        overlaySettings = overlaySettings.normalized()
        overlayPreferences.save(overlaySettings)
        updateActiveProfile { it.copy(overlay = overlaySettings) }
        applyOverlayAppearance(useStoredPlacement = false)
    }

    private fun currentPlacement(): OverlayPlacement =
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            overlaySettings.landscapePlacement
        } else {
            overlaySettings.portraitPlacement
        }

    private fun withCurrentPlacement(placement: OverlayPlacement): OverlaySettings =
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            overlaySettings.copy(landscapePlacement = placement)
        } else {
            overlaySettings.copy(portraitPlacement = placement)
        }.normalized()

    private fun applyStoredPlacement(updateWindow: Boolean = true) {
        val root = overlayView as? FrameLayout ?: return
        val bounds = displayBounds()
        val placement = currentPlacement()
        val panelInset = panelSafeInsetPx()
        val panelMaxX =
            (bounds.width() - panelWidthPx() - panelInset * 2).coerceAtLeast(0)
        val panelMaxY =
            (bounds.height() - dp(MINIMUM_VISIBLE_HEIGHT_DP)).coerceAtLeast(0)
        val bubbleMaxX = (bounds.width() - dp(BUBBLE_SIZE_DP)).coerceAtLeast(0)
        val bubbleMaxY = (bounds.height() - dp(BUBBLE_SIZE_DP)).coerceAtLeast(0)
        panelX = panelInset + (placement.panelXFraction * panelMaxX).toInt()
        panelY = (placement.panelYFraction * panelMaxY).toInt()
        bubbleX = (placement.bubbleXFraction * bubbleMaxX).toInt()
        bubbleY = (placement.bubbleYFraction * bubbleMaxY).toInt()
        when {
            profileSwitcherView != null -> {
                overlayParams.width = dp(PROFILE_SWITCHER_WIDTH_DP)
            }
            bubbleView?.visibility == View.VISIBLE -> {
                overlayParams.width = dp(BUBBLE_SIZE_DP)
                overlayParams.height = dp(BUBBLE_SIZE_DP)
                overlayParams.x = requireNotNull(bubbleX)
                overlayParams.y = requireNotNull(bubbleY)
            }
            else -> {
                overlayParams.width = panelWidthPx()
                overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                overlayParams.x = panelX
                overlayParams.y = panelY
            }
        }
        clampOverlayPosition(overlayParams)
        if (updateWindow) windowManager.updateViewLayout(root, overlayParams)
    }

    private fun applyOverlayAppearance(useStoredPlacement: Boolean) {
        panelView?.alpha = overlaySettings.opacityPercent / 100f
        bubbleView?.alpha = overlaySettings.opacityPercent / 100f
        profileSwitcherView?.alpha = overlaySettings.opacityPercent / 100f
        if (editorView != null) return
        if (useStoredPlacement) {
            applyStoredPlacement()
            return
        }
        val root = overlayView as? FrameLayout ?: return
        if (
            panelView?.visibility == View.VISIBLE &&
            profileSwitcherView == null
        ) {
            overlayParams.width = panelWidthPx()
            overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            panelX = overlayParams.x
            panelY = overlayParams.y
        }
        clampOverlayPosition(overlayParams)
        windowManager.updateViewLayout(root, overlayParams)
    }

    private fun savePanelPlacement() {
        panelX = overlayParams.x
        panelY = overlayParams.y
        val bounds = displayBounds()
        val inset = panelSafeInsetPx()
        val maxX = (bounds.width() - panelWidthPx() - inset * 2).coerceAtLeast(1)
        val maxY = (bounds.height() - dp(MINIMUM_VISIBLE_HEIGHT_DP)).coerceAtLeast(1)
        val placement = currentPlacement().copy(
            panelXFraction = (panelX - inset).coerceAtLeast(0).toFloat() / maxX,
            panelYFraction = panelY.toFloat() / maxY,
        )
        overlaySettings = withCurrentPlacement(placement)
        saveOverlaySettingsToActiveProfile()
    }

    private fun saveBubblePlacement() {
        val x = bubbleX ?: return
        val y = bubbleY ?: return
        val bounds = displayBounds()
        val maxX = (bounds.width() - dp(BUBBLE_SIZE_DP)).coerceAtLeast(1)
        val maxY = (bounds.height() - dp(BUBBLE_SIZE_DP)).coerceAtLeast(1)
        val placement = currentPlacement().copy(
            bubbleXFraction = x.toFloat() / maxX,
            bubbleYFraction = y.toFloat() / maxY,
        )
        overlaySettings = withCurrentPlacement(placement)
        saveOverlaySettingsToActiveProfile()
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
            panelView?.visibility == View.VISIBLE -> panelWidthPx()
            else -> dp(BUBBLE_SIZE_DP)
        }
        val safeInset = if (
            panelView?.visibility == View.VISIBLE &&
            bubbleView?.visibility != View.VISIBLE &&
            editorView == null &&
            profileSwitcherView == null
        ) {
            panelSafeInsetPx()
        } else {
            0
        }
        params.x = params.x.coerceIn(
            safeInset,
            (bounds.width() - width - safeInset).coerceAtLeast(safeInset),
        )
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

    private fun snapPanelToEdge(params: WindowManager.LayoutParams) {
        val bounds = displayBounds()
        val inset = panelSafeInsetPx()
        val width = panelWidthPx()
        params.x = if (params.x + width / 2 < bounds.width() / 2) {
            inset
        } else {
            (bounds.width() - width - inset).coerceAtLeast(inset)
        }
        clampOverlayPosition(params)
        overlayView?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun panelWidthPx(): Int {
        val widthDp = if (
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        ) {
            overlaySettings.landscapeWidthDp
        } else {
            overlaySettings.compactWidthDp
        }
        val desired = dp(widthDp)
        return if (
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            overlaySettings.avoidGameControls
        ) {
            desired.coerceAtMost(
                (displayBounds().width() - dp(LANDSCAPE_CONTROL_INSET_DP) * 2)
                    .coerceAtLeast(dp(MINIMUM_SAFE_PANEL_WIDTH_DP)),
            )
        } else {
            desired
        }
    }

    private fun panelSafeInsetPx(): Int =
        if (
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            overlaySettings.avoidGameControls
        ) {
            dp(LANDSCAPE_CONTROL_INSET_DP)
        } else {
            0
        }

    private fun displayBounds(): android.graphics.Rect =
        windowManager.currentWindowMetrics.bounds

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
            minWidth = dp(48)
            minHeight = dp(48)
            isFocusable = true
            isClickable = true
            contentDescription = description
            val selectableBackground = TypedValue()
            if (
                theme.resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless,
                    selectableBackground,
                    true,
                )
            ) {
                setBackgroundResource(selectableBackground.resourceId)
            }
            setOnClickListener { action() }
        }

    private fun io.github.taxledgr.runecompanion.toolkit.ToolkitState.toPersistedData() =
        PersistedToolkitData(
            reminders = reminders,
            slayerTask = slayerTask,
            checklist = checklist,
            tripTimer = tripTimer,
            priceWatchlist = priceWatchlist,
            trackedPlayer = trackedPlayer,
        )

    private fun textView(label: String, size: Float, colour: Int) = TextView(this).apply {
        text = label
        textSize = size * overlayTextScale()
        setTextColor(colour)
        typeface = Typeface.create("sans", Typeface.NORMAL)
    }

    private fun overlayTextScale(): Float =
        (
            when (activityProfiles.activeProfile.personalization.overlayDensity) {
                LayoutDensity.COMPACT -> 0.9f
                LayoutDensity.COMFORTABLE -> 1f
                LayoutDensity.LARGE -> 1.12f
            }
            ) * (overlaySettings.textScalePercent / 100f)

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
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
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
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
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
        private const val PROFILE_SWITCHER_WIDTH_DP = 300
        private const val PROFILE_SWITCHER_HEIGHT_FRACTION = 0.62f
        private const val BUBBLE_SIZE_DP = 58
        private const val BUBBLE_EDGE_INSET_DP = 8
        private const val BUBBLE_DEFAULT_OFFSET_DP = 140
        private const val MINIMUM_VISIBLE_HEIGHT_DP = 72
        private const val LANDSCAPE_CONTROL_INSET_DP = 68
        private const val MINIMUM_SAFE_PANEL_WIDTH_DP = 220
        private const val EXPANDED_LIST_HEIGHT_DP = 430
        private const val OVERLAY_MAP_HEIGHT_DP = 180
        private const val EDITOR_WIDTH_FRACTION = 0.92f
        private const val EDITOR_HEIGHT_FRACTION = 0.88f

        private val _running = MutableStateFlow(false)
        val running = _running.asStateFlow()

        fun reloadIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java).setAction(ACTION_RELOAD)
    }
}

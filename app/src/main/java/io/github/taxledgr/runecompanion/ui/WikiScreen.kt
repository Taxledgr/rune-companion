package io.github.taxledgr.runecompanion.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.taxledgr.runecompanion.util.AppUserAgent

@Composable
fun WikiPortalScreen(onOpenArticle: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val sections = listOf(
        "Getting around" to listOf(
            "Transportation",
            "Fairy rings",
            "Teleportation spells",
            "Shooting Stars",
        ),
        "Account progress" to listOf(
            "Optimal quest guide",
            "Achievement Diary",
            "Skill training guides",
            "Combat Achievements",
        ),
        "Combat" to listOf(
            "Boss",
            "Slayer training",
            "Equipment Stats",
            "Drop table",
        ),
        "Activities" to listOf(
            "Treasure Trails/Full guide",
            "Farming training",
            "Minigames",
            "Money making guide",
        ),
        "Items and economy" to listOf(
            "Grand Exchange",
            "RuneScape:Real-time Prices",
            "Calculator:Herblore",
            "Calculator:Skill calculators",
        ),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "IN-APP REFERENCE LIBRARY",
                title = "OSRS Wiki",
                subtitle = "Search and read the complete current Wiki without leaving Rune Companion.",
            )
        }
        item {
            Card {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search every OSRS Wiki article") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onOpenArticle(wikiSearchUrl(query)) },
                        enabled = query.isNotBlank(),
                    ) { Text("Search in app") }
                    Text(
                        "Articles are loaded from oldschool.runescape.wiki so information stays " +
                            "current. Rune Companion does not copy or modify the Wiki.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        sections.forEach { (heading, articles) ->
            item {
                Text(heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Card {
                    Column(Modifier.padding(10.dp)) {
                        articles.forEach { article ->
                            OutlinedButton(
                                onClick = { onOpenArticle(wikiUrl(article)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(article) }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "OSRS Wiki content is provided by its contributors under CC BY-NC-SA 3.0; " +
                    "individual media may have additional terms. RuneScape and Old School " +
                    "RuneScape are trademarks of Jagex.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun WikiReaderScreen(
    initialUrl: String,
    onClose: () -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var title by remember { mutableStateOf("OSRS Wiki") }
    var currentUrl by remember(initialUrl) { mutableStateOf(normalizeWikiUrl(initialUrl)) }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onClose()
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { webView?.goBack() },
                    enabled = webView?.canGoBack() == true,
                ) { Text("‹") }
                OutlinedButton(
                    onClick = { webView?.goForward() },
                    enabled = webView?.canGoForward() == true,
                ) { Text("›") }
                Text(
                    title,
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(onClick = onClose) { Text("Close") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search Wiki") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        currentUrl = wikiSearchUrl(query)
                        webView?.loadUrl(currentUrl)
                    },
                    enabled = query.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Go") }
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.rgb(18, 25, 30))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.safeBrowsingEnabled = true
                    settings.userAgentString =
                        "${settings.userAgentString} ${AppUserAgent.value}"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val url = request.url.toString()
                            return if (isWikiUrl(url)) {
                                false
                            } else {
                                onOpenExternal(url)
                                true
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            currentUrl = url
                            title = view.title?.removeSuffix(" - OSRS Wiki") ?: "OSRS Wiki"
                        }
                    }
                    loadUrl(currentUrl)
                    webView = this
                }
            },
            update = { view ->
                if (view.url != currentUrl) view.loadUrl(currentUrl)
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                destroy()
            }
        }
    }
}

fun isWikiUrl(url: String): Boolean =
    runCatching {
        val host = Uri.parse(url).host.orEmpty().lowercase()
        host == WIKI_HOST || host.endsWith(".$WIKI_HOST")
    }.getOrDefault(false)

private fun normalizeWikiUrl(url: String): String =
    if (isWikiUrl(url)) url else wikiUrl("Old School RuneScape Wiki")

private const val WIKI_HOST = "oldschool.runescape.wiki"

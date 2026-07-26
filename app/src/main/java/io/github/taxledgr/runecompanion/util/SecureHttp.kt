package io.github.taxledgr.runecompanion.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal fun openTrustedHttpsConnection(
    url: String,
    allowedHosts: Set<String>,
): HttpsURLConnection {
    val trustedUrl = TrustedUrlPolicy.normalizeHttpsUrl(url, allowedHosts)
        ?: throw IOException("Blocked an untrusted network destination")
    return (URL(trustedUrl).openConnection() as HttpsURLConnection).apply {
        instanceFollowRedirects = false
        useCaches = false
    }
}

internal fun HttpsURLConnection.readUtf8Response(maxBytes: Int): String {
    val declaredLength = contentLengthLong
    if (declaredLength > maxBytes) {
        throw IOException("Network response exceeded the safety limit")
    }
    return inputStream.use { stream -> stream.readUtf8Limited(maxBytes) }
}

internal fun InputStream.readUtf8Limited(maxBytes: Int): String {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) {
            throw IOException("Network response exceeded the safety limit")
        }
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}

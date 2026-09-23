package com.cake.freshenda.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class UpdateMetadataUnavailable : IOException()

class UpdateChecker(
    private val openConnection: (URL) -> HttpsURLConnection = { it.openConnection() as HttpsURLConnection },
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        var url = URL(METADATA_URL)
        // GitHub 附件会重定向到下载域名；全程只允许 HTTPS，限制跳转次数和响应大小。
        repeat(6) {
            ensureActive()
            val connection = openConnection(url)
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "Freshenda-UpdateChecker")
                when (val status = connection.responseCode) {
                    200 -> {
                        val bytes = ByteArrayOutputStream()
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(4_096)
                            while (true) {
                                ensureActive()
                                val count = input.read(buffer)
                                if (count == -1) break
                                if (bytes.size() + count > MAX_BYTES) throw IOException("Update metadata too large")
                                bytes.write(buffer, 0, count)
                            }
                        }
                        return@withContext json.decodeFromString<UpdateInfo>(bytes.toString(Charsets.UTF_8.name())).validate()
                    }
                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: throw IOException("Missing redirect")
                        val next = URL(url, location)
                        if (next.protocol != "https" || next.userInfo != null) throw IOException("Invalid redirect")
                        url = next
                    }
                    404 -> throw UpdateMetadataUnavailable()
                    else -> throw IOException("Update HTTP $status")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    companion object {
        const val METADATA_URL = "https://github.com/Holo-Spice/Freshenda/releases/latest/download/update.json"
        private const val MAX_BYTES = 128 * 1_024
    }
}

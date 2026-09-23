package com.cake.freshenda

import com.cake.freshenda.update.UpdateChecker
import com.cake.freshenda.update.UpdateInfo
import com.cake.freshenda.update.UpdateMetadataUnavailable
import com.cake.freshenda.update.UpdatePolicy
import com.cake.freshenda.update.UpdateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class UpdateTest {
    private val info = UpdateInfo(1, 5, "1.1.1", 26,
        "https://github.com/Holo-Spice/Freshenda/releases/tag/v1.1.1", listOf("修复日期刷新"))

    @Test fun versionCodeControlsOrderingEvenWhenNamesSuggestOtherwise() {
        assertEquals(UpdateResult.AVAILABLE, evaluate(info.copy(versionName = "0.1")))
        assertEquals(UpdateResult.UP_TO_DATE, evaluate(info.copy(versionCode = 4)))
        assertEquals(UpdateResult.UP_TO_DATE, evaluate(info.copy(versionCode = 3, versionName = "9.0")))
    }

    @Test fun minimumSdkBlocksIncompatibleUpdates() {
        assertEquals(UpdateResult.INCOMPATIBLE, evaluate(info.copy(minSdk = 38)))
        assertEquals(UpdateResult.AVAILABLE, evaluate(info.copy(minSdk = 37)))
    }

    @Test fun manualCheckOverridesOnlyTheIgnoredVersion() {
        assertEquals(UpdateResult.IGNORED, evaluate(info, ignored = 5))
        assertEquals(UpdateResult.AVAILABLE, evaluate(info, ignored = 5, manual = true))
        assertEquals(UpdateResult.AVAILABLE, evaluate(info.copy(versionCode = 6), ignored = 5))
    }

    @Test fun automaticCheckHandlesFirstRunDayBoundaryAndClockRollback() {
        val day = 86_400_000L
        assertTrue(UpdatePolicy.shouldCheck(day, 0))
        assertFalse(UpdatePolicy.shouldCheck(day * 2 - 1, day))
        assertTrue(UpdatePolicy.shouldCheck(day * 2, day))
        assertTrue(UpdatePolicy.shouldCheck(day - 1, day))
    }

    @Test fun invalidMetadataAndForeignDownloadPagesAreRejected() {
        listOf(
            info.copy(schemaVersion = 2),
            info.copy(versionCode = 0),
            info.copy(minSdk = 0),
            info.copy(versionName = ""),
            info.copy(releaseUrl = "http://github.com/Holo-Spice/Freshenda/releases/tag/v1"),
            info.copy(releaseUrl = "https://github.com.evil.test/Holo-Spice/Freshenda/releases/tag/v1"),
            info.copy(releaseUrl = "https://github.com/other/app/releases/tag/v1"),
            info.copy(releaseUrl = "https://github.com/Holo-Spice/Freshenda/releases/tag/"),
        ).forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { invalid.validate() } }
    }

    @Test fun followsHttpsRedirectAndClosesConnections() = runBlocking {
        val first = FakeConnection(302, location = "https://release-assets.githubusercontent.com/update.json")
        val second = FakeConnection(200, Json.encodeToString(info))
        val queue = ArrayDeque(listOf(first, second))
        val urls = mutableListOf<String>()
        val checker = UpdateChecker { url -> urls += url.toString(); queue.removeFirst() }
        assertEquals(info, checker.check())
        assertEquals(UpdateChecker.METADATA_URL, urls.first())
        assertTrue(urls.last().startsWith("https://release-assets.githubusercontent.com/"))
        assertTrue(first.closed && second.closed)
        assertEquals(8_000, second.readTimeout)
    }

    @Test fun missingMetadataIsNotReportedAsLatest() {
        assertThrows(UpdateMetadataUnavailable::class.java) {
            runBlocking { UpdateChecker { FakeConnection(404) }.check() }
        }
    }

    @Test fun failedHttpAndTimeoutRemainFailures() {
        assertThrows(IOException::class.java) {
            runBlocking { UpdateChecker { FakeConnection(503) }.check() }
        }
        assertThrows(SocketTimeoutException::class.java) {
            runBlocking { UpdateChecker { throw SocketTimeoutException() }.check() }
        }
    }

    @Test fun rejectsMalformedAndOversizedResponses() {
        listOf("<html>error</html>", "x".repeat(128 * 1_024 + 1)).forEach { body ->
            val connection = FakeConnection(200, body)
            assertThrows(Exception::class.java) { runBlocking { UpdateChecker { connection }.check() } }
            assertTrue(connection.closed)
        }
    }

    @Test fun rejectsHttpDowngradeAndRedirectLoops() {
        val downgrade = FakeConnection(302, location = "http://example.com/update.json")
        assertThrows(IOException::class.java) { runBlocking { UpdateChecker { downgrade }.check() } }
        var count = 0
        assertThrows(IOException::class.java) {
            runBlocking {
                UpdateChecker { count++; FakeConnection(302, location = "/loop") }.check()
            }
        }
        assertEquals(6, count)
        assertTrue(downgrade.closed)
    }

    @Test fun unknownJsonFieldsDoNotBreakForwardCompatibility() = runBlocking {
        val body = Json.encodeToString(info).dropLast(1) + ",\"futureField\":true}"
        assertEquals(info, UpdateChecker { FakeConnection(200, body) }.check())
    }

    @Test fun cancellationIsNotConvertedToAnUpdateResult() {
        assertThrows(CancellationException::class.java) {
            runBlocking { UpdateChecker { throw CancellationException() }.check() }
        }
    }

    private fun evaluate(candidate: UpdateInfo, ignored: Long = 0, manual: Boolean = false) =
        UpdatePolicy.evaluate(candidate, 4, 37, ignored, manual)

    private class FakeConnection(
        private val status: Int,
        private val body: String = "",
        private val location: String? = null,
    ) : HttpsURLConnection(URL("https://github.com")) {
        var closed = false
        override fun getResponseCode() = status
        override fun getHeaderField(name: String): String? = if (name == "Location") location else null
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
        override fun connect() = Unit
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getCipherSuite() = "TLS_FAKE"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}

package com.cake.freshenda.update

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class UpdateInfo(
    val schemaVersion: Int,
    val versionCode: Long,
    val versionName: String,
    val minSdk: Int,
    val releaseUrl: String,
    val notes: List<String> = emptyList(),
) {
    fun validate(): UpdateInfo {
        require(schemaVersion == 1 && versionCode > 0 && minSdk > 0)
        require(versionName.isNotBlank() && versionName.length <= 80)
        require(notes.size <= 50 && notes.all { it.length <= 2_000 })
        val uri = URI(releaseUrl)
        require(uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 && uri.userInfo == null)
        require(uri.rawPath.startsWith("/Holo-Spice/Freshenda/releases/tag/") &&
            uri.rawPath.removePrefix("/Holo-Spice/Freshenda/releases/tag/").isNotBlank())
        require(uri.query == null && uri.fragment == null)
        return this
    }
}

data class UpdatePreferences(
    val lastCheckEpochMillis: Long = 0,
    val ignoredVersionCode: Long = 0,
)

enum class UpdateResult { AVAILABLE, UP_TO_DATE, INCOMPATIBLE, IGNORED }

object UpdatePolicy {
    private const val CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1_000L

    fun shouldCheck(now: Long, lastCheck: Long): Boolean =
        lastCheck <= 0 || now < lastCheck || now - lastCheck >= CHECK_INTERVAL_MILLIS

    fun evaluate(info: UpdateInfo, installedVersion: Long, sdk: Int, ignoredVersion: Long, manual: Boolean): UpdateResult = when {
        info.versionCode <= installedVersion -> UpdateResult.UP_TO_DATE
        info.minSdk > sdk -> UpdateResult.INCOMPATIBLE
        !manual && info.versionCode == ignoredVersion -> UpdateResult.IGNORED
        else -> UpdateResult.AVAILABLE
    }
}

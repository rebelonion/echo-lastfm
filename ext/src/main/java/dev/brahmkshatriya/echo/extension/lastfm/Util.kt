package dev.brahmkshatriya.echo.extension.lastfm

import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.config.BuildConfig
import dev.brahmkshatriya.echo.extension.LastFMAPI.Companion.PLUGIN_IDENTIFIER
import dev.brahmkshatriya.echo.extension.LastFMAPI.Companion.SAVED_SCROBBLES_SETTINGS_KEY
import dev.brahmkshatriya.echo.extension.Scrobble
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Logs a message to the console if the app is in debug mode.
 * @param message The message to log.
 */
fun log(message: String) {
    if (BuildConfig.isDebug()) {
        println("$PLUGIN_IDENTIFIER: $message")
    }
}

/**
 * converts an object to a list of itself.
 * The exact same as calling listOf(this) but looks cleaner.
 * @return A list containing the object.
 */
fun <T> T.listOf(): List<T> = listOf(this)

/**
 * Checks if an object is null.
 * @return True if the object is null, false otherwise.
 */
fun <T> T?.isNull(): Boolean = this == null


fun Settings.addScrobble(
    scrobble: Scrobble
) {
    val scrobbles: MutableList<Scrobble> =
        this.getString(SAVED_SCROBBLES_SETTINGS_KEY)?.toScrobbles()?.toMutableList()
            ?: mutableListOf()
    if (!scrobbles.any { it.artist == scrobble.artist && it.track == scrobble.track && it.addedAt == scrobble.addedAt }) {
        scrobbles.add(scrobble)
    }
    this.putString(SAVED_SCROBBLES_SETTINGS_KEY, scrobbles.toJsonString())
}

fun Settings.getScrobbles(): List<Scrobble> {
    return this.getString(SAVED_SCROBBLES_SETTINGS_KEY)?.toScrobbles() ?: emptyList()
}

fun Settings.removeScrobble(scrobble: Scrobble) {
    val scrobbles: MutableList<Scrobble> =
        this.getString(SAVED_SCROBBLES_SETTINGS_KEY)?.toScrobbles()?.toMutableList()
            ?: mutableListOf()
    scrobbles.remove(scrobble)
    this.putString(SAVED_SCROBBLES_SETTINGS_KEY, scrobbles.toJsonString())
}

fun List<Scrobble>.toJsonString(): String{
    return Json.encodeToString(this)
}

fun String.toScrobbles(): List<Scrobble> {
    return Json.decodeFromString(this)
}
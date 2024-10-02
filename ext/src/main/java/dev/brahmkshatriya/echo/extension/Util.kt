package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.config.BuildConfig
import dev.brahmkshatriya.echo.extension.LastFMAPI.Companion.PLUGIN_IDENTIFIER
import org.json.JSONObject

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

/**
 * Converts a string to a JSONObject.
 * @return The JSONObject.
 */
fun String.toJSONObject() = JSONObject(this)
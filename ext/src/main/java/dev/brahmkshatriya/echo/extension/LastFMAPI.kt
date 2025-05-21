package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.config.BuildConfig
import dev.brahmkshatriya.echo.extension.lastfm.UrlBuilder.Companion.generateUrlWithSig
import dev.brahmkshatriya.echo.extension.lastfm.UrlBuilder.Companion.urlBuilder
import dev.brahmkshatriya.echo.extension.lastfm.addScrobble
import dev.brahmkshatriya.echo.extension.lastfm.isNull
import dev.brahmkshatriya.echo.extension.lastfm.log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit.SECONDS

@Serializable
data class Scrobble(
    val artist: String,
    val track: String,
    val timestamp: Long,
    val album: String? = null,
    val addedAt: Long = System.currentTimeMillis() / 1000 / 60, // Only save one track per minute
)

class LastFMAPI {
    private var user: User? = null
    private var settings: Settings? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, SECONDS)
        .readTimeout(10, SECONDS)
        .writeTimeout(10, SECONDS).build()

    fun sendNowPlaying(scrobble: Scrobble) {
        if (getSessionKey().isNullOrBlank()) throw loginRequiredException()
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["artist"] = scrobble.artist
        parameters["track"] = scrobble.track
        if (!scrobble.album.isNullOrBlank()) {
            parameters["album"] = scrobble.album
        }
        val method = "track.updateNowPlaying"
        val url = generateUrlWithSig(method, getSessionKey(), parameters)
        val (code, body) = try{
            sendRequest(url)
        } catch (e: IOException) { // We're going to assume this means no internet
            log("Network error: ${e.message}")
            return // Hiding it so user doesn't get spammed with errors when offline
        }
        if (code.isNull()) {
            log(body)
        } else {
            log("Now Playing Error: Unexpected code $code")
            log(body)
            throw Exception("Now Playing Error $code: $body")
        }
    }

    fun sendScrobble(scrobble: Scrobble): Boolean {
        if (getSessionKey().isNullOrBlank()) throw loginRequiredException()
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["artist"] = scrobble.artist
        parameters["track"] = scrobble.track
        parameters["timestamp"] = scrobble.timestamp.toString()
        if (!scrobble.album.isNullOrBlank()) {
            parameters["album"] = scrobble.album
        }
        val method = "track.scrobble"
        val url = generateUrlWithSig(method, getSessionKey(), parameters)
        val (code, body) = try {
            sendRequest(url)
        } catch (e: IOException) {
            log("Network error: ${e.message}")
            settings?.addScrobble(scrobble)
            return false
        }
        if (code.isNull()) {
            log(body)
        } else {
            log("Scrobble Error: Unexpected code $code")
            log(body)
            throw Exception("Scrobble Error $code}: $body")
        }
        return true
    }

    fun login(username: String, password: String): User {
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["username"] = username
        parameters["password"] = password
        val method = "auth.getMobileSession"
        val url = generateUrlWithSig(method, null, parameters)
        val (code, body) = sendRequest(url)
        val json = Json { ignoreUnknownKeys = true }
        val jsonObject = json.parseToJsonElement(body).jsonObject

        return if (code.isNull()) {
            log(body)
            val sessionJson = jsonObject["session"]?.jsonObject
                ?: throw Exception("Session data not found")

            val name = sessionJson["name"]?.jsonPrimitive?.contentOrNull
                ?: throw Exception("Name not found in session")

            val key = sessionJson["key"]?.jsonPrimitive?.contentOrNull
                ?: throw Exception("Key not found in session")

            val image = getPFP(name)
            User(key, name, image?.toImageHolder())
        } else {
            log("Unexpected code $code")
            log(body)
            val errorCode = jsonObject["error"]?.jsonPrimitive?.intOrNull
                ?: throw Exception("Error code not found")

            val message = jsonObject["message"]?.jsonPrimitive?.contentOrNull
                ?: throw Exception("Error message not found")

            throw Exception("Error $errorCode: $message")
        }
    }

    private fun getPFP(username: String): String? {
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["user"] = username
        parameters["api_key"] = getApiKey()
        val url = urlBuilder("user.getinfo", parameters)
        val (_, body) = sendRequest(url)
        val json = Json { ignoreUnknownKeys = true }
        val userObject = json.parseToJsonElement(body).jsonObject["user"]?.jsonObject
            ?: throw Exception("User data not found")
        val images = userObject["image"]?.jsonArray
            ?: throw Exception("Image data not found")

        var mediumImage: String? = null
        var largeImage: String? = null

        for (i in images.indices) {
            val imageObject = images[i].jsonObject
            val size = imageObject["size"]?.jsonPrimitive?.contentOrNull
                ?: throw Exception("Size not found in image data")
            val imUrl = imageObject["#text"]?.jsonPrimitive?.contentOrNull
                ?: throw Exception("Image URL not found in image data")

            when (size) {
                "medium" -> mediumImage = imUrl
                "large" -> largeImage = imUrl
            }
        }

        return mediumImage ?: largeImage
    }

    fun updateUser(user: User?) {
        this.user = user
    }

    fun getUser(): User? {
        return user
    }

    fun updateSettings(settings: Settings?) {
        this.settings = settings
    }

    private fun getSessionKey(): String? {
        return user?.id
    }

    private fun buildRequest(url: String, body: RequestBody? = null): Request {
        val sendBody = body ?: byteArrayOf().toRequestBody(null, 0, 0)
        return Request.Builder().url(url).post(sendBody).header("User-Agent", USER_AGENT).build()
    }

    private fun sendRequest(url: String, body: RequestBody? = null): Pair<Int?, String> {
        val request = buildRequest(url, body)
        val response = client.newCall(request).execute()
        val responseBody = response.body.string()
        val code = if (!response.isSuccessful)
            response.code
        else
            null
        response.close()
        return Pair(code, responseBody)
    }

    private fun loginRequiredException() =
        ClientException.LoginRequired()

    companion object {
        const val PLUGIN_IDENTIFIER = "Echo-Lastfm-Plugin"
        const val SAVED_SCROBBLES_SETTINGS_KEY = "saved_scrobbles"
        private val USER_AGENT =
            "$PLUGIN_IDENTIFIER/${BuildConfig.versionCode()} (${System.getProperty("os.name")}:${System.getProperty("os.version")})"

        fun getApiKey(): String {
            return BuildConfig.getK()
        }

        fun getSecret(): String {
            return BuildConfig.getScrt()
        }
    }
}
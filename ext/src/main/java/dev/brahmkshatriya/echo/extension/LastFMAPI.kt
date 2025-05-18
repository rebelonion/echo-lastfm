package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.config.BuildConfig
import dev.brahmkshatriya.echo.extension.lastfm.generateUrlWithSig
import dev.brahmkshatriya.echo.extension.lastfm.isNull
import dev.brahmkshatriya.echo.extension.lastfm.log
import dev.brahmkshatriya.echo.extension.lastfm.toJSONObject
import dev.brahmkshatriya.echo.extension.lastfm.urlBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit.SECONDS

class LastFMAPI {
    private var user: User? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, SECONDS)
        .readTimeout(10, SECONDS)
        .writeTimeout(10, SECONDS).build()

    fun sendNowPlaying(track: String, artist: String, album: String?) {
        if (getSessionKey().isNullOrBlank()) throw loginRequiredException()
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["artist"] = artist
        parameters["track"] = track
        if (!album.isNullOrBlank()) {
            parameters["album"] = album
        }
        val method = "track.updateNowPlaying"
        val url = generateUrlWithSig(method, getSessionKey(), parameters)
        val (code, body) = sendRequest(url)
        if (code.isNull()) {
            log(body)
        } else {
            log("Now Playing Error: Unexpected code $code")
            log(body)
            throw Exception("Now Playing Error $code: $body")
        }
    }

    fun sendScrobble(timestamp: Long, track: String, artist: String, album: String?) {
        if (getSessionKey().isNullOrBlank()) throw loginRequiredException()
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["artist"] = artist
        parameters["track"] = track
        parameters["timestamp"] = timestamp.toString()
        if (!album.isNullOrBlank()) {
            parameters["album"] = album
        }
        val method = "track.scrobble"
        val url = generateUrlWithSig(method, getSessionKey(), parameters)
        val (code, body) = sendRequest(url)
        if (code.isNull()) {
            log(body)
        } else {
            log("Scrobble Error: Unexpected code $code")
            log(body)
            throw Exception("Scrobble Error $code}: $body")
        }
    }

    fun login(username: String, password: String): User {
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["username"] = username
        parameters["password"] = password
        val method = "auth.getMobileSession"
        val url = generateUrlWithSig(method, null, parameters)
        val (code, body) = sendRequest(url)
        val jsonObject = body.toJSONObject()
        return if (code.isNull()) {
            log(body)
            val sessionJson = jsonObject.getJSONObject("session")
            val name = sessionJson.getString("name")
            val key = sessionJson.getString("key")
            val image = getPFP(name)
            User(key, name, image?.toImageHolder())
        } else {
            log("Unexpected code $code")
            log(body)
            val errorCode = jsonObject.getInt("error")
            val message = jsonObject.getString("message")
            throw Exception("Error $errorCode: $message")
        }
    }

    private fun getPFP(username: String): String? {
        val parameters: MutableMap<String, String> = mutableMapOf()
        parameters["user"] = username
        parameters["api_key"] = getApiKey()
        val url = urlBuilder("user.getinfo", parameters)
        val (_, body) = sendRequest(url)
        val userObject = body.toJSONObject().getJSONObject("user")
        val images = userObject.getJSONArray("image")

        var mediumImage: String? = null
        var largeImage: String? = null

        for (i in 0 until images.length()) {
            val imageObject = images.getJSONObject(i)
            val size = imageObject.getString("size")
            val imUrl = imageObject.getString("#text")

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
        private val USER_AGENT =
            "$PLUGIN_IDENTIFIER/${BuildConfig.versionCode()} (${System.getProperty("os.name")}:${System.getProperty("os.version")})"

        fun getApiKey(): String {
            return BuildConfig.getKey()
        }

        fun getSecret(): String {
            return BuildConfig.getSecret()
        }
    }
}
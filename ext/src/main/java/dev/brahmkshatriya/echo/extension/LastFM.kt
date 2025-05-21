package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.lastfm.getScrobbles
import dev.brahmkshatriya.echo.extension.lastfm.listOf
import dev.brahmkshatriya.echo.extension.lastfm.removeScrobble
import java.io.IOException

@Suppress("unused")
class LastFM : ExtensionClient, LoginClient.CustomInput, TrackerClient {
    private val api = LastFMAPI()

    override val markAsPlayedDuration: Long
        get() = 60 * 1000 // 1 minute

    override suspend fun onExtensionSelected() {}

    override suspend fun onMarkAsPlayed(details: TrackDetails) {
        val timestamp = System.currentTimeMillis() / 1000 - 30
        var artists = details.track.artists.joinToString(",") { it.name }
        if (artists.isBlank() && details.track.title.isBlank()) return
        if (artists.isBlank()) artists = details.track.title
        val success = api.sendScrobble(
            Scrobble(
                artist = artists,
                track = details.track.title,
                timestamp = timestamp,
                album = details.track.album?.title
            )
        )
        if (success) {
            val scrobbles = setting.getScrobbles()
            if (scrobbles.isNotEmpty()) {
                for (scrobble in scrobbles) {
                    var shouldRemove = false
                    try {
                        api.sendScrobble(scrobble)
                        shouldRemove = true
                    } catch (e: Exception) {
                        if (e !is IOException) {
                            shouldRemove = true
                        }
                    }

                    if (shouldRemove) {
                        setting.removeScrobble(scrobble)
                    }
                }
            }

        }
    }

    override suspend fun onTrackChanged(details: TrackDetails?) {
        val title = details?.track?.title ?: return
        var artists = details.track.artists.joinToString(",") { it.name }
        if (artists.isBlank()) artists = title
        api.sendNowPlaying(
            Scrobble(
                artist = artists,
                track = title,
                timestamp = System.currentTimeMillis() / 1000 - 30,
                album = details.track.album?.title
            )
        )
    }

    override suspend fun getCurrentUser(): User? {
        return api.getUser()
    }

    override val forms: List<LoginClient.Form>
        get() = LoginClient.Form(
            key = "login",
            label = "Login",
            icon = LoginClient.InputField.Type.Username,
            inputFields = listOf(
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Username,
                    key = "username",
                    label = "Username (not email)",
                    isRequired = true,
                ),
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Password,
                    key = "password",
                    label = "Password",
                    isRequired = true,
                )
            )
        ).listOf()

    override suspend fun onLogin(key: String, data: Map<String, String?>): List<User> {
        val username = data["username"] ?: return emptyList()
        val password = data["password"] ?: return emptyList()
        val user = api.login(username, password)
        api.updateUser(user)
        return user.listOf()
    }

    override suspend fun onSetLoginUser(user: User?) {
        if (user?.id == null) {
            api.updateUser(null)
        } else {
            api.updateUser(user)
        }
    }

    override val settingItems: List<Setting> = emptyList()

    private lateinit var setting: Settings
    override fun setSettings(settings: Settings) {
        setting = settings
        api.updateSettings(settings)
    }

}
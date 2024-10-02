package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

class LastFM : ExtensionClient, LoginClient.UsernamePassword, TrackerClient {
    private val api = LastFMAPI()

    override suspend fun onExtensionSelected() {}

    override suspend fun onMarkAsPlayed(clientId: String, context: EchoMediaItem?, track: Track) {
        val timestamp = System.currentTimeMillis() / 1000 - 30
        var artists = track.artists.joinToString(",") { it.name }
        if (artists.isBlank() && track.title.isBlank()) return
        if (artists.isBlank()) artists = track.title
        api.sendScrobble(timestamp, track.title, artists, track.album?.title)
    }

    override suspend fun onStartedPlaying(clientId: String, context: EchoMediaItem?, track: Track) {
        var artists = track.artists.joinToString(",") { it.name }
        if (artists.isBlank() && track.title.isBlank()) return
        if (artists.isBlank()) artists = track.title
        api.sendNowPlaying(track.title, artists, track.album?.title)
    }

    override suspend fun onStoppedPlaying(
        clientId: String,
        context: EchoMediaItem?,
        track: Track
    ) {
    }

    override suspend fun getCurrentUser(): User? {
        return api.getUser()
    }

    override suspend fun onLogin(username: String, password: String): List<User> {
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
    }

}
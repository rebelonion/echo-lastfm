package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.clients.TrackerMarkClient
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingSlider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.lastfm.SettingData
import dev.brahmkshatriya.echo.extension.lastfm.fixMojibake
import dev.brahmkshatriya.echo.extension.lastfm.getScrobbles
import dev.brahmkshatriya.echo.extension.lastfm.listOf
import dev.brahmkshatriya.echo.extension.lastfm.removeScrobble
import java.io.IOException

@Suppress("unused")
class LastFM : ExtensionClient, LoginClient.CustomInput, TrackerClient, TrackerMarkClient {
    private val api = LastFMAPI()

    override suspend fun getMarkAsPlayedDuration(details: TrackDetails): Long? {
        details.track.duration?.let {
            if (it <= 0) return MARK_AS_PLAYED_DURATION_SETTING.default * 1000L
            return markAsPlayedDuration(it)
        }
        return MARK_AS_PLAYED_DURATION_SETTING.default * 1000L
    }

    override suspend fun onExtensionSelected() {}

    override suspend fun onPlayingStateChanged(details: TrackDetails?, isPlaying: Boolean) {

    }

    override suspend fun onMarkAsPlayed(details: TrackDetails) {
        val timestamp = System.currentTimeMillis() / 1000 - 30
        var artists = details.getArtistString().fixMojibake()
        if (artists.isBlank() && details.track.title.isBlank()) return
        if (artists.isBlank()) artists = details.track.title.fixMojibake()
        val success = api.sendScrobble(
            Scrobble(
                artist = artists,
                track = details.track.title.fixMojibake(),
                timestamp = timestamp,
                album = details.track.album?.title?.fixMojibake()
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
        if (!isNowPlayingEnabled()) return
        val title = details?.track?.title?.fixMojibake() ?: return
        var artists = details.getArtistString().fixMojibake()
        if (artists.isBlank()) artists = title
        api.sendNowPlaying(
            Scrobble(
                artist = artists,
                track = title,
                timestamp = System.currentTimeMillis() / 1000 - 30,
                album = details.track.album?.title?.fixMojibake()
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

    override fun setLoginUser(user: User?) {
        if (user?.id == null) {
            api.updateUser(null)
        } else {
            api.updateUser(user)
        }
    }

    override suspend fun getSettingItems(): List<Setting> {
        return listOf(
            SettingSlider(
                key = MARK_AS_PLAYED_DURATION_SETTING.key,
                title = "Mark as Played Duration (in seconds)",
                summary = "Duration after which a track is marked as played",
                defaultValue = 60,
                from = 10,
                to = 300,
                steps = 10
            ),
            SettingSlider(
                key = MARK_AS_PLAYED_PERCENTAGE_SETTING.key,
                title = "Mark as Played Percentage",
                summary = "Percentage of track duration after which a track is marked as played",
                defaultValue = 50,
                from = 20,
                to = 100,
            ),
            SettingSwitch(
                key = ONLY_USE_FIRST_ARTIST_SETTING.key,
                title = "Only use first artist",
                summary = "If a track has multiple artists, only the first one will be used",
                defaultValue = true
            ),
            SettingSwitch(
                key = ENABLE_NOW_PLAYING_SETTING.key,
                title = "Enable Now Playing",
                summary = "Send Now Playing updates to Last.fm",
                defaultValue = true
            ),
            SettingTextInput(
                key = ARTIST_EXCLUSION_LIST_SETTING.key,
                title = "Artist Exclusion List",
                summary = "Semicolon separated list of artists that will never be split even if they contain '&'. Example: Abor & Tynna; Paola & Chiara",
                defaultValue = ""
            )
        )
    }

    private lateinit var setting: Settings
    override fun setSettings(settings: Settings) {
        setting = settings
        api.updateSettings(settings)
    }

    fun markAsPlayedDuration(songDuration: Long): Long {
        val durationSetting = MARK_AS_PLAYED_DURATION_SETTING.get(setting)
        val percentageSetting = MARK_AS_PLAYED_PERCENTAGE_SETTING.get(setting)
        val percentageDuration = (songDuration * percentageSetting) / 100
        return minOf(durationSetting * 1000L, percentageDuration)
    }

    fun TrackDetails.getArtistString(): String {
        val exclusionList = ARTIST_EXCLUSION_LIST_SETTING.get(setting)
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { ARTIST_EXCLUSION_LIST_DEFAULT }

        val fullArtist = this.track.artists.joinToString(" & ") { it.name }
        if (exclusionList.any { it.equals(fullArtist, ignoreCase = true) }) {
            return fullArtist
        }

        return if (ONLY_USE_FIRST_ARTIST_SETTING.get(setting)) {
            this.track.artists.firstOrNull()?.name ?: ""
        } else {
            this.track.artists.joinToString(",") { it.name }
        }
    }

    fun isNowPlayingEnabled(): Boolean {
        return ENABLE_NOW_PLAYING_SETTING.get(setting)
    }

    companion object {
        val MARK_AS_PLAYED_DURATION_SETTING: SettingData<Int> =
            SettingData("mark_as_played_duration", 60)
        val MARK_AS_PLAYED_PERCENTAGE_SETTING: SettingData<Int> =
            SettingData("mark_as_played_percentage", 50)
        val ONLY_USE_FIRST_ARTIST_SETTING: SettingData<Boolean> =
            SettingData("only_use_first_artist", true)
        val ENABLE_NOW_PLAYING_SETTING: SettingData<Boolean> =
            SettingData("enable_now_playing", true)

        // Lista de exclusión predeterminada
        val ARTIST_EXCLUSION_LIST_DEFAULT = emptyList<String>()
        // Setting para la lista de exclusión personalizada
        val ARTIST_EXCLUSION_LIST_SETTING: SettingData<String> =
            SettingData("artist_exclusion_list", "")
    }

}
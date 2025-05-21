package dev.brahmkshatriya.echo.extension.config

object BuildConfig {
    private lateinit var slt: ByteArray

    private fun setSlt(saltBytes: ByteArray) {
        slt = saltBytes
    }

    private fun dbfsct(obfuscatedData: ByteArray): String {
        return obfuscatedData.mapIndexed { index, byte ->
            (byte.toInt() xor slt[index % slt.size].toInt()).toByte()
        }.toByteArray().decodeToString()
    }

    fun getK(): String {
        setSlt(byteArrayOf(
            10, 102, 99, -96, 2, -108, 4, 4, -2, -95, 14, -60, 124, 52, -81, 66
        ))

        val data = byteArrayOf(
            59, 3, 6, -110, 50, -90, 60, 101, -56, -110, 111, -10, 74, 7, -105, 122,
            60, 3, 2, -60, 97, -14, 98, 98, -52, -57, 59, -90, 73, 85, -97, 113
        )

        return dbfsct(data)
    }

    fun getScrt(): String {
        setSlt(byteArrayOf(
            19, 29, -31, 45, 7, 44, -99, 117, 30, -107, -98, -52, -125, 73, -102, 92
        ))

        val data = byteArrayOf(
            35, 127, -48, 20, 49, 72, -5, 68, 127, -89, -5, -6, -25, 43, -87, 56,
            36, 36, -40, 76, 54, 30, -7, 67, 40, -15, -89, -1, -74, 43, -4, 108
        )

        return dbfsct(data)
    }

    fun isDebug(): Boolean {
        return false
    }

    fun versionCode(): Int {
        return 1
    }
}
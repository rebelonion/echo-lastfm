package dev.brahmkshatriya.echo.extension.lastfm

import dev.brahmkshatriya.echo.extension.LastFMAPI.Companion.getApiKey
import dev.brahmkshatriya.echo.extension.LastFMAPI.Companion.getSecret
import java.math.BigInteger
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class UrlBuilder {
    companion object {
        /**
         * Generates a URL and signature for the given method and parameters. method should not be included in [parameters]
         * @param method The method to be called.
         * @param parameters The parameters to be passed to the method.
         * @return A Pair containing the generated URL and signature.
         */
        fun generateUrlWithSig(
            method: String,
            sessionKey: String?,
            parameters: MutableMap<String, String>
        ): String {
            parameters["api_key"] = getApiKey()
            if (!sessionKey.isNullOrBlank()) {
                parameters["sk"] = sessionKey
            }
            val sigMap = parameters.toMutableMap()
            sigMap["method"] = method
            val sig = generateSignature(sigMap)
            parameters["api_sig"] = sig
            val url = urlBuilder(method, parameters)
            return url
        }

        /**
         * Generates am MD5 hash signature for the given parameters.
         * @param parameters The parameters to be signed.
         * @return The generated signature.
         */
        private fun generateSignature(parameters: MutableMap<String, String>): String {
            val signatureString = StringBuilder()
            parameters.toSortedMap().forEach {
                if (it.key != "format" && it.key != "callback") {
                    signatureString.append(it.key)
                    signatureString.append(it.value)
                }
            }
            signatureString.append(getSecret())
            return signatureString.toString().md5Hash()
        }

        /**
         * Builds a URL for LastFM API calls.
         * @param method The method to be called.
         * @param parameters The parameters to be passed to the method.
         * @return The generated URL.
         */
        fun urlBuilder(method: String, parameters: MutableMap<String, String>): String {
            val url = StringBuilder()
                .append(API_URL)
                .append(METHOD_IDENTIFIER)
                .append(method)
                .append(JSON_IDENTIFIER)
            parameters.toSortedMap().forEach {
                val encodedKey = URLEncoder.encode(it.key, StandardCharsets.UTF_8.toString())
                val encodedValue = URLEncoder.encode(it.value, StandardCharsets.UTF_8.toString())
                url.append("&$encodedKey=$encodedValue")
            }
            return url.toString()
        }

        /**
         * Generates an MD5 hash of the string.
         * @return The MD5 hash.
         */
        private fun String.md5Hash(): String {
            val md5 = MessageDigest.getInstance("MD5")
            val bytes = md5.digest(this.toByteArray(Charsets.UTF_8))
            val bigInt = BigInteger(1, bytes)
            return bigInt.toString(16).padStart(32, '0')
        }

        private const val API_URL = "https://ws.audioscrobbler.com/2.0/"
        private const val METHOD_IDENTIFIER = "?method="
        private const val JSON_IDENTIFIER = "&format=json"
    }
}
package com.kwsilence.security

import com.kwsilence.mserver.BuildConfig
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Deprecated("Rework JWT")
object EncoderUtil {
    private val base64Encoder = Base64.getUrlEncoder()
    private val base64Decoder = Base64.getUrlDecoder()

    @Deprecated("Rework JWT")
    enum class Algorithm {
        HS256, // HMAC-SHA256
        RS256  // RSA-SHA256
    }

    fun signHS256(data: String, key: String = BuildConfig.jwtSecret): String {
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val hs256 = Mac.getInstance("HmacSHA256").apply { init(secretKey) }
        return hs256.doFinal(data.toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)
    }

    fun signRS256(data: String): String {
        return ""
    }

    fun String.encodeUrlBase64(): String = toByteArray(Charsets.UTF_8).encodeUrlBase64()
    fun ByteArray.encodeUrlBase64(): String = base64Encoder.encode(this).toString(Charsets.UTF_8)
    fun String.decodeUrlBase64(): String = base64Decoder.decode(this).toString(Charsets.UTF_8)
}
package com.kwsilence.security

import com.kwsilence.security.EncoderUtil.encodeUrlBase64
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Deprecated("Rework JWT")
class JWT private constructor(
    private val header: String,
    private val payload: String,
    private val signature: String
) {

    val token: String get() = listOf(header, payload, signature).joinToString(separator = ".")

    class Builder {
        var algorithm: EncoderUtil.Algorithm = EncoderUtil.Algorithm.HS256
        private val type: String = "JWT"
        private val payloadMap: HashMap<String, String> = HashMap()
        private var lifeTime: Long = TimeUnit.MICROSECONDS.convert(5, TimeUnit.MINUTES)

        fun setLifeTime(duration: Long, unit: TimeUnit = TimeUnit.MINUTES) {
            lifeTime = TimeUnit.MICROSECONDS.convert(duration, unit)
        }

        fun addClaim(key: String, value: String) {
            payloadMap[key] = value
        }

        fun build(): JWT {
            val headerMap = mapOf(
                "alg" to algorithm.name,
                "typ" to type
            )
            val header = Json.encodeToString(headerMap)
            payloadMap["exp"] = (Date().time + lifeTime).toString()
            val payload = Json.encodeToString(payloadMap)
            return createJWT(header, payload, algorithm)
        }
    }

    fun verify() {

    }

    companion object {
        private fun createJWT(header: String, payload: String, algorithm: EncoderUtil.Algorithm): JWT {
            val base64Header = header.encodeUrlBase64()
            val base64Payload = payload.encodeUrlBase64()
            val unsignedData = "$base64Header.$base64Payload"
            val base64Signature = when (algorithm) {
                EncoderUtil.Algorithm.HS256 -> EncoderUtil.signHS256(unsignedData)
                EncoderUtil.Algorithm.RS256 -> EncoderUtil.signRS256(unsignedData)
            }.encodeUrlBase64()
            return JWT(base64Header, base64Payload, base64Signature)
        }
    }
}
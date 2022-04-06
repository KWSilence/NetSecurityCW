package com.kwsilence.security

import java.util.Base64
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JWT private constructor(
    private val header: String,
    private val payload: String,
    private val signature: String
) {
    enum class Algorithm {
        HS256, // HMAC-SHA256
        RS256  // RSA-SHA256
    }

    class Builder {
        var algorithm: Algorithm = Algorithm.HS256
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
                "type" to type
            )
            val header = Json.encodeToString(headerMap)
            payloadMap["exp"] = (Date().time + lifeTime).toString()
            val payload = Json.encodeToString(payloadMap)
            return createJWT(header, payload)
        }
    }

    fun verify() {

    }

    companion object {
        private fun signString(target: String): String {
//            val hs256 = Mac.getInstance("HmacSHA256")
//            val secretKey = SecretKeySpec()
            return ""
        }

        private fun createJWT(header: String, payload: String): JWT {
            val encoder = Base64.getUrlEncoder()
            val base64Header = encoder.encode(header.toByteArray()).toString()
            val base64Payload = encoder.encode(payload.toByteArray()).toString()
            val base64Signature = encoder.encode(signString("$base64Header.$base64Payload").toByteArray()).toString()
            return JWT(base64Header, base64Payload, base64Signature)
        }
    }
}
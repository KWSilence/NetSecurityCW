package com.kwsilence.util

import com.kwsilence.db.model.User
import com.kwsilence.mserver.BuildConfig
import com.kwsilence.util.ExceptionUtil.throwBase
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.ktor.http.HttpStatusCode
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

object TokenUtil {
    private const val EXP_DURATION = 5L
    private val TIME_UNIT = TimeUnit.MINUTES

    fun getDefaultAuthKey(): SecretKey = Keys.hmacShaKeyFor(BuildConfig.jwtSecret.toByteArray())

    fun getExpirationDate(duration: Long = EXP_DURATION, unit: TimeUnit = TIME_UNIT): Date =
        Calendar.getInstance().apply {
            timeInMillis += TimeUnit.MILLISECONDS.convert(duration, unit)
        }.time

    fun checkToken(auth: String?): JwtParser {
        auth ?: HttpStatusCode.Unauthorized.throwBase()
        return runCatching {
            Jwts.parserBuilder().apply {
                setSigningKey(getDefaultAuthKey())
            }.build().apply {
                parse(auth)
            }
        }.getOrNull() ?: HttpStatusCode.Unauthorized.throwBase()
    }

    @kotlinx.serialization.Serializable
    data class TokenForUser(
        val authToken: String,
        val refreshToken: String
    )

    // todo refresh token
    fun getTokenPair(user: User, key: SecretKey = getDefaultAuthKey()): TokenForUser {
        val claims = mapOf("usr" to user.id.value)
        val authToken = Jwts.builder().apply {
            addClaims(claims)
            setId(UUID.randomUUID().toString())
            setIssuedAt(Date())
            setExpiration(getExpirationDate())
            signWith(key, SignatureAlgorithm.HS256)
        }.compact()
        return TokenForUser(authToken = authToken, refreshToken = "")
    }
}
package com.kwsilence.util

import com.kwsilence.mserver.BuildConfig
import com.kwsilence.service.data.UserTokenPair
import com.kwsilence.util.ExceptionUtil.throwBase
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.ktor.http.HttpStatusCode
import java.io.File
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TokenUtil {
    private const val EXP_DURATION = 5L
    private val TIME_UNIT = if (BuildConfig.debug) TimeUnit.HOURS else TimeUnit.MINUTES

    private val defaultAuthKey: SecretKey = Keys.hmacShaKeyFor(BuildConfig.jwtSecret.toByteArray())
    private fun getExpirationDate(duration: Long = EXP_DURATION, unit: TimeUnit = TIME_UNIT): Date =
        Calendar.getInstance().apply {
            timeInMillis += TimeUnit.MILLISECONDS.convert(duration, unit)
        }.time

    fun checkAuthToken(auth: String?, authKey: SecretKey = defaultAuthKey): Claims =
        auth?.let {
            runCatching {
                Jwts.parserBuilder().apply { setSigningKey(authKey) }.build().parseClaimsJws(auth).body
            }.getOrNull()
        } ?: HttpStatusCode.Unauthorized.throwBase()

    fun checkRefreshToken(refresh: String?, key: PublicKey = publicKey): Claims =
        refresh?.let {
            runCatching {
                Jwts.parserBuilder().apply { setSigningKey(key) }.build().parseClaimsJws(refresh).body
            }.getOrNull()
        } ?: HttpStatusCode.Unauthorized.throwBase()

    fun getTokenPair(userId: UUID, authKey: SecretKey = defaultAuthKey): UserTokenPair {
        val claims = mapOf("usr" to userId.toString())
        val authToken = Jwts.builder().apply {
            addClaims(claims)
            setId(UUID.randomUUID().toString())
            setIssuedAt(Date())
            setExpiration(getExpirationDate())
            signWith(authKey, SignatureAlgorithm.HS256)
        }.compact()
        val refreshToken = Jwts.builder().apply {
            addClaims(claims)
            setIssuedAt(Date())
            setExpiration((getExpirationDate(30, TimeUnit.DAYS)))
            signWith(privateKey, SignatureAlgorithm.RS256)
        }.compact()
        return UserTokenPair(authToken = authToken, refreshToken = refreshToken)
    }

    suspend fun generateKeyPair() {
        withContext(Dispatchers.IO) {
            val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()

            val privateFile = File("secrets/rsa.key")
            if (!privateFile.exists()) {
                FileOutputStream(privateFile).use { out ->
                    out.write(keyPair.private.encoded)
                }
            }

            val publicFile = File("secrets/rsa.pub")
            if (!publicFile.exists()) {
                FileOutputStream(publicFile).use { out ->
                    out.write(keyPair.public.encoded)
                }
            }
        }
    }


    val privateKey: PrivateKey by lazy {
        val bytes = File("secrets/rsa.key").readBytes()
        val ks = PKCS8EncodedKeySpec(bytes)
        KeyFactory.getInstance("RSA").generatePrivate(ks)
    }

    val publicKey: PublicKey by lazy {
        val bytes = File("secrets/rsa.pub").readBytes()
        val ks = X509EncodedKeySpec(bytes)
        KeyFactory.getInstance("RSA").generatePublic(ks)
    }

    fun String.toUUID(): UUID = UUID.fromString(this)
}
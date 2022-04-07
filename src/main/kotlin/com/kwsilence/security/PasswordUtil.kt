package com.kwsilence.security

import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtil {
    private val random: SecureRandom = SecureRandom.getInstance("SHA1PRNG")
    private const val SALT_SIZE = 64
    private const val KEY_BYTE_LEN = 256
    private const val ITERATION = 1495
    private fun getSalt(): ByteArray {
        val result = ByteArray(SALT_SIZE)
        random.nextBytes(result)
        return result
    }

    fun generatePassword(password: String, salt: ByteArray = getSalt()): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION, KEY_BYTE_LEN)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val encodedPass = skf.generateSecret(spec).encoded
        return (salt + encodedPass).encodeBase64()
    }

    fun verifyPassword(provided: String, secured: String): Boolean {
        val storedPass = secured.decodeBase64Bytes()
        val key = storedPass.asList().subList(0, SALT_SIZE).toByteArray()
        return generatePassword(provided, key) == secured
    }
}
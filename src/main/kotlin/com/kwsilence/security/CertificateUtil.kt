package com.kwsilence.security

import io.ktor.network.tls.certificates.generateCertificate
import java.io.File

/* todo delete on release
  keytool -keystore keystore.jks -alias testKtorAlias -genkeypair -keyalg RSA -keysize 4096 -validity 30
    -dname 'CN=localhost, OU=ktor, O=ktor, L=Unspecified, ST=Unspecified, C=RU'*/
object CertificateUtil {
    private const val keyAlias = "testKtorAlias"
    private const val keyPassword = "testKtor"
    private const val jksPassword = "testKtor"
    fun create() {
        val keyStoreFile = File("secrets/keystore.jks")
        generateCertificate(
            file = keyStoreFile,
            keyAlias = keyAlias,
            keyPassword = keyPassword,
            jksPassword = jksPassword
        )
    }
}
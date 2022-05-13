package com.kwsilence

import com.kwsilence.db.DatabaseUtil
import com.kwsilence.di.repositoryModule
import com.kwsilence.di.serviceModule
import com.kwsilence.plugins.configureHTTP
import com.kwsilence.plugins.configureMonitoring
import com.kwsilence.plugins.configureRouting
import com.kwsilence.plugins.configureSerialization
import com.kwsilence.security.CertificateUtil
import com.kwsilence.util.LogUtil.setLogger
import com.kwsilence.util.TokenUtil
import com.kwsilence.util.extension.launchDefault
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    runBlocking {
        listOf(
            launchDefault {
                TokenUtil.apply {
                    generateKeyPair()
                    privateKey
                    publicKey
                }
            },
            launchDefault { CertificateUtil.create() },
            launchDefault { DatabaseUtil.initDatabase() }
        ).joinAll()
    }
    setLogger()
    install(Koin) {
        printLogger()
        modules(repositoryModule, serviceModule)
    }
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}

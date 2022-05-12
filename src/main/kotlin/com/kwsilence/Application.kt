package com.kwsilence

import com.kwsilence.db.DatabaseUtil
import com.kwsilence.mserver.BuildConfig
import com.kwsilence.plugins.configureHTTP
import com.kwsilence.plugins.configureMonitoring
import com.kwsilence.plugins.configureRouting
import com.kwsilence.plugins.configureSerialization
import com.kwsilence.security.CertificateUtil
import com.kwsilence.util.LogUtil.setLogger
import com.kwsilence.util.TokenUtil
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        val keyGenJob = CoroutineScope(Dispatchers.Default).launch {
            TokenUtil.apply {
                generateKeyPair()
                privateKey
                publicKey
            }
        }
        CertificateUtil.create()
        DatabaseUtil.initDatabase()
        keyGenJob.join()
    }
    EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    if (BuildConfig.debug) setLogger()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}

package com.kwsilence

import com.kwsilence.db.DatabaseUtil
import io.ktor.server.application.*
import com.kwsilence.plugins.*
import com.kwsilence.plugins.ApiLocations.configureLocations
import com.kwsilence.security.CertificateUtil
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    CertificateUtil.create()
    DatabaseUtil.initDatabase()
    EngineMain.main(args)
}

@KtorExperimentalLocationsAPI
@Suppress("unused")
fun Application.module() {
    configureLocations()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}

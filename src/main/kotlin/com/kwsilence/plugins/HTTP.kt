package com.kwsilence.plugins

import com.kwsilence.mserver.BuildConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.httpsredirect.HttpsRedirect

fun Application.configureHTTP() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    if (!BuildConfig.debug) {
        install(HttpsRedirect) {
            sslPort = 8443
            permanentRedirect = true
        }
    }
}

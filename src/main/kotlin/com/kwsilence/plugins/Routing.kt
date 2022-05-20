package com.kwsilence.plugins

import com.kwsilence.controller.authorizationController
import com.kwsilence.controller.extensionController
import com.kwsilence.controller.syncController
import com.kwsilence.util.ExceptionUtil
import com.kwsilence.util.LogUtil
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get {
            call.respondText("Hello World!", ContentType.Text.Plain, HttpStatusCode.OK)
        }
        authorizationController()
        extensionController()
        syncController()
    }
    install(StatusPages) {
        exception<ExceptionUtil.BaseException> { call, baseException ->
            when (val message = baseException.message) {
                null -> call.respond(baseException.code)
                else -> {
                    LogUtil.error("ERROR", baseException)
                    call.respond(baseException.code, message)
                }
            }
        }
    }
}

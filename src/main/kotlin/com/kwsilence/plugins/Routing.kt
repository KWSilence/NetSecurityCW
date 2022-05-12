package com.kwsilence.plugins

import com.kwsilence.controller.setupAuthorizationController
import com.kwsilence.controller.setupExtensionController
import com.kwsilence.controller.setupSyncController
import com.kwsilence.db.repository.AuthRepository
import com.kwsilence.db.repository.SyncRepository
import com.kwsilence.service.LoginService
import com.kwsilence.service.RegistrationService
import com.kwsilence.service.ResetPasswordService
import com.kwsilence.service.SyncService
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
    val authRepository = AuthRepository()
    val syncRepository = SyncRepository()
    val loginService = LoginService(authRepository)
    val registrationService = RegistrationService(authRepository)
    val resetPasswordService = ResetPasswordService(authRepository)
    val syncService = SyncService(syncRepository)

    routing {
        get {
            call.respondText("Hello World!", ContentType.Text.Plain, HttpStatusCode.OK)
        }
        setupAuthorizationController(registrationService, loginService, resetPasswordService)
        setupExtensionController()
        setupSyncController(syncService)
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

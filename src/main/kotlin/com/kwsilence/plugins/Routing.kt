package com.kwsilence.plugins

import com.kwsilence.db.repository.AuthRepository
import com.kwsilence.db.repository.MangaRepository
import com.kwsilence.service.LoginService
import com.kwsilence.service.RegistrationService
import com.kwsilence.service.ResetPasswordService
import com.kwsilence.service.SyncService
import com.kwsilence.util.ApiHelper
import com.kwsilence.util.ExceptionUtil
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.FileUtil
import com.kwsilence.util.TokenUtil
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.get
import io.ktor.server.plugins.StatusPages
import io.ktor.server.request.receiveOrNull
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@KtorExperimentalLocationsAPI
fun Application.configureRouting() {
    val authRepository = AuthRepository()
    val mangaRepository = MangaRepository()
    val loginService = LoginService(authRepository)
    val registrationService = RegistrationService(authRepository)
    val resetPasswordService = ResetPasswordService(authRepository)
    val syncService = SyncService(mangaRepository)

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        post("/register") {
            registrationService.register(call.receiveOrNull())
            call.respond(HttpStatusCode.OK, "Registration successful")
        }

        post("/login") {
            val tokenPair = loginService.login(call.receiveOrNull())
            call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
                Json.encodeToString(tokenPair)
            }
        }

        get(ApiHelper.RESET_PASS_PATH) {
            resetPasswordService.sendResetPasswordMail(call.parameters["mail"])
            call.respond(HttpStatusCode.OK, "password reset link sent")
        }

        get("${ApiHelper.RESET_PASS_PATH}/{token}") {
            resetPasswordService.findUserId(call.parameters["token"])
            call.respondFile(File("html/resetpass.html"))
        }

        post("${ApiHelper.RESET_PASS_PATH}/{token}") {
            resetPasswordService.resetPassword(call.parameters["token"], call.receiveOrNull())
            call.respond(HttpStatusCode.OK, "password successfully changed")
        }

        get("/refresh") {
            val tokenPair = loginService.refreshToken(call.request.headers["Authorization"])
            call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
                Json.encodeToString(tokenPair)
            }
        }

        get<ApiLocations.SharedPath> { shared ->
            val auth = call.request.headers["Authorization"]
            TokenUtil.checkAuthToken(auth)
            FileUtil.shared(shared.path)?.let { file ->
                call.respondFile(file)
            } ?: HttpStatusCode.NotFound.throwBase()
        }

        get(ApiHelper.CONFIRM_PATH) {
            registrationService.sendConfirmMessage(call.parameters["mail"])
            call.respond(HttpStatusCode.OK, "confirm mail link sent")
        }

        get<ApiLocations.ConfirmMail> { confirm ->
            registrationService.confirmMail(confirm.token)
            call.respond(HttpStatusCode.OK, "mail was successfully confirmed")
        }

        get("/apk/{name}") {
            FileUtil.shared("extension/apk/${call.parameters["name"]}")?.let { apk ->
                call.respondFile(apk)
            } ?: HttpStatusCode.NotFound.throwBase()
        }

        get("/icon/{name}") {
            FileUtil.shared("extension/icon/${call.parameters["name"]}")?.let { icon ->
                call.respondFile(icon)
            } ?: HttpStatusCode.NotFound.throwBase()
        }

        get("/info") {
            FileUtil.shared("extension/info.json")?.let { info ->
                call.respondFile(info)
            } ?: HttpStatusCode.NotFound.throwBase()
        }

        post("/update") {
            val result = syncService.update(call.request.headers["Authorization"], call.receiveOrNull())
            call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
                Json.encodeToString(result)
            }
        }

        post("/sync") {
            val result = syncService.sync(call.request.headers["Authorization"], call.receiveOrNull())
            call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
                Json.encodeToString(result)
            }
        }

        install(StatusPages) {
            exception<ExceptionUtil.BaseException> { call, baseException ->
                when (val message = baseException.message) {
                    null -> call.respond(baseException.code)
                    else -> call.respond(baseException.code, message)
                }
            }
        }
    }
}

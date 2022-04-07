package com.kwsilence.plugins

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.mserver.BuildConfig
import com.kwsilence.service.LoginService
import com.kwsilence.service.RegistrationService
import com.kwsilence.service.ResetPasswordService
import com.kwsilence.util.ApiHelper
import com.kwsilence.util.ExceptionUtil
import com.kwsilence.util.TokenUtil
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.get
import io.ktor.server.plugins.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@KtorExperimentalLocationsAPI
fun Application.configureRouting() {
    val repository = DatabaseRepository()
    val loginService = LoginService(repository)
    val registrationService = RegistrationService(repository)
    val resetPasswordService = ResetPasswordService(repository)

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/register") {
            registrationService.register(call.parameters["mail"], call.parameters["password"])
            call.respond(HttpStatusCode.OK, "Registration successful")
        }

        get("/login") {
            val user = loginService.login(call.parameters["mail"], call.parameters["password"])
            val pair = TokenUtil.getTokenPair(user)
            call.respond(HttpStatusCode.OK, Json.encodeToString(pair))
        }

        get(ApiHelper.RESET_PASS_PATH) {
            resetPasswordService.sendResetPasswordMail(call.parameters["mail"])
            call.respond(HttpStatusCode.OK, "reset password mail sent")
        }

        get<ApiLocations.ResetPassword> { reset ->
            when (resetPasswordService.resetPassword(reset.token, call.parameters["password"])) {
                true -> call.respond(HttpStatusCode.OK, "password successfully changed")
                false -> call.respondFile(File("html/resetpass.html"))
            }
        }

        // todo refresh token
        get("/refresh") {
            val token = call.parameters["token"]
        }

        get<ApiLocations.SharedPath> { shared ->
            val auth = call.request.headers["Authorization"]
            TokenUtil.checkToken(auth)
            val sharedFile = File("${BuildConfig.sharePath}/${shared.path}")
            when (sharedFile.exists() && sharedFile.isFile) {
                true -> call.respondFile(sharedFile)
                false -> call.respond(HttpStatusCode.NotFound)
            }
        }

        get(ApiHelper.CONFIRM_PATH) {
            registrationService.sendConfirmMessage(call.parameters["mail"])
            call.respond(HttpStatusCode.OK, "confirm message sent")
        }

        get<ApiLocations.ConfirmMail> { confirm ->
            registrationService.confirmMail(confirm.token)
            call.respond(HttpStatusCode.OK, "mail was successfully confirmed")
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

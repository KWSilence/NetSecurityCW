package com.kwsilence.plugins

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.mserver.BuildConfig
import com.kwsilence.service.LoginService
import com.kwsilence.service.RegistrationService
import com.kwsilence.util.EmailUtil
import com.kwsilence.util.EmailUtil.send
import com.kwsilence.util.ExceptionUtil
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.get
import io.ktor.server.plugins.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File

@KtorExperimentalLocationsAPI
fun Application.configureRouting() {
    val repository = DatabaseRepository()
    val loginService = LoginService(repository)
    val registrationService = RegistrationService(repository)

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/info") {
            val infoFile = File("shared/info.json")
            when (infoFile.exists()) {
                true -> call.respondFile(infoFile)
                false -> call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/register") {
            registrationService.register(call.parameters["mail"], call.parameters["password"])
            call.respond(HttpStatusCode.OK, "Registration successful")
        }

        get("/login") {
            loginService.login(call.parameters["mail"], call.parameters["password"])
            call.respond(HttpStatusCode.OK)
        }

        // todo reset password
        get("/reset") {
            val mail = call.parameters["mail"] ?: call.respond(HttpStatusCode.BadRequest, "mail param not set")
        }

        get<ApiLocations.SharedPath> { shared ->
//            todo check auth
//            call.request.headers["Authorization"]
            val sharedFile = File("${BuildConfig.sharePath}/${shared.path}")
            when (sharedFile.exists() && sharedFile.isFile) {
                true -> call.respondFile(sharedFile)
                false -> call.respond(HttpStatusCode.NotFound)
            }
        }

        // todo rework mail
        get("/mail") {
            runCatching {
                EmailUtil.getDefaultMessage(
                    mailTo = BuildConfig.emailName,
                    subject = "Test Java Mail",
                    text = "HI"
                ).send()
                call.respond(HttpStatusCode.OK, "Email was sent")
            }.onFailure {
                log.error("Mail", it)
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

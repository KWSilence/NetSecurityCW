package com.kwsilence.plugins

import com.kwsilence.db.model.User
import com.kwsilence.mserver.BuildConfig
import com.kwsilence.util.EmailUtil
import com.kwsilence.util.EmailUtil.checkMail
import com.kwsilence.util.EmailUtil.send
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
import org.jetbrains.exposed.sql.insert

@KtorExperimentalLocationsAPI
fun Application.configureRouting() {
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

        // todo mb remake to post
        get("/register") {
            val mail = checkMail(call.parameters["mail"]) ?: call.respond(HttpStatusCode.BadRequest, "illegal mail")
            val password = call.parameters["password"]?.trim().let { pass ->
                when {
                    pass == null || pass.length < 6 -> call.respond(
                        HttpStatusCode.BadRequest,
                        "short password (less than 6)"
                    )
                    else -> pass
                }
            }
            // todo encode password
            User.insert {

            }
            // todo verify link
        }

        // todo getting token pair
        get("/login") {
            val mail = call.parameters["mail"] ?: call.respond(HttpStatusCode.BadRequest, "mail param not set")
            val password = call.parameters["password"] ?: call.respond(
                HttpStatusCode.BadRequest,
                "password param not set"
            )
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

        // todo delete shared
        get("/shared") {
            val sharedFolder = File(BuildConfig.sharePath)
            fun File.getList(depth: Int = 0): List<String> {
                var tabs = ""
                repeat(depth - 1) { tabs += "  " }
                return when {
                    isFile -> listOf("$tabs$name")
                    isDirectory -> {
                        val output = ArrayList<String>().apply {
                            if (depth != 0) add("$tabs$name:")
                        }
                        listFiles()?.forEach {
                            output += it.getList(depth + 1)
                        }
                        output
                    }
                    else -> throw Exception()
                }
            }
            call.respond(
                sharedFolder.getList().let { list ->
                    StringBuilder().apply {
                        list.forEach { fileName -> appendLine(fileName) }
                    }
                }.toString()
            )
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
            exception<AuthenticationException> { call, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> { call, _ ->
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

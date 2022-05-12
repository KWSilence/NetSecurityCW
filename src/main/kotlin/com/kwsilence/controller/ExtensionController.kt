package com.kwsilence.controller

import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.FileUtil
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.setupExtensionController() {
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
}
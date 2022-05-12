package com.kwsilence.controller

import com.kwsilence.service.SyncService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveOrNull
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Routing.setupSyncController(
    syncService: SyncService
) {
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
}
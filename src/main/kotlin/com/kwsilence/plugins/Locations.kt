package com.kwsilence.plugins

import com.kwsilence.util.ApiHelper
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Location
import io.ktor.server.locations.Locations

@KtorExperimentalLocationsAPI
object ApiLocations {
    fun Application.configureLocations() {
        install(Locations)
    }

    @Location("/shared/{path}")
    data class SharedPath(val path: String)

    @Location("${ApiHelper.CONFIRM_PATH}/{token}")
    data class ConfirmMail(val token: String)
}

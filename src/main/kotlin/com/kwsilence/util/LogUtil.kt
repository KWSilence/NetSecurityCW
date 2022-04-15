package com.kwsilence.util

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.util.logging.Logger

object LogUtil {
    private var logger: Logger? = null
    fun Application.setLogger() { logger = log }
    fun error(message: String) = logger?.error(message)
    fun info(message: String) = logger?.info(message)
    fun debug(message: String) = logger?.debug(message)
}
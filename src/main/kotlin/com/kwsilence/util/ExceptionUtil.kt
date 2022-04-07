package com.kwsilence.util

import io.ktor.http.HttpStatusCode

object ExceptionUtil {
    class BaseException(val code: HttpStatusCode, override val message: String? = null) : RuntimeException(message)

    fun HttpStatusCode.throwBase(): Nothing = throw BaseException(this)
    fun Pair<HttpStatusCode, String>.throwBase(): Nothing = throw BaseException(first, second)
}
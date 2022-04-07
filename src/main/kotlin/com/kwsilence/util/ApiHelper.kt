package com.kwsilence.util

import com.kwsilence.mserver.BuildConfig

object ApiHelper {
    const val CONFIRM_PATH = "/confirm"
    const val RESET_PASS_PATH = "/reset"

    fun String.withBaseUrl(): String = "${BuildConfig.baseUrl}$this"
}
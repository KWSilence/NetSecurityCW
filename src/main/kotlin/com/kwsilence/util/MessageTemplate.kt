package com.kwsilence.util

import com.kwsilence.mserver.BuildConfig
import javax.mail.Message

object MessageTemplate {

    private val testMail: String? = when (BuildConfig.debug) {
        true -> BuildConfig.emailName
        false -> null
    }

    fun confirmEmail(mailTo: String, url: String): Message =
        EmailUtil.getDefaultMessage(
            mailTo = testMail ?: mailTo,
            subject = "Confirm registration",
            text = "Click to link to confirm registration: ${HtmlUtil.hyperlink(url, "confirm")}.",
            type = HtmlUtil.mailType
        )

    fun resetPassword(mailTo: String, url: String): Message =
        EmailUtil.getDefaultMessage(
            mailTo = testMail ?: mailTo,
            subject = "Reset password",
            text = "Click to link to reset password: ${HtmlUtil.hyperlink(url, "reset")}.",
            type = HtmlUtil.mailType
        )
}
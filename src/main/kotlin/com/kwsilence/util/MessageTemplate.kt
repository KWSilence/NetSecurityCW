package com.kwsilence.util

import com.kwsilence.data.MailMessage

object MessageTemplate {

    fun confirmEmail(mailTo: String, url: String): MailMessage =
        MailMessage(
            mailTo = mailTo,
            subject = "Confirm email Ktor",
            content = "Use link $url to confirm email. Without confirming email you can't use application."
        )

    fun resetPassword(mailTo: String, url: String): MailMessage =
        MailMessage(
            mailTo = mailTo,
            subject = "Reset password Ktor",
            content = "To reset password use link $url"
        )
}
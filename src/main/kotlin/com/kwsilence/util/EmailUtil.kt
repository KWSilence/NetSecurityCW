package com.kwsilence.util

import com.kwsilence.mserver.BuildConfig
import java.util.Date
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


object EmailUtil {
    private val session =
        Properties().apply {
            putIfAbsent("mail.transport.protocol", "smtp")
            putIfAbsent("mail.smtp.host", "smtp.mail.ru")
            putIfAbsent("mail.smtp.port", "587")
            putIfAbsent("mail.smtp.auth", "true")
            putIfAbsent("mail.smtp.starttls.enable", "true")
            putIfAbsent("mail.smtp.ssl.trust", "smtp.mail.ru")
            putIfAbsent("mail.smtp.ssl.protocols", "TLSv1.2")
        }.let { props -> Session.getDefaultInstance(props, MailAuth(BuildConfig.emailName, BuildConfig.emailPass)) }

    private val mailRegex = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$".toRegex()

    fun checkMail(mail: String?): String? = mail?.trim()?.let {
        when (mailRegex.matches(it)) {
            true -> it
            false -> null
        }
    }

    fun getDefaultMessage(mailTo: String, subject: String, text: String, type: String? = null): Message =
        MimeMessage(session).apply {
            setFrom(InternetAddress(BuildConfig.emailName))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo, false))
            type?.let { setContent(text, type) } ?: setText(text)
            setSubject(subject)
            sentDate = Date()
        }

    fun Message.send() {
        session.transport.run {
            connect()
            sendMessage(this@send, allRecipients)
            close()
        }
    }

    private class MailAuth(private val name: String, private val pass: String) : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(name, pass)
        }
    }
}
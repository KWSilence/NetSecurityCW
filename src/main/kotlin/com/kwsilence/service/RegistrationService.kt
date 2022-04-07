package com.kwsilence.service

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.db.Tokens
import com.kwsilence.security.PasswordUtil
import com.kwsilence.util.ApiHelper
import com.kwsilence.util.ApiHelper.withBaseUrl
import com.kwsilence.util.EmailUtil
import com.kwsilence.util.EmailUtil.send
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.MessageTemplate
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegistrationService(private val repository: DatabaseRepository) {
    fun register(mailParam: String?, passwordParam: String?) {
        val email = getMailOrThrow(mailParam)
        val pass = getPasswordOrThrow(passwordParam)
        val userId = repository.createUser(email, pass)
        CoroutineScope(Dispatchers.IO).launch {
            sendConfirmMessage(userId, email)
        }
    }

    fun sendConfirmMessage(userId: Int, mail: String) {
        val confirmToken = UUID.randomUUID().toString()
        repository.setUserToken(userId, confirmToken, Tokens.CONFIRM)
        val confirmUrl = "${ApiHelper.CONFIRM_PATH.withBaseUrl()}/$confirmToken"
        MessageTemplate.confirmEmail(mail, confirmUrl).send()
    }

    fun sendConfirmMessage(userMail: String?) {
        repository.getUserByMail(userMail)?.let { user ->
            if (user.isConfirmed) (HttpStatusCode.Conflict to "user already confirmed mail")
            sendConfirmMessage(user.id.value, userMail!!)
        } ?: (HttpStatusCode.BadRequest to "incorrect mail param").throwBase()
    }

    fun confirmMail(confirmToken: String) {
        val userId = repository.getUserIdByToken(confirmToken, Tokens.CONFIRM) ?: HttpStatusCode.NotFound.throwBase()
        repository.updateUser(userId, confirmed = true)
        repository.resetUserTokens(userId, Tokens.CONFIRM)
    }

    private fun getMailOrThrow(mailParam: String?): String {
        val checkMail = EmailUtil.checkMail(mailParam)
        val repUser = repository.getUserByMail(checkMail)
        return when {
            mailParam == null -> (HttpStatusCode.BadRequest to "set mail param").throwBase()
            checkMail == null -> (HttpStatusCode.BadRequest to "incorrect mail").throwBase()
            repUser != null -> (HttpStatusCode.BadRequest to "user already exist").throwBase()
            else -> checkMail
        }
    }

    private fun getPasswordOrThrow(passwordParam: String?): String =
        passwordParam?.trim().let { pass ->
            when {
                pass == null -> (HttpStatusCode.BadRequest to "set password param").throwBase()
                pass.length < 6 -> (HttpStatusCode.BadRequest to "short password (less than 6)").throwBase()
                else -> PasswordUtil.generatePassword(pass)
            }
        }
}
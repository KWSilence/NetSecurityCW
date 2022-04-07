package com.kwsilence.service

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.db.Tokens
import com.kwsilence.security.PasswordUtil
import com.kwsilence.util.ApiHelper
import com.kwsilence.util.ApiHelper.withBaseUrl
import com.kwsilence.util.EmailUtil.send
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.MessageTemplate
import io.ktor.http.HttpStatusCode
import java.util.UUID

class ResetPasswordService(private val repository: DatabaseRepository) {
    fun sendResetPasswordMail(userMail: String?) {
        repository.getUserIdByMail(userMail)?.let { userId ->
            val resetToken = UUID.randomUUID().toString()
            repository.setUserToken(userId, resetToken, Tokens.RESET)
            val resetUrl = "${ApiHelper.RESET_PASS_PATH.withBaseUrl()}/$resetToken"
            MessageTemplate.resetPassword(userMail!!, resetUrl).send()
        } ?: (HttpStatusCode.BadRequest to "incorrect mail param").throwBase()
    }

    fun resetPassword(token: String, newPass: String?): Boolean {
        val userId = repository.getUserIdByToken(token, Tokens.RESET) ?: HttpStatusCode.NotFound.throwBase()
        return when (newPass) {
            null -> false
            else -> {
                val pass = getPasswordOrThrow(newPass)
                repository.updateUser(userId, pass = pass)
                repository.resetUserTokens(userId, Tokens.RESET)
                true
            }
        }
    }

    private fun getPasswordOrThrow(newPass: String): String =
        newPass.trim().let { pass ->
            when {
                pass.length < 6 -> (HttpStatusCode.BadRequest to "short password (less than 6)").throwBase()
                else -> PasswordUtil.generatePassword(pass)
            }
        }
}
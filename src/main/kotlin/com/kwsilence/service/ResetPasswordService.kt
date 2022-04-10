package com.kwsilence.service

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.db.Tokens
import com.kwsilence.security.PasswordUtil
import com.kwsilence.service.data.PasswordReset
import com.kwsilence.util.ApiHelper
import com.kwsilence.util.ApiHelper.withBaseUrl
import com.kwsilence.util.EmailUtil.send
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.MessageTemplate
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResetPasswordService(private val repository: DatabaseRepository) {
    fun sendResetPasswordMail(userMail: String?) {
        repository.getUserByMail(userMail)?.let { user ->
            if (!user.isConfirmed) (HttpStatusCode.Conflict to "confirm mail before").throwBase()
            val resetToken = UUID.randomUUID().toString()
            repository.setUserToken(user.id.value, resetToken, Tokens.RESET)
            CoroutineScope(Dispatchers.IO).launch {
                val resetUrl = "${ApiHelper.RESET_PASS_PATH.withBaseUrl()}/$resetToken"
                MessageTemplate.resetPassword(userMail!!, resetUrl).send()
            }
        } ?: (HttpStatusCode.BadRequest to "incorrect mail param").throwBase()
    }

    fun resetPassword(token: String?, reset: PasswordReset?) {
        val userId = findUserId(token)
        when (reset?.newPassword) {
            null -> (HttpStatusCode.BadRequest to "set new password in body").throwBase()
            else -> {
                val pass = getPasswordOrThrow(reset.newPassword)
                repository.apply {
                    updateUser(userId, pass = pass)
                    resetUserTokens(userId, listOf(Tokens.RESET, Tokens.REFRESH))
                }
            }
        }
    }

    fun findUserId(token: String?): Int =
        token?.let {
            repository.getUserIdByToken(token, Tokens.RESET)
        } ?: HttpStatusCode.NotFound.throwBase()

    private fun getPasswordOrThrow(newPass: String): String =
        newPass.trim().let { pass ->
            when {
                pass.length < 6 -> (HttpStatusCode.BadRequest to "short password (less than 6)").throwBase()
                else -> PasswordUtil.generatePassword(pass)
            }
        }
}
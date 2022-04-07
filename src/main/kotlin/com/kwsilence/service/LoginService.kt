package com.kwsilence.service

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.security.PasswordUtil
import com.kwsilence.util.ExceptionUtil.throwBase
import io.ktor.http.HttpStatusCode

class LoginService(private val repository: DatabaseRepository) {
    fun login(mailParam: String?, passwordParam: String?) {
        val email = getMailOrThrow(mailParam)
        val pass = getPasswordOrThrow(passwordParam)
        val user = repository.getUserByMail(email)
        when {
            user == null || !PasswordUtil.verifyPassword(pass, user.password) -> {
                (HttpStatusCode.BadRequest to "incorrect login or password").throwBase()
            }
            !user.isConfirmed -> {
                (HttpStatusCode.BadRequest to "mail is not confirmed").throwBase()
            }
        }
    }

    private fun getMailOrThrow(mailParam: String?): String = when (mailParam) {
        null -> (HttpStatusCode.BadRequest to "mail param not set").throwBase()
        else -> mailParam
    }

    private fun getPasswordOrThrow(passwordParam: String?): String = when (passwordParam) {
        null -> (HttpStatusCode.BadRequest to "password param not set").throwBase()
        else -> passwordParam
    }
}
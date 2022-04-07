package com.kwsilence.service

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.security.PasswordUtil
import com.kwsilence.util.EmailUtil
import com.kwsilence.util.ExceptionUtil.throwBase
import io.ktor.http.HttpStatusCode

// todo send confirm to mail
class RegistrationService(private val repository: DatabaseRepository) {
    fun register(mailParam: String?, passwordParam: String?) {
        val email = getMailOrThrow(mailParam)
        val pass = getPasswordOrThrow(passwordParam)
        repository.createUser(email, pass)
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
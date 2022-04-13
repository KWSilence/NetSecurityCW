package com.kwsilence.service

import com.kwsilence.db.Tokens
import com.kwsilence.db.repository.AuthRepository
import com.kwsilence.security.PasswordUtil
import com.kwsilence.service.data.UserCred
import com.kwsilence.service.data.UserTokenPair
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.TokenUtil
import com.kwsilence.util.TokenUtil.toUUID
import io.ktor.http.HttpStatusCode

class LoginService(private val repository: AuthRepository) {
    fun login(cred: UserCred?, checkConfirm: Boolean = true): UserTokenPair {
        val email = getMailOrThrow(cred?.mail)
        val pass = getPasswordOrThrow(cred?.pass)
        val user = repository.getUserByMail(email)
        when {
            user == null || !PasswordUtil.verifyPassword(pass, user.password) -> {
                (HttpStatusCode.BadRequest to "incorrect login or password").throwBase()
            }
            checkConfirm && !user.isConfirmed -> {
                (HttpStatusCode.BadRequest to "mail is not confirmed").throwBase()
            }
        }
        val tokenPair = TokenUtil.getTokenPair(user!!.id.value)
        repository.setUserToken(user.id.value, tokenPair.refreshToken, Tokens.REFRESH)
        return tokenPair
    }

    fun refreshToken(refreshToken: String?): UserTokenPair {
        val userId = (TokenUtil.checkRefreshToken(refreshToken)["usr"] as? String)?.toUUID()
        userId ?: HttpStatusCode.Unauthorized.throwBase()
        val tokenId = repository.getUserTokenId(userId, refreshToken!!, Tokens.REFRESH)
        tokenId ?: HttpStatusCode.NotFound.throwBase()
        val tokenPair = TokenUtil.getTokenPair(userId)
        repository.updateToken(tokenId, tokenPair.refreshToken)
        return tokenPair
    }

    private fun getMailOrThrow(mailParam: String?): String = when (mailParam) {
        null -> (HttpStatusCode.BadRequest to "mail in body not set").throwBase()
        else -> mailParam
    }

    private fun getPasswordOrThrow(passwordParam: String?): String = when (passwordParam) {
        null -> (HttpStatusCode.BadRequest to "password in body not set").throwBase()
        else -> passwordParam
    }
}
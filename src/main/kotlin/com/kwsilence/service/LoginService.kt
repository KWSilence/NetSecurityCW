package com.kwsilence.service

import com.kwsilence.db.DatabaseRepository
import com.kwsilence.db.Tokens
import com.kwsilence.security.PasswordUtil
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.TokenUtil
import io.ktor.http.HttpStatusCode

class LoginService(private val repository: DatabaseRepository) {
    fun login(mailParam: String?, passwordParam: String?, checkConfirm: Boolean = true): TokenUtil.UserTokenPair {
        val email = getMailOrThrow(mailParam)
        val pass = getPasswordOrThrow(passwordParam)
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

    fun refreshToken(refreshToken: String?): TokenUtil.UserTokenPair {
        val userId = TokenUtil.checkRefreshToken(refreshToken).parseClaimsJws(refreshToken).body["usr"] as? Int
        userId ?: HttpStatusCode.Unauthorized.throwBase()
        val tokenId = repository.getUserTokenId(userId, refreshToken!!, Tokens.REFRESH)
        tokenId ?: HttpStatusCode.NotFound.throwBase()
        val tokenPair = TokenUtil.getTokenPair(userId)
        repository.updateToken(tokenId, tokenPair.refreshToken)
        return tokenPair
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
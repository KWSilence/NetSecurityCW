package com.kwsilence.db.repository

import com.kwsilence.db.Tokens
import com.kwsilence.db.model.User
import com.kwsilence.db.table.auth.UserTable
import com.kwsilence.db.table.auth.UserTokenTable
import java.util.UUID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class AuthRepository {
    fun getUserByMail(email: String?): User? =
        transaction {
            email?.let {
                User.find { UserTable.mail eq email }.firstOrNull()
            }
        }

    fun getUserIdByMail(email: String?): UUID? =
        getUserByMail(email)?.id?.value

    fun createUser(email: String, pass: String): UUID =
        transaction {
            User.new {
                mail = email
                password = pass
            }.id.value
        }

    fun updateUser(userId: UUID, pass: String? = null, confirmed: Boolean? = null) {
        transaction {
            pass?.let { User[userId].password = it }
            confirmed?.let { User[userId].isConfirmed = it }
        }
    }

    fun getUserTokenId(userId: UUID, token: String, type: Tokens): Int? =
        transaction {
            UserTokenTable.select {
                UserTokenTable.run {
                    (this.userId eq userId) and (this.token eq token) and (this.type eq type.id)
                }
            }.firstOrNull()?.get(UserTokenTable.id)?.value
        }

    fun getUserTokens(userId: UUID, type: Tokens): List<String> =
        ArrayList<String>().apply {
            transaction {
                UserTokenTable.select {
                    (UserTokenTable.userId eq userId) and (UserTokenTable.type eq type.id)
                }
            }.forEach {
                add(it[UserTokenTable.token])
            }
        }

    fun getUserIdByToken(token: String, type: Tokens): UUID? =
        transaction {
            UserTokenTable.select {
                (UserTokenTable.token eq token) and (UserTokenTable.type eq type.id)
            }.firstOrNull()?.get(UserTokenTable.userId)?.value
        }

    fun updateToken(tokenId: Int, newToken: String) {
        transaction {
            UserTokenTable.update(where = { (UserTokenTable.id eq tokenId) }) {
                it[token] = newToken
            }
        }
    }

    fun setUserToken(userId: UUID, token: String, type: Tokens) {
        transaction {
            UserTokenTable.insert {
                it[this.userId] = userId
                it[this.token] = token
                it[this.type] = type.id
            }
        }
    }

    fun resetUserTokens(userId: UUID, types: List<Tokens>) {
        transaction {
            types.forEach { type ->
                UserTokenTable.deleteWhere {
                    (UserTokenTable.userId eq userId) and (UserTokenTable.type eq type.id)
                }
            }
        }
    }
}
package com.kwsilence.db

import com.kwsilence.db.model.User
import com.kwsilence.db.table.UserTable
import com.kwsilence.db.table.UserTokenTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseRepository {
    fun getUserByMail(email: String?): User? =
        transaction {
            email?.let {
                User.find { UserTable.mail eq email }.firstOrNull()
            }
        }

    fun getUserIdByMail(email: String?): Int? =
        getUserByMail(email)?.id?.value

    fun createUser(email: String, pass: String): Int =
        transaction {
            User.new {
                mail = email
                password = pass
            }.id.value
        }

    fun updateUser(userId: Int, pass: String? = null, confirmed: Boolean? = null) {
        transaction {
            pass?.let { User[userId].password = it }
            confirmed?.let { User[userId].isConfirmed = it }
        }
    }

    fun getUserTokens(userId: Int, type: Tokens): List<String> =
        ArrayList<String>().apply {
            transaction {
                UserTokenTable.select {
                    (UserTokenTable.userId eq userId) and (UserTokenTable.type eq type.id)
                }
            }.forEach {
                add(it[UserTokenTable.token])
            }
        }

    fun getUserIdByToken(token: String, type: Tokens): Int? =
        transaction {
            UserTokenTable.select {
                (UserTokenTable.token eq token) and (UserTokenTable.type eq type.id)
            }.firstOrNull()?.get(UserTokenTable.userId)?.value
        }

    fun replaceRefreshToken(oldToken: String, newToken: String) {
        transaction {
            UserTokenTable.update(where = {
                (UserTokenTable.token eq oldToken) and (UserTokenTable.type eq Tokens.REFRESH.id)
            }) {
                it[token] = newToken
            }
        }
    }

    fun setUserToken(userId: Int, token: String, type: Tokens) {
        transaction {
            UserTokenTable.insert {
                it[this.userId] = userId
                it[this.token] = token
                it[this.type] = type.id
            }
        }
    }

    fun resetUserTokens(userId: Int, type: Tokens) {
        transaction {
            UserTokenTable.deleteWhere {
                (UserTokenTable.userId eq userId) and (UserTokenTable.type eq type.id)
            }
        }
    }
}
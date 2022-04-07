package com.kwsilence.db

import com.kwsilence.db.model.User
import com.kwsilence.db.table.UserTable
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseRepository {
    fun getUserByMail(email: String?): User? =
        transaction {
            email?.let {
                User.find { UserTable.mail eq email }.firstOrNull()
            }
        }

    fun createUser(email: String, pass: String) {
        transaction {
            User.new {
                mail = email
                password = pass
            }
        }
    }
}
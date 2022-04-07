package com.kwsilence.db.table

import org.jetbrains.exposed.dao.id.IntIdTable

object UserTable : IntIdTable("users") {
    val mail = varchar("mail", 100).uniqueIndex()
    val password = varchar("password", 128)
    val isConfirmed = bool("is_confirmed").default(false)
}
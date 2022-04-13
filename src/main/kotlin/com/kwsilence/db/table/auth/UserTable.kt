package com.kwsilence.db.table.auth

import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable("users") {
    val mail = varchar("mail", 100).uniqueIndex()
    val password = varchar("password", 128)
    val isConfirmed = bool("is_confirmed").default(false)
}
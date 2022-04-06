package com.kwsilence.db.model

import org.jetbrains.exposed.dao.id.IntIdTable

// todo crypt password
object User : IntIdTable("users") {
    val mail = varchar("mail", 40).uniqueIndex()
    val password = varchar("password", 40)
    val isConfirmed = bool("is_confirmed").default(false)
}
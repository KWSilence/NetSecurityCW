package com.kwsilence.db.table

import org.jetbrains.exposed.dao.id.IntIdTable

object UserTokenTable : IntIdTable("users_tokens") {
    val userId = reference("user_id", UserTable)
    val token = varchar("token", 256)
    val type = reference("type_id", TokenTypeTable)
}
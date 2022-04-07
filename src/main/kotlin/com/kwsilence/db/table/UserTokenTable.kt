package com.kwsilence.db.table

import org.jetbrains.exposed.dao.id.IntIdTable

object UserTokenTable : IntIdTable("users_tokens") {
    val userId = reference("user_id", UserTable)
    val refreshToken = varchar("refresh_token", 60)
}
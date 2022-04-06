package com.kwsilence.db.model

import org.jetbrains.exposed.dao.id.IntIdTable

object UserToken : IntIdTable("users_tokens") {
    val userId = reference("user_id", User)
    val refreshToken = varchar("refresh_token", 60)
}
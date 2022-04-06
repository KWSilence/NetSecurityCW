package com.kwsilence.db.model

import org.jetbrains.exposed.dao.id.IntIdTable

object UserLink : IntIdTable("users_links") {
    val userId = reference("user_id", User)
    val link = varchar("link", 50)
    val type = reference("type_id", LinkType)
}
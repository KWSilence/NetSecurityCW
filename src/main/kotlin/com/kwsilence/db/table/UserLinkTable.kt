package com.kwsilence.db.table

import org.jetbrains.exposed.dao.id.IntIdTable

object UserLinkTable : IntIdTable("users_links") {
    val userId = reference("user_id", UserTable)
    val link = varchar("link", 50)
    val type = reference("type_id", LinkTypeTable)
}
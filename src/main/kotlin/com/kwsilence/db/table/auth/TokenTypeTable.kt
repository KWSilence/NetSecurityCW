package com.kwsilence.db.table.auth

import org.jetbrains.exposed.dao.id.IntIdTable

object TokenTypeTable : IntIdTable("token_type") {
    val type = varchar("type", 40)
}
package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.UUIDTable

object CategoryTable : UUIDTable("category") {
    val name = varchar("name", 300)
    val updateDate = long("update_date")
    val deleted = bool("deleted").default(false)
}
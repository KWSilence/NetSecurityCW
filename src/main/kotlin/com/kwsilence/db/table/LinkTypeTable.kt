package com.kwsilence.db.table

import org.jetbrains.exposed.dao.id.IntIdTable

object LinkTypeTable : IntIdTable("link_type") {
    val type = varchar("type", 40)
}
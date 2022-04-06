package com.kwsilence.db.model

import org.jetbrains.exposed.dao.id.IntIdTable

object LinkType : IntIdTable("link_type") {
    val type = varchar("type", 40)
}
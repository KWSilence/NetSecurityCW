package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.IntIdTable

object MangaStatusTypeTable : IntIdTable("manga_status_type") {
    val type = varchar("type", 100)
}
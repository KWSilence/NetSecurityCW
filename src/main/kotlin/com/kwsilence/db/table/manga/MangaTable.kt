package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.UUIDTable

object MangaTable : UUIDTable("manga") {
    val title = varchar("title", 300)
    val sourceId = integer("source_id")
    val url = varchar("url", 300)
    val artist = varchar("artist", 300).nullable()
    val author = varchar("author", 300).nullable()
    val description = varchar("description", 1000).nullable()
    val genres = varchar("genres", 1000).nullable()
    val coverUrl = varchar("cover_url", 300).nullable()
    val updateDate = long("update_date")
    val deleted = bool("deleted").default(false)
}
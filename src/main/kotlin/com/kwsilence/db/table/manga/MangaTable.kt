package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.UUIDTable

object MangaTable : UUIDTable("manga") {
    val title = varchar("title", 1000)
    val sourceId = long("source_id")
    val url = varchar("url", 300)
    val coverUrl = varchar("cover_url", 300).nullable()
    val lastModified = long("last_modified")
    val updateDate = long("update_date")
    val operation = reference("operation", OperationTypeTable)
}
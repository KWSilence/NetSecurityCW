package com.kwsilence.db.table.manga

import java.util.Date
import org.jetbrains.exposed.dao.id.UUIDTable

object MangaTable : UUIDTable("manga") {
    val title = varchar("title", 300)
    val sourceId = long("source_id")
    val url = varchar("url", 300)
    val status = reference("status", MangaStatusTypeTable)
    val author = varchar("author", 300).nullable()
    val description = varchar("description", 3000).nullable()
    val genres = varchar("genres", 2000).nullable()
    val coverUrl = varchar("cover_url", 300).nullable()
    val updateDate = long("update_date")
    val createDate = long("create_date").default(Date().time)
    val operation = reference("operation", OperationTypeTable)
}
package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object ChapterTable : UUIDTable("chapter") {
    val name = varchar("name", 300)
    val mangaId = reference("manga_id", MangaTable, onDelete = ReferenceOption.CASCADE)
    val url = varchar("url", 300)
    val isRead = bool("read")
    val number = float("number")
    val lastPageRead = integer("last_page_read")
    val uploadDate = long("upload_date")
    val lastModified = long("last_modified")
    val updateDate = long("update_date")
    val operation = reference("operation", OperationTypeTable)
}
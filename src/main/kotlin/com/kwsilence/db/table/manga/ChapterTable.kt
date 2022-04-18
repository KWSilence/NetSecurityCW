package com.kwsilence.db.table.manga

import java.util.Date
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object ChapterTable : UUIDTable("chapter") {
    val name = varchar("name", 300)
    val mangaId = reference("mangaId", MangaTable, onDelete = ReferenceOption.CASCADE)
    val url = varchar("url", 300)
    val isRead = bool("read")
    val number = float("number")
    val lastPageRead = integer("last_page_read")
    val uploadDate = long("upload_date")
    val updateDate = long("update_date")
    val createDate = long("create_date").default(Date().time)
    val operation = reference("operation", OperationTypeTable)
}
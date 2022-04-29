package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object MangaCategoryTable : UUIDTable("manga_category") {
    val mangaId = reference("manga_id", MangaTable, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", CategoryTable, onDelete = ReferenceOption.CASCADE)
    val lastModified = long("last_modified")
    val updateDate = long("update_date")
    val operation = reference("operation", OperationTypeTable)
}
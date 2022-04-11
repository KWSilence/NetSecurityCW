package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.UUIDTable

object MangaCategoryTable: UUIDTable("manga_category") {
    val mangaId = reference("manga_id", MangaTable)
    val categoryId = reference("category_id", CategoryTable)
    val updateDate = long("update_date")
    val deleted = bool("deleted").default(false)
}
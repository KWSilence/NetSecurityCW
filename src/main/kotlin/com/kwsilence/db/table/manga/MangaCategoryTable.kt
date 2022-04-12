package com.kwsilence.db.table.manga

import com.kwsilence.service.data.Operation
import java.util.Date
import org.jetbrains.exposed.dao.id.UUIDTable

object MangaCategoryTable: UUIDTable("manga_category") {
    val mangaId = reference("manga_id", MangaTable)
    val categoryId = reference("category_id", CategoryTable)
    val updateDate = long("update_date")
    val createDate = long("create_date").default(Date().time)
    val operation = integer("operation").default(Operation.INS.id)
}
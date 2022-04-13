package com.kwsilence.db.table.manga

import java.util.Date
import org.jetbrains.exposed.dao.id.UUIDTable

object CategoryTable : UUIDTable("category") {
    val name = varchar("name", 300)
    val updateDate = long("update_date")
    val createDate = long("create_date").default(Date().time)
    val operation = reference("operation", OperationTypeTable)
}
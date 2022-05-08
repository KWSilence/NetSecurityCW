package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.UUIDTable

object CategoryTable : UUIDTable("category") {
    val name = varchar("name", 300)
    val order = integer("order")
    val lastModified = long("last_modified")
    val updateDate = long("update_date")
    val operation = reference("operation", OperationTypeTable)
}
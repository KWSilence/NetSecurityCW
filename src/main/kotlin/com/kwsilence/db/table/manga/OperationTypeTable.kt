package com.kwsilence.db.table.manga

import org.jetbrains.exposed.dao.id.IntIdTable

object OperationTypeTable: IntIdTable("operation_type") {
    val type = varchar("type", 50)
}
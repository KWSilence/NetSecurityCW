package com.kwsilence.db.table.manga

import com.kwsilence.db.table.auth.UserTable
import java.util.Date
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object UserCategoryTable : UUIDTable("user_category") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", CategoryTable, onDelete = ReferenceOption.CASCADE)
    val updateDate = long("update_date")
    val createDate = long("create_date").default(Date().time)
    val operation = reference("operation", OperationTypeTable)
}
package com.kwsilence.db.table.manga

import com.kwsilence.db.table.auth.UserTable
import java.util.Date
import org.jetbrains.exposed.dao.id.UUIDTable

object UserCategoryTable : UUIDTable("user_category") {
    val userId = reference("user_id", UserTable)
    val categoryId = reference("category_id", CategoryTable)
    val updateDate = long("update_date")
    val createDate = long("create_date").default(Date().time)
    val operation = reference("operation", OperationTypeTable)
}
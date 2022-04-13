package com.kwsilence.db.model

import com.kwsilence.db.table.auth.UserTable
import java.util.UUID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(UserTable)

    var mail by UserTable.mail
    var password by UserTable.password
    var isConfirmed by UserTable.isConfirmed
}
package com.kwsilence.db.model

import com.kwsilence.db.table.auth.UserTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UserTable)

    var mail by UserTable.mail
    var password by UserTable.password
    var isConfirmed by UserTable.isConfirmed
}
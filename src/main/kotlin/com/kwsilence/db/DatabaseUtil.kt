package com.kwsilence.db

import com.kwsilence.db.table.LinkTypeTable
import com.kwsilence.db.table.UserLinkTable
import com.kwsilence.db.table.UserTable
import com.kwsilence.db.table.UserTokenTable
import com.kwsilence.mserver.BuildConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseUtil {
    val db: Database by lazy {
        Database.connect(
            url = BuildConfig.dbUrl,
            driver = BuildConfig.dbDriver,
            user = BuildConfig.dbUser,
            password = BuildConfig.dbPass
        )
    }

    fun initDatabase() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(UserTable, UserTokenTable, UserLinkTable, LinkTypeTable)
            DatabaseHelper.Links.values().forEach { link ->
                LinkTypeTable.insertIgnore {
                    it[id] = link.id
                    it[type] = link.type
                }
            }
        }
    }
}

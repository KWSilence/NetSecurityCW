package com.kwsilence.db

import com.kwsilence.db.model.LinkType
import com.kwsilence.db.model.User
import com.kwsilence.db.model.UserLink
import com.kwsilence.db.model.UserToken
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
            SchemaUtils.create(User, UserToken, UserLink, LinkType)
            DatabaseHelper.Links.values().forEach { link ->
                LinkType.insertIgnore {
                    it[id] = link.id
                    it[type] = link.type
                }
            }
        }
    }
}

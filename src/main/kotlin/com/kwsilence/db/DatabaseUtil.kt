package com.kwsilence.db

import com.kwsilence.db.table.auth.TokenTypeTable
import com.kwsilence.db.table.auth.UserTable
import com.kwsilence.db.table.auth.UserTokenTable
import com.kwsilence.db.table.manga.CategoryTable
import com.kwsilence.db.table.manga.ChapterTable
import com.kwsilence.db.table.manga.MangaCategoryTable
import com.kwsilence.db.table.manga.MangaTable
import com.kwsilence.db.table.manga.OperationTypeTable
import com.kwsilence.db.table.manga.UserCategoryTable
import com.kwsilence.mserver.BuildConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.TransactionManager
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
        TransactionManager.defaultDatabase = db
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(
                UserTable,
                UserTokenTable,
                TokenTypeTable,

                UserCategoryTable,

                OperationTypeTable,
                CategoryTable,
                MangaTable,
                MangaCategoryTable,
                ChapterTable
            )
            Tokens.values().forEach { link ->
                TokenTypeTable.insertIgnore {
                    it[id] = link.id
                    it[type] = link.type
                }
            }
            Operation.values().forEach { operation ->
                OperationTypeTable.insertIgnore {
                    it[id] = operation.id
                    it[type] = operation.type
                }
            }
        }
    }
}

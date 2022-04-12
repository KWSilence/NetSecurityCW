package com.kwsilence.db.repository

import com.kwsilence.db.model.tables
import com.kwsilence.db.table.manga.CategoryTable
import com.kwsilence.db.table.manga.ChapterTable
import com.kwsilence.db.table.manga.MangaCategoryTable
import com.kwsilence.db.table.manga.MangaTable
import com.kwsilence.db.table.manga.UserCategoryTable
import com.kwsilence.service.data.DataUpdate
import com.kwsilence.service.data.Operation
import com.kwsilence.service.data.ResponseDataUpdate
import com.kwsilence.service.data.ResponseSyncData
import com.kwsilence.service.data.SyncData
import com.kwsilence.util.ExceptionUtil.throwBase
import io.ktor.http.HttpStatusCode
import java.util.Date
import java.util.UUID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class MangaRepository {
    fun update(userId: Int, data: List<DataUpdate>): List<ResponseDataUpdate> {
        val result = ArrayList<ResponseDataUpdate>()
        transaction {
            data.forEach { record ->
                when (val table = tables[record.tb]) {
                    is CategoryTable -> {
                        table.proceedTable(record).also { stringUID ->
                            UserCategoryTable.proceedTable(userId, stringUID, record.op)
                        }
                    }
                    is ChapterTable -> {
                        val mangaUID = result getMangaId record
                        table.proceedTable(record, mangaUID)
                    }
                    is MangaCategoryTable -> {
                        val mangaUID = result getMangaId record
                        val categoryUID = result getCategoryId record
                        table.proceedTable(record, mangaUID, categoryUID)
                    }
                    is MangaTable -> table.proceedTable(record)
                    else -> (HttpStatusCode.Conflict to "invalid table '${record.tb}'").throwBase()
                }?.let { uid ->
                    result.add(ResponseDataUpdate(uid = uid, lid = record.lid, tb = record.tb))
                }
            }
        }
        return result
    }

    fun sync(userId: Int, data: SyncData): List<ResponseSyncData> {
        val result = ArrayList<ResponseSyncData>()
        val lastUpdate = data.lastUpdate
        transaction {
            result.apply {
                addAll(getCategoryUpdates(userId, lastUpdate))
                addAll(getMangaUpdates(userId, lastUpdate))
                addAll(getMangaCategoryUpdates(userId, lastUpdate))
                addAll(getChapterUpdates(userId, lastUpdate))
            }
        }
        return result
    }


    private infix fun List<ResponseDataUpdate>.getMangaId(record: DataUpdate): UUID? =
        record.data["_mangaId"]?.toInt()?.let { mangaId ->
            find { it.lid == mangaId && it.tb == "manga" }?.uid?.toUUID()
        }

    private infix fun List<ResponseDataUpdate>.getCategoryId(record: DataUpdate): UUID? =
        record.data["_categoryId"]?.toInt()?.let { categoryId ->
            find { it.lid == categoryId && it.tb == "category" }?.uid?.toUUID()
        }

    private fun CategoryTable.proceedTable(record: DataUpdate): String? {
        val data = record.data
        return when (record.op) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[name] = data["name"] ?: ("category" missing "name")
                        it[updateDate] = updateTime
                        it[operation] = Operation.UPD.id
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    it[name] = data["name"] ?: ("category" missing "name")
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = Operation.DEL.id
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
        }
    }

    private fun UserCategoryTable.proceedTable(userId: Int, stringUID: String?, operation: Int) {
        val categoryUID = stringUID?.toUUID() ?: return
        when (operation) {
            Operation.INS.id -> {
                insert {
                    it[this.userId] = userId
                    it[categoryId] = categoryUID
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[UserCategoryTable.operation] = Operation.INS.id
                }
            }
            Operation.DEL.id -> {
                update({ categoryId eq categoryUID }) {
                    it[updateDate] = updateTime
                    it[UserCategoryTable.operation] = Operation.DEL.id
                }
            }
        }
    }

    private fun ChapterTable.proceedTable(record: DataUpdate, mangaUID: UUID? = null): String? {
        val data = record.data
        return when (record.op) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[name] = data["name"] ?: ("chapter" missing "name")
                        it[url] = data["url"] ?: ("chapter" missing "url")
                        it[isRead] = data["isRead"]?.toBoolean() ?: ("chapter" missing "isRead")
                        it[number] = data["number"]?.toInt() ?: ("chapter" missing "number")
                        it[lastPageRead] = data["lastPageRead"]?.toInt() ?: ("chapter" missing "lastPageRead")
                        it[uploadDate] = data["uploadDate"]?.toLong() ?: ("chapter" missing "uploadDate")
                        it[updateDate] = updateTime
                        it[operation] = Operation.UPD.id
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    it[name] = data["name"] ?: ("chapter" missing "name")
                    it[url] = data["url"] ?: ("chapter" missing "url")
                    it[mangaId] = data["mangaId"]?.toUUID() ?: mangaUID ?: ("chapter" missing "mangaId")
                    it[isRead] = data["isRead"]?.toBoolean() ?: ("chapter" missing "isRead")
                    it[number] = data["number"]?.toInt() ?: ("chapter" missing "number")
                    it[lastPageRead] = data["lastPageRead"]?.toInt() ?: ("chapter" missing "lastPageRead")
                    it[uploadDate] = data["uploadDate"]?.toLong() ?: ("chapter" missing "uploadDate")
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = Operation.DEL.id
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
        }
    }

    private fun MangaCategoryTable.proceedTable(record: DataUpdate, mangaUID: UUID?, categoryUID: UUID?): String? {
        val data = record.data
        return when (record.op) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[mangaId] = data["mangaId"]?.toUUID() ?: mangaUID ?: ("manga" missing "mangaId")
                        it[categoryId] = data["categoryId"]?.toUUID() ?: categoryUID ?: ("manga" missing "categoryId")
                        it[updateDate] = updateTime
                        it[operation] = Operation.UPD.id
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    it[mangaId] = data["mangaId"]?.toUUID() ?: mangaUID ?: ("manga" missing "mangaId")
                    it[categoryId] = data["categoryId"]?.toUUID() ?: categoryUID ?: ("manga" missing "categoryId")
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = Operation.DEL.id
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
        }
    }

    private fun MangaTable.proceedTable(record: DataUpdate): String? {
        val data = record.data
        return when (record.op) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[title] = data["title"] ?: ("manga" missing "title")
                        it[sourceId] = data["sourceId"]?.toInt() ?: ("manga" missing "sourceId")
                        it[url] = data["url"] ?: ("manga" missing "url")
                        it[artist] = data["artist"]
                        it[author] = data["author"]
                        it[description] = data["description"]
                        it[genres] = data["genres"]
                        it[coverUrl] = data["coverUrl"]
                        it[updateDate] = updateTime
                        it[operation] = Operation.UPD.id
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    it[title] = data["title"] ?: ("manga" missing "title")
                    it[sourceId] = data["sourceId"]?.toInt() ?: ("manga" missing "sourceId")
                    it[url] = data["url"] ?: ("manga" missing "url")
                    it[artist] = data["artist"]
                    it[author] = data["author"]
                    it[description] = data["description"]
                    it[genres] = data["genres"]
                    it[coverUrl] = data["coverUrl"]
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = Operation.DEL.id
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
        }
    }

    private fun getCategoryUpdates(userId: Int, lastUpdate: Long): List<ResponseSyncData> {
        val result = ArrayList<ResponseSyncData>()
        (UserCategoryTable innerJoin CategoryTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id)
        }.forEach {
            val createDate = it[CategoryTable.createDate]
            val updateDate = it[CategoryTable.updateDate]
            val operation = it[CategoryTable.operation]
            val updateMap = mapOf(
                "name" to it[CategoryTable.name],
                "updateDate" to it[CategoryTable.updateDate].toString()
            )
            val uid = it[CategoryTable.id].value.toString()
            val table = "category"
            result.change(uid, table, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun getMangaCategoryUpdates(userId: Int, lastUpdate: Long): List<ResponseSyncData> {
        val result = ArrayList<ResponseSyncData>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id) and
                    (MangaCategoryTable.categoryId eq CategoryTable.id)
        }.forEach {
            val createDate = it[MangaCategoryTable.createDate]
            val updateDate = it[MangaCategoryTable.updateDate]
            val operation = it[MangaCategoryTable.operation]
            val updateMap = mapOf(
                "mangaId" to it[MangaCategoryTable.mangaId].value.toString(),
                "categoryId" to it[MangaCategoryTable.categoryId].value.toString(),
                "updateDate" to it[MangaCategoryTable.updateDate].toString()
            )
            val uid = it[MangaCategoryTable.id].value.toString()
            val table = "manga_category"
            result.change(uid, table, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun getMangaUpdates(userId: Int, lastUpdate: Long): List<ResponseSyncData> {
        val result = ArrayList<ResponseSyncData>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin MangaTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id) and
                    (MangaCategoryTable.categoryId eq CategoryTable.id) and (MangaTable.id eq MangaCategoryTable.mangaId)
        }.forEach {
            val createDate = it[MangaTable.createDate]
            val updateDate = it[MangaTable.updateDate]
            val operation = it[MangaTable.operation]
            val updateMap = mapOf(
                "title" to it[MangaTable.title],
                "categoryId" to it[MangaTable.sourceId].toString(),
                "url" to it[MangaTable.url],
                "artist" to it[MangaTable.artist],
                "author" to it[MangaTable.author],
                "description" to it[MangaTable.description],
                "genres" to it[MangaTable.genres],
                "coverUrl" to it[MangaTable.coverUrl],
                "updateDate" to it[MangaTable.updateDate].toString()
            )
            val uid = it[MangaTable.id].value.toString()
            val table = "manga"
            result.change(uid, table, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun getChapterUpdates(userId: Int, lastUpdate: Long): List<ResponseSyncData> {
        val result = ArrayList<ResponseSyncData>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin
                MangaTable innerJoin ChapterTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id) and
                    (MangaCategoryTable.categoryId eq CategoryTable.id) and
                    (MangaTable.id eq MangaCategoryTable.mangaId) and
                    (ChapterTable.mangaId eq MangaTable.id)
        }.forEach {
            val createDate = it[ChapterTable.createDate]
            val updateDate = it[ChapterTable.updateDate]
            val operation = it[ChapterTable.operation]
            val updateMap = mapOf(
                "name" to it[ChapterTable.name],
                "mangaId" to it[ChapterTable.mangaId].value.toString(),
                "url" to it[ChapterTable.url],
                "isRead" to it[ChapterTable.isRead].toString(),
                "number" to it[ChapterTable.number].toString(),
                "lastPageRead" to it[ChapterTable.lastPageRead].toString(),
                "uploadDate" to it[ChapterTable.uploadDate].toString(),
                "updateDate" to it[ChapterTable.updateDate].toString()
            )
            val uid = it[ChapterTable.id].value.toString()
            val table = "chapter"
            result.change(uid, table, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun ArrayList<ResponseSyncData>.change(
        uid: String,
        table: String,
        lastUpdate: Long,
        createDate: Long,
        updateDate: Long,
        operation: Int,
        updateMap: Map<String, String?>
    ) {
        when {
            createDate > lastUpdate && operation != Operation.DEL.id -> {
                ResponseSyncData(uid = uid, tb = table, op = Operation.INS.id, data = updateMap)
            }
            updateDate > lastUpdate && operation == Operation.UPD.id -> {
                ResponseSyncData(uid = uid, tb = table, op = Operation.UPD.id, data = updateMap)
            }
            updateDate > lastUpdate && operation == Operation.DEL.id -> {
                ResponseSyncData(uid = uid, tb = table, op = Operation.DEL.id)
            }
            else -> null
        }?.let { syncData -> add(syncData) }
    }

    private fun String.toUUID(): UUID = UUID.fromString(this)
    private infix fun String.missing(field: String): Nothing =
        (HttpStatusCode.BadRequest to "$this: missing $field").throwBase()

    private val updateTime get() = Date().time
}
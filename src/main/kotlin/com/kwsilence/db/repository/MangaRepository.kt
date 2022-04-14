package com.kwsilence.db.repository

import com.kwsilence.db.Operation
import com.kwsilence.db.model.tableKeyOrder
import com.kwsilence.db.model.tables
import com.kwsilence.db.table.manga.CategoryTable
import com.kwsilence.db.table.manga.ChapterTable
import com.kwsilence.db.table.manga.MangaCategoryTable
import com.kwsilence.db.table.manga.MangaTable
import com.kwsilence.db.table.manga.UserCategoryTable
import com.kwsilence.service.data.DataUpdate
import com.kwsilence.service.data.DataUpdateItem
import com.kwsilence.service.data.ResponseDataUpdateItem
import com.kwsilence.service.data.ResponseSyncDataItem
import com.kwsilence.service.data.SyncData
import com.kwsilence.service.data.TableData
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.TokenUtil.toUUID
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
    fun update(userId: UUID, data: DataUpdate): Map<String, List<ResponseDataUpdateItem>> {
        val result = HashMap<String, ArrayList<ResponseDataUpdateItem>>()
        transaction {
            tableKeyOrder.forEach { key ->
                data[key]?.also { result[key] = ArrayList() }?.forEach { updateItem ->
                    val record = key to updateItem
                    when (val table = tables[key]) {
                        is CategoryTable -> {
                            table.proceedTable(record)?.also { stringUID ->
                                UserCategoryTable.proceedTable(userId, stringUID.toUUID(), record.operation)
                            }
                        }
                        is ChapterTable -> {
                            val mangaUID = result getMangaId updateItem
                            table.proceedTable(record, mangaUID)
                        }
                        is MangaCategoryTable -> {
                            val mangaUID = result getMangaId updateItem
                            val categoryUID = result getCategoryId updateItem
                            table.proceedTable(record, mangaUID, categoryUID)
                        }
                        is MangaTable -> table.proceedTable(record)
                        else -> (HttpStatusCode.Conflict to "invalid table '$key'").throwBase()
                    }?.let { uid ->
                        result[key]?.add(ResponseDataUpdateItem(uid = uid, lid = updateItem.lid))
                    }
                }
            }
        }
        return result
    }

    fun sync(userId: UUID, data: SyncData): Map<String, List<ResponseSyncDataItem>> =
        transaction {
            val lastUpdate = data.lastUpdate
            HashMap<String, List<ResponseSyncDataItem>>().apply {
                getCategoryUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("category", it) }
                getMangaUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("manga", it) }
                getMangaCategoryUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("manga_category", it) }
                getChapterUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("chapter", it) }
            }
        }

    private infix fun Map<String, List<ResponseDataUpdateItem>>.getMangaId(record: DataUpdateItem): String? =
        record.data["_mangaId"]?.toInt()?.let { mangaId -> get("manga")?.find { it.lid == mangaId }?.uid }

    private infix fun Map<String, List<ResponseDataUpdateItem>>.getCategoryId(record: DataUpdateItem): String? =
        record.data["_categoryId"]?.toInt()?.let { categoryId -> get("category")?.find { it.lid == categoryId }?.uid }

    private fun CategoryTable.proceedTable(record: Pair<String, DataUpdateItem>): String? =
        when (record.operation) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[name] = record.getOrMissing("name")
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    it[name] = record.getOrMissing("name")
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[operation] = record.operation
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.operation}'").throwBase()
        }

    private fun UserCategoryTable.proceedTable(userId: UUID, categoryUID: UUID, operation: Int) {
        when (operation) {
            Operation.INS.id -> {
                insert {
                    it[this.userId] = userId
                    it[categoryId] = categoryUID
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[UserCategoryTable.operation] = operation
                }
            }
            Operation.DEL.id -> {
                update({ categoryId eq categoryUID }) {
                    it[updateDate] = updateTime
                    it[UserCategoryTable.operation] = operation
                }
            }
        }
    }

    private fun ChapterTable.proceedTable(record: Pair<String, DataUpdateItem>, mangaUID: String? = null): String? =
        when (record.operation) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        record.run {
                            it[name] = getOrMissing("name")
                            it[url] = getOrMissing("url")
                            it[isRead] = getOrMissing("isRead").toBoolean()
                            it[number] = getOrMissing("number").toInt()
                            it[lastPageRead] = getOrMissing("lastPageRead").toInt()
                            it[uploadDate] = getOrMissing("uploadDate").toLong()
                        }
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    record.run {
                        it[name] = getOrMissing("name")
                        it[url] = getOrMissing("url")
                        it[mangaId] = getOrMissing("mangaId", mangaUID).toUUID()
                        it[isRead] = getOrMissing("isRead").toBoolean()
                        it[number] = getOrMissing("number").toInt()
                        it[lastPageRead] = getOrMissing("lastPageRead").toInt()
                        it[uploadDate] = getOrMissing("uploadDate").toLong()
                    }
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[operation] = record.operation
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.operation}'").throwBase()
        }


    private fun MangaCategoryTable.proceedTable(
        record: Pair<String, DataUpdateItem>, mangaUID: String?, categoryUID: String?
    ): String? =
        when (record.operation) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[mangaId] = record.getOrMissing("mangaId", mangaUID).toUUID()
                        it[categoryId] = record.getOrMissing("categoryId", categoryUID).toUUID()
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    it[mangaId] = record.getOrMissing("mangaId", mangaUID).toUUID()
                    it[categoryId] = record.getOrMissing("categoryId", categoryUID).toUUID()
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[operation] = record.operation
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.operation}'").throwBase()
        }

    private fun MangaTable.proceedTable(record: Pair<String, DataUpdateItem>): String? =
        when (record.operation) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        record.run {
                            it[title] = getOrMissing("title")
                            it[sourceId] = getOrMissing("sourceId").toInt()
                            it[url] = getOrMissing("url")
                            it[artist] = get("artist")
                            it[author] = get("author")
                            it[description] = get("description")
                            it[genres] = get("genres")
                            it[coverUrl] = get("coverUrl")
                        }
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    record.run {
                        it[title] = getOrMissing("title")
                        it[sourceId] = getOrMissing("sourceId").toInt()
                        it[url] = getOrMissing("url")
                        it[artist] = get("artist")
                        it[author] = get("author")
                        it[description] = get("description")
                        it[genres] = get("genres")
                        it[coverUrl] = get("coverUrl")
                    }
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[operation] = record.operation
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[operation] = record.operation
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.operation}'").throwBase()
        }

    private fun getCategoryUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id)
        }.forEach {
            val createDate = it[CategoryTable.createDate]
            val updateDate = it[CategoryTable.updateDate]
            val operation = it[CategoryTable.operation].value
            val updateMap = mapOf(
                "name" to it[CategoryTable.name], "updateDate" to it[CategoryTable.updateDate].toString()
            )
            val uid = it[CategoryTable.id].value.toString()
            result.change(uid, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun getMangaCategoryUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id) and (MangaCategoryTable.categoryId eq CategoryTable.id)
        }.forEach {
            val createDate = it[MangaCategoryTable.createDate]
            val updateDate = it[MangaCategoryTable.updateDate]
            val operation = it[MangaCategoryTable.operation].value
            val updateMap = mapOf(
                "mangaId" to it[MangaCategoryTable.mangaId].value.toString(),
                "categoryId" to it[MangaCategoryTable.categoryId].value.toString(),
                "updateDate" to it[MangaCategoryTable.updateDate].toString()
            )
            val uid = it[MangaCategoryTable.id].value.toString()
            result.change(uid, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun getMangaUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin MangaTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id) and (MangaCategoryTable.categoryId eq CategoryTable.id) and (MangaTable.id eq MangaCategoryTable.mangaId)
        }.forEach {
            val createDate = it[MangaTable.createDate]
            val updateDate = it[MangaTable.updateDate]
            val operation = it[MangaTable.operation].value
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
            result.change(uid, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun getChapterUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin MangaTable innerJoin ChapterTable).select {
            (UserCategoryTable.userId eq userId) and (UserCategoryTable.categoryId eq CategoryTable.id) and (MangaCategoryTable.categoryId eq CategoryTable.id) and (MangaTable.id eq MangaCategoryTable.mangaId) and (ChapterTable.mangaId eq MangaTable.id)
        }.forEach {
            val createDate = it[ChapterTable.createDate]
            val updateDate = it[ChapterTable.updateDate]
            val operation = it[ChapterTable.operation].value
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
            result.change(uid, lastUpdate, createDate, updateDate, operation, updateMap)
        }
        return result
    }

    private fun ArrayList<ResponseSyncDataItem>.change(
        uid: String,
        lastUpdate: Long,
        createDate: Long,
        updateDate: Long,
        operation: Int,
        updateMap: Map<String, String?>
    ) {
        when {
            createDate > lastUpdate && operation != Operation.DEL.id -> {
                ResponseSyncDataItem(uid = uid, op = Operation.INS.id, data = updateMap)
            }
            updateDate > lastUpdate && operation == Operation.UPD.id -> {
                ResponseSyncDataItem(uid = uid, op = Operation.UPD.id, data = updateMap)
            }
            updateDate > lastUpdate && operation == Operation.DEL.id -> {
                ResponseSyncDataItem(uid = uid, op = Operation.DEL.id)
            }
            else -> null
        }?.let { syncData -> add(syncData) }
    }

    private infix fun String.missing(field: String): Nothing =
        (HttpStatusCode.BadRequest to "$this: missing $field").throwBase()

    private fun TableData.get(key: String): String? = second.data[key]
    private val TableData.operation: Int get() = second.op
    private val TableData.uid: String? get() = second.uid
    private fun TableData.getOrMissing(field: String, default: String? = null): String =
        get(field) ?: default ?: (first missing field)

    private fun <T> List<T>.takeIfNotEmpty(): List<T>? = takeIf { it.isNotEmpty() }

    private val updateTime get() = Date().time
}
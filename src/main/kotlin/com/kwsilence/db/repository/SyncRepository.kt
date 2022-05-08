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
import com.kwsilence.util.extension.asyncTransaction
import com.kwsilence.util.extension.newTransactionIO
import io.ktor.http.HttpStatusCode
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.awaitAll
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class SyncRepository {
    suspend fun update(userId: UUID, data: DataUpdate): Map<String, List<ResponseDataUpdateItem>> = newTransactionIO {
        HashMap<String, ArrayList<ResponseDataUpdateItem>>().also { result ->
            tableKeyOrder.forEach { keys ->
                keys.map { key ->
                    asyncTransaction {
                        data[key]?.also { result[key] = ArrayList() }?.map { updateItem ->
                            asyncTransaction {
                                val record = key to updateItem
                                when (val table = tables[key]) {
                                    is CategoryTable -> {
                                        table.proceedTable(userId, record)?.also { stringUID ->
                                            UserCategoryTable.proceedTable(userId, stringUID.toUUID(), record.op)
                                        }
                                    }
                                    is ChapterTable -> {
                                        val mangaUID = result getMangaId updateItem
                                        table.proceedTable(userId, record, mangaUID)
                                    }
                                    is MangaCategoryTable -> {
                                        val mangaUID = result getMangaId updateItem
                                        val categoryUID = result getCategoryId updateItem
                                        table.proceedTable(userId, record, mangaUID, categoryUID)
                                    }
                                    is MangaTable -> table.proceedTable(userId, record)
                                    else -> (HttpStatusCode.Conflict to "invalid table '$key'").throwBase()
                                }?.let { uid -> ResponseDataUpdateItem(uid = uid, lid = updateItem.lid) }
                            }
                        }?.awaitAll()?.filterNotNull()?.let { key to it }
                    }
                }.awaitAll().filterNotNull().forEach { pair -> result[pair.first]?.addAll(pair.second) }
            }
        }
    }

    suspend fun sync(userId: UUID, data: SyncData): Map<String, List<ResponseSyncDataItem>> = newTransactionIO {
        val lastUpdate = data.lastUpdate
        val category = asyncTransaction { CategoryTable.getUpdates(userId, lastUpdate).takeIfNotEmpty() }
        val manga = asyncTransaction { MangaTable.getUpdates(userId, lastUpdate).takeIfNotEmpty() }
        val mangaCategory = asyncTransaction { MangaCategoryTable.getUpdates(userId, lastUpdate).takeIfNotEmpty() }
        val chapter = asyncTransaction { ChapterTable.getUpdates(userId, lastUpdate).takeIfNotEmpty() }
        HashMap<String, List<ResponseSyncDataItem>>().apply {
            category.await()?.let { put("category", it) }
            manga.await()?.let { put("manga", it) }
            mangaCategory.await()?.let { put("manga_category", it) }
            chapter.await()?.let { put("chapter", it) }
        }
    }

    private fun CategoryTable.getCategory(userId: UUID, record: TableData) = runCatching {
        val categoryName = record.getOrMissing("name")
        (record.uid)?.toUUID()?.let { uid ->
            select { id eq uid }.firstOrNull()
        } ?: innerJoin(UserCategoryTable)
            .slice(columns)
            .select { (name eq categoryName) and (UserCategoryTable.userId eq userId) }
            .firstOrNull()
    }.getOrNull()

    private fun ChapterTable.getChapter(userId: UUID, record: TableData, mangaUID: String?) = runCatching {
        val mUID = record.getOrMissing("mangaId", mangaUID).toUUID()
        val rURL = record.getOrMissing("url")
        (record.uid)?.toUUID()?.let { uid ->
            select { (id eq uid) }.firstOrNull()
        } ?: (innerJoin(MangaTable) innerJoin MangaCategoryTable innerJoin CategoryTable innerJoin UserCategoryTable)
            .slice(columns)
            .select { (mangaId eq mUID) and (url eq rURL) and (UserCategoryTable.userId eq userId) }
            .firstOrNull()
    }.getOrNull()

    private fun MangaCategoryTable.getMangaCategory(
        userId: UUID, record: TableData, mangaUID: String?, categoryUID: String?
    ) = runCatching {
        val mUID = record.getOrMissing("mangaId", mangaUID).toUUID()
        val cUID = record.getOrMissing("categoryId", categoryUID).toUUID()
        record.uid?.toUUID()?.let { uid ->
            select { (id eq uid) }.firstOrNull()
        } ?: (innerJoin(CategoryTable) innerJoin UserCategoryTable)
            .slice(columns)
            .select { (mangaId eq mUID) and (categoryId eq cUID) and (UserCategoryTable.userId eq userId) }
            .firstOrNull()
    }.getOrNull()

    private fun MangaTable.getManga(userId: UUID, record: TableData) = runCatching {
        val rSourceId = record.getOrMissing("sourceId").toLong()
        val rURL = record.getOrMissing("url")
        record.uid?.toUUID()?.let { uid ->
            select { (id eq uid) }.firstOrNull()
        } ?: (innerJoin(MangaCategoryTable) innerJoin CategoryTable innerJoin UserCategoryTable)
            .slice(columns)
            .select { (sourceId eq rSourceId) and (url eq rURL) and (UserCategoryTable.userId eq userId) }
            .firstOrNull()
    }.getOrNull()


    private infix fun Map<String, List<ResponseDataUpdateItem>>.getMangaId(record: DataUpdateItem): String? =
        record.data["_mangaId"]?.toInt()?.let { mangaId -> get("manga")?.find { it.lid == mangaId }?.uid }

    private infix fun Map<String, List<ResponseDataUpdateItem>>.getCategoryId(record: DataUpdateItem): String? =
        record.data["_categoryId"]?.toInt()?.let { categoryId -> get("category")?.find { it.lid == categoryId }?.uid }

    private fun CategoryTable.getNewCategoryOrder(userUID: UUID): Int =
        innerJoin(UserCategoryTable)
            .slice(CategoryTable.columns)
            .select { (UserCategoryTable.userId eq userUID) }
            .orderBy(order, SortOrder.DESC)
            .firstOrNull()?.get(order)?.plus(1) ?: 0

    // todo fix order troubles later
    private fun CategoryTable.proceedTable(userUID: UUID, record: TableData): String? = when (record.op) {
        Operation.INS.id, Operation.UPD.id -> {
            runCatching {
                getCategory(userUID, record)?.let { category ->
                    if (record.update > category[updateDate]) {
                        update({ id eq category[id] }) {
                            it[name] = record.getOrMissing("name")
                            it[order] = record.getOrMissing("order").toInt()
                            it[updateDate] = record.update
                            it[lastModified] = currentTime
                            it[operation] = record.op
                        }
                    }
                    category[id].value.toString()
                } ?: record.uid
            }.getOrNull() ?: insertAndGetId {
                it[name] = record.getOrMissing("name")
                it[order] = getNewCategoryOrder(userUID)
                it[updateDate] = record.update
                it[lastModified] = currentTime
                it[operation] = record.op
            }.value.toString()
        }
        Operation.DEL.id -> {
            record.uid?.also { uid ->
                update({ id eq uid.toUUID() }) {
                    it[order] = -1
                    it[updateDate] = currentTime
                    it[lastModified] = currentTime
                    it[operation] = record.op
                }
            }
        }
        else -> invalidOperation(record.op)
    }

    private fun UserCategoryTable.proceedTable(userUID: UUID, categoryUID: UUID, op: Int) {
        when (op) {
            Operation.INS.id -> {
                select {
                    (userId eq userUID) and (categoryId eq categoryUID)
                }.firstOrNull() ?: insert {
                    it[userId] = userUID
                    it[categoryId] = categoryUID
                    it[updateDate] = currentTime
                    it[lastModified] = currentTime
                    it[operation] = op
                }
            }
            Operation.DEL.id -> {
                update({ categoryId eq categoryUID }) {
                    it[updateDate] = currentTime
                    it[lastModified] = currentTime
                    it[operation] = op
                }
            }
        }
    }

    private fun ChapterTable.proceedTable(
        userId: UUID, record: Pair<String, DataUpdateItem>, mangaUID: String? = null
    ): String? = when (record.op) {
        Operation.INS.id, Operation.UPD.id -> {
            runCatching {
                getChapter(userId, record, mangaUID)?.let { chapter ->
                    if (record.update > chapter[updateDate]) {
                        update({ id eq chapter[id] }) {
                            record.run {
                                it[name] = getOrMissing("name")
                                it[url] = getOrMissing("url")
                                it[isRead] = getOrMissing("isRead").toBoolean()
                                it[number] = getOrMissing("number").toFloat()
                                it[lastPageRead] = getOrMissing("lastPageRead").toInt()
                                it[uploadDate] = getOrMissing("uploadDate").toLong()
                                it[updateDate] = update
                                it[lastModified] = currentTime
                                it[operation] = Operation.UPD.id
                            }
                        }
                    }
                    chapter[id].value.toString()
                } ?: record.uid
            }.getOrNull() ?: insertAndGetId {
                record.run {
                    it[name] = getOrMissing("name")
                    it[url] = getOrMissing("url")
                    it[mangaId] = getOrMissing("mangaId", mangaUID).toUUID()
                    it[isRead] = getOrMissing("isRead").toBoolean()
                    it[number] = getOrMissing("number").toFloat()
                    it[lastPageRead] = getOrMissing("lastPageRead").toInt()
                    it[uploadDate] = getOrMissing("uploadDate").toLong()
                    it[updateDate] = update
                    it[lastModified] = currentTime
                    it[operation] = op
                }
            }.value.toString()
        }
        Operation.DEL.id -> {
            record.uid?.also { uid ->
                update({ id eq uid.toUUID() }) {
                    it[updateDate] = currentTime
                    it[lastModified] = currentTime
                    it[operation] = record.op
                }
            }
        }
        else -> invalidOperation(record.op)
    }


    private fun MangaCategoryTable.proceedTable(
        userId: UUID, record: TableData, mangaUID: String?, categoryUID: String?
    ): String? = when (record.op) {
        Operation.INS.id, Operation.UPD.id -> {
            runCatching {
                getMangaCategory(userId, record, mangaUID, categoryUID)?.let { mangaCategory ->
                    if (record.update > mangaCategory[updateDate]) {
                        update({ id eq mangaCategory[id] }) {
                            it[mangaId] = record.getOrMissing("mangaId", mangaUID).toUUID()
                            it[categoryId] = record.getOrMissing("categoryId", categoryUID).toUUID()
                            it[updateDate] = record.update
                            it[lastModified] = currentTime
                            it[operation] = Operation.UPD.id
                        }
                    }
                    mangaCategory[id].value.toString()
                } ?: record.uid
            }.getOrNull() ?: insertAndGetId {
                it[mangaId] = record.getOrMissing("mangaId", mangaUID).toUUID()
                it[categoryId] = record.getOrMissing("categoryId", categoryUID).toUUID()
                it[updateDate] = record.update
                it[lastModified] = currentTime
                it[operation] = record.op
            }.value.toString()
        }
        Operation.DEL.id -> {
            record.uid?.also { uid ->
                update({ id eq uid.toUUID() }) {
                    it[updateDate] = currentTime
                    it[lastModified] = currentTime
                    it[operation] = record.op
                }
            }
        }
        else -> invalidOperation(record.op)
    }

    private fun MangaTable.proceedTable(userId: UUID, record: TableData): String? = when (record.op) {
        Operation.INS.id, Operation.UPD.id -> {
            runCatching {
                getManga(userId, record)?.let { manga ->
                    if (record.update > manga[updateDate]) {
                        update({ id eq manga[id] }) {
                            record.run {
                                it[title] = getOrMissing("title")
                                it[sourceId] = getOrMissing("sourceId").toLong()
                                it[url] = getOrMissing("url")
                                it[coverUrl] = get("coverUrl")
                                it[updateDate] = update
                                it[lastModified] = currentTime
                                it[operation] = Operation.UPD.id
                            }
                        }
                    }
                    manga[id].value.toString()
                } ?: record.uid
            }.getOrNull() ?: insertAndGetId {
                record.run {
                    it[title] = getOrMissing("title")
                    it[sourceId] = getOrMissing("sourceId").toLong()
                    it[url] = getOrMissing("url")
                    it[coverUrl] = get("coverUrl")
                    it[updateDate] = update
                    it[lastModified] = currentTime
                    it[operation] = op
                }
            }.value.toString()
        }
        Operation.DEL.id -> {
            record.uid?.also { uid ->
                update({ id eq uid.toUUID() }) {
                    it[updateDate] = currentTime
                    it[lastModified] = currentTime
                    it[operation] = record.op
                }
            }
        }
        else -> invalidOperation(record.op)
    }

    private fun CategoryTable.getUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin this).slice(columns)
            .select { (UserCategoryTable.userId eq userId) and (lastModified greater lastUpdate) }.withDistinct()
            .forEach {
                val lastModified = it[lastModified]
                val operation = it[operation].value
                val updateMap = mapOf(
                    "name" to it[name],
                    "order" to it[order].toString(),
                    "updateDate" to it[updateDate].toString()
                )
                val uid = it[CategoryTable.id].value.toString()
                result.change(uid, lastUpdate, lastModified, operation, updateMap)
            }
        return result
    }

    private fun MangaCategoryTable.getUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable innerJoin this).slice(columns)
            .select { (UserCategoryTable.userId eq userId) and (lastModified greater lastUpdate) }.withDistinct()
            .forEach {
                val lastModified = it[lastModified]
                val operation = it[operation].value
                val updateMap = mapOf(
                    "mangaId" to it[mangaId].value.toString(),
                    "categoryId" to it[categoryId].value.toString(),
                    "updateDate" to it[updateDate].toString()
                )
                val uid = it[id].value.toString()
                result.change(uid, lastUpdate, lastModified, operation, updateMap)
            }
        return result
    }

    private fun MangaTable.getUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin this).slice(columns)
            .select { (UserCategoryTable.userId eq userId) and (lastModified greater lastUpdate) }.withDistinct()
            .forEach {
                val lastModified = it[lastModified]
                val operation = it[operation].value
                val updateMap = mapOf(
                    "title" to it[title],
                    "sourceId" to it[sourceId].toString(),
                    "url" to it[url],
                    "coverUrl" to it[coverUrl],
                    "updateDate" to it[updateDate].toString()
                )
                val uid = it[id].value.toString()
                result.change(uid, lastUpdate, lastModified, operation, updateMap)
            }
        return result
    }

    private fun ChapterTable.getUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin MangaTable innerJoin this).slice(
            columns
        ).select { (UserCategoryTable.userId eq userId) and (lastModified greater lastUpdate) }.withDistinct()
            .forEach {
                val lastModified = it[lastModified]
                val operation = it[operation].value
                val updateMap = mapOf(
                    "name" to it[name],
                    "mangaId" to it[mangaId].value.toString(),
                    "url" to it[url],
                    "isRead" to it[isRead].toString(),
                    "number" to it[number].toString(),
                    "lastPageRead" to it[lastPageRead].toString(),
                    "uploadDate" to it[uploadDate].toString(),
                    "updateDate" to it[updateDate].toString()
                )
                val uid = it[ChapterTable.id].value.toString()
                result.change(uid, lastUpdate, lastModified, operation, updateMap)
            }
        return result
    }

    private fun ArrayList<ResponseSyncDataItem>.change(
        uid: String, lastUpdate: Long, updateDate: Long, operation: Int, updateMap: Map<String, String?>
    ) {
        val afterUpdate = updateDate > lastUpdate
        when {
            afterUpdate && operation != Operation.DEL.id -> ResponseSyncDataItem(uid, operation, updateMap)
            afterUpdate && operation == Operation.DEL.id -> ResponseSyncDataItem(uid, Operation.DEL.id)
            else -> null
        }?.let { syncData -> add(syncData) }
    }

    private infix fun TableData.missing(field: String): Nothing =
        (HttpStatusCode.BadRequest to "$first: missing $field : $second").throwBase()

    private fun TableData.get(key: String): String? = second.data[key]
    private val TableData.op: Int get() = second.op
    private val TableData.uid: String? get() = second.uid
    private val TableData.update: Long get() = second.update

    private fun TableData.getOrMissing(field: String, default: String? = null): String =
        get(field) ?: default ?: (this missing field)

    private fun <T> List<T>.takeIfNotEmpty(): List<T>? = takeIf { it.isNotEmpty() }
    private fun invalidOperation(id: Int): Nothing =
        (HttpStatusCode.Conflict to "invalid operation id '$id'").throwBase()

    private val currentTime get() = Date().time
}
package com.kwsilence.db.repository

import com.kwsilence.db.Operation
import com.kwsilence.db.model.tableKeyOrder
import com.kwsilence.db.model.tables
import com.kwsilence.db.table.auth.UserTable
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

class SyncRepository {
    fun update(userId: UUID, data: DataUpdate): Map<String, List<ResponseDataUpdateItem>> {
        val result = HashMap<String, ArrayList<ResponseDataUpdateItem>>()
        transaction {
            tableKeyOrder.forEach { key ->
                data[key]?.also { result[key] = ArrayList() }?.forEach { updateItem ->
                    val record = key to updateItem
                    when (val table = tables[key]) {
                        is CategoryTable -> {
                            table.proceedTable(record)?.also { stringUID ->
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
                    }?.let { uid ->
                        result[key]?.add(ResponseDataUpdateItem(uid = uid, lid = updateItem.lid))
                    }
                }
            }
        }
        return result
    }

    fun sync(userId: UUID, data: SyncData): Map<String, List<ResponseSyncDataItem>> = transaction {
        val lastUpdate = data.lastUpdate
        HashMap<String, List<ResponseSyncDataItem>>().apply {
            getCategoryUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("category", it) }
            getMangaUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("manga", it) }
            getMangaCategoryUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("manga_category", it) }
            getChapterUpdates(userId, lastUpdate).takeIfNotEmpty()?.let { put("chapter", it) }
        }
    }

    private fun CategoryTable.getCategory(uid: String) = select { id eq uid.toUUID() }.first()

    private fun ChapterTable.getChapter(uid: String) = select { id eq uid.toUUID() }.first()

    private fun ChapterTable.getChapter(userId: UUID, record: TableData, mangaUID: String?) = runCatching {
        val mUID = record.getOrMissing("mangaId", mangaUID).toUUID()
        val rURL = record.getOrMissing("url")
        (record.uid)?.toUUID()?.let { uid ->
            select { (id eq uid) }.firstOrNull()
        } ?: (this innerJoin MangaTable innerJoin MangaCategoryTable innerJoin CategoryTable
                innerJoin UserCategoryTable innerJoin UserTable)
            .slice(columns)
            .select { (mangaId eq mUID) and (url eq rURL) and (UserTable.id eq userId) }.firstOrNull()
    }.getOrNull()

    private fun MangaCategoryTable.getMangaCategory(uid: String) = select { id eq uid.toUUID() }.first()

    private fun MangaCategoryTable.getMangaCategory(
        userId: UUID,
        record: TableData,
        mangaUID: String?,
        categoryUID: String?
    ) = runCatching {
        val mUID = record.getOrMissing("mangaId", mangaUID).toUUID()
        val cUID = record.getOrMissing("categoryId", categoryUID).toUUID()
        record.uid?.toUUID()?.let { uid ->
            select { (id eq uid) }.firstOrNull()
        } ?: (this innerJoin CategoryTable innerJoin UserCategoryTable innerJoin UserTable)
            .slice(columns)
            .select { (mangaId eq mUID) and (categoryId eq cUID) and (UserTable.id eq userId) }
            .firstOrNull()
    }.getOrNull()

    private fun MangaTable.getManga(uid: String) = select { id eq uid.toUUID() }.first()

    private fun MangaTable.getManga(userId: UUID, record: TableData) = runCatching {
        val rSourceId = record.getOrMissing("sourceId").toLong()
        val rURL = record.getOrMissing("url")
        record.uid?.toUUID()?.let { uid ->
            select { (id eq uid) }.firstOrNull()
        } ?: (this innerJoin MangaCategoryTable innerJoin CategoryTable innerJoin UserCategoryTable innerJoin UserTable)
            .slice(columns)
            .select { (sourceId eq rSourceId) and (url eq rURL) and (UserTable.id eq userId) }.firstOrNull()
    }.getOrNull()


    private infix fun Map<String, List<ResponseDataUpdateItem>>.getMangaId(record: DataUpdateItem): String? =
        record.data["_mangaId"]?.toInt()?.let { mangaId -> get("manga")?.find { it.lid == mangaId }?.uid }

    private infix fun Map<String, List<ResponseDataUpdateItem>>.getCategoryId(record: DataUpdateItem): String? =
        record.data["_categoryId"]?.toInt()?.let { categoryId -> get("category")?.find { it.lid == categoryId }?.uid }

    private fun CategoryTable.proceedTable(record: TableData): String? = when (record.op) {
        Operation.UPD.id -> {
            record.uid?.also { uid ->
                runCatching {
                    if (record.update > getCategory(uid)[updateDate]) {
                        update({ id eq uid.toUUID() }) {
                            it[name] = record.getOrMissing("name")
                            it[updateDate] = record.update
                            it[lastModified] = updateTime
                            it[operation] = record.op
                        }
                    }
                }
            }
        }
        Operation.INS.id -> {
            runCatching {
                record.uid!!.also { getCategory(it) }
            }.getOrNull() ?: insertAndGetId {
                it[name] = record.getOrMissing("name")
                record.update.let { date ->
                    it[updateDate] = date
                    it[createDate] = date
                }
                it[lastModified] = updateTime
                it[operation] = record.op
            }.value.toString()
        }
        Operation.DEL.id -> {
            record.uid?.also { uid ->
                update({ id eq uid.toUUID() }) {
                    it[updateDate] = updateTime
                    it[lastModified] = updateTime
                    it[operation] = record.op
                }
            }
        }
        else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
    }

    private fun UserCategoryTable.proceedTable(userUID: UUID, categoryUID: UUID, op: Int) {
        when (op) {
            Operation.INS.id -> {
                select {
                    (userId eq userUID) and (categoryId eq categoryUID)
                }.firstOrNull() ?: insert {
                    it[userId] = userUID
                    it[categoryId] = categoryUID
                    updateTime.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[lastModified] = updateTime
                    it[operation] = op
                }
            }
            Operation.DEL.id -> {
                update({ categoryId eq categoryUID }) {
                    it[updateDate] = updateTime
                    it[lastModified] = updateTime
                    it[operation] = op
                }
            }
        }
    }

    private fun ChapterTable.proceedTable(
        userId: UUID,
        record: Pair<String, DataUpdateItem>,
        mangaUID: String? = null
    ): String? =
        when (record.op) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    runCatching {
                        if (record.update > getChapter(uid)[updateDate]) {
                            update({ id eq uid.toUUID() }) {
                                record.run {
                                    it[name] = getOrMissing("name")
                                    it[url] = getOrMissing("url")
                                    it[isRead] = getOrMissing("isRead").toBoolean()
                                    it[number] = getOrMissing("number").toFloat()
                                    it[lastPageRead] = getOrMissing("lastPageRead").toInt()
                                    it[uploadDate] = getOrMissing("uploadDate").toLong()
                                    it[updateDate] = update
                                    it[lastModified] = updateTime
                                    it[operation] = op
                                }
                            }
                        }
                    }
                }
            }
            Operation.INS.id -> {
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
                                    it[lastModified] = updateTime
                                    it[operation] = Operation.UPD.id
                                }
                            }
                        }
                        chapter[id].toString()
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
                    }
                    record.update.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[lastModified] = updateTime
                    it[operation] = record.op
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[lastModified] = updateTime
                        it[operation] = record.op
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
        }


    private fun MangaCategoryTable.proceedTable(
        userId: UUID,
        record: TableData,
        mangaUID: String?,
        categoryUID: String?
    ): String? =
        when (record.op) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    runCatching {
                        if (record.update > getMangaCategory(uid)[updateDate]) {
                            update({ id eq uid.toUUID() }) {
                                it[mangaId] = record.getOrMissing("mangaId", mangaUID).toUUID()
                                it[categoryId] = record.getOrMissing("categoryId", categoryUID).toUUID()
                                it[updateDate] = record.update
                                it[lastModified] = updateTime
                                it[operation] = record.op
                            }
                        }
                    }
                }
            }
            Operation.INS.id -> {
                runCatching {
                    getMangaCategory(userId, record, mangaUID, categoryUID)?.let { mangaCategory ->
                        if (record.update > mangaCategory[updateDate]) {
                            update({ id eq mangaCategory[id] }) {
                                it[mangaId] = record.getOrMissing("mangaId", mangaUID).toUUID()
                                it[categoryId] = record.getOrMissing("categoryId", categoryUID).toUUID()
                                it[updateDate] = record.update
                                it[lastModified] = updateTime
                                it[operation] = Operation.UPD.id
                            }
                        }
                        mangaCategory[id].toString()
                    } ?: record.uid
                }.getOrNull() ?: insertAndGetId {
                    it[mangaId] = record.getOrMissing("mangaId", mangaUID).toUUID()
                    it[categoryId] = record.getOrMissing("categoryId", categoryUID).toUUID()
                    record.update.let { date ->
                        it[updateDate] = date
                        it[createDate] = date
                    }
                    it[lastModified] = updateTime
                    it[operation] = record.op
                }.value.toString()
            }
            Operation.DEL.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[updateDate] = updateTime
                        it[lastModified] = updateTime
                        it[operation] = record.op
                    }
                }
            }
            else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
        }

    private fun MangaTable.proceedTable(userId: UUID, record: TableData): String? = when (record.op) {
        Operation.UPD.id -> {
            record.uid?.also { uid ->
                runCatching {
                    if (record.update > getManga(uid)[updateDate]) {
                        update({ id eq uid.toUUID() }) {
                            record.run {
                                it[title] = getOrMissing("title")
                                it[sourceId] = getOrMissing("sourceId").toLong()
                                it[url] = getOrMissing("url")
                                it[coverUrl] = get("coverUrl")
                            }
                            it[updateDate] = record.update
                            it[lastModified] = updateTime
                            it[operation] = record.op
                        }
                    }
                }
            }
        }
        Operation.INS.id -> {
            runCatching {
                getManga(userId, record)?.let { manga ->
                    if (record.update > manga[updateDate]) {
                        update({ id eq manga[id] }) {
                            record.run {
                                it[title] = getOrMissing("title")
                                it[sourceId] = getOrMissing("sourceId").toLong()
                                it[url] = getOrMissing("url")
                                it[coverUrl] = get("coverUrl")
                            }
                            it[updateDate] = record.update
                            it[lastModified] = updateTime
                            it[operation] = Operation.UPD.id
                        }
                    }
                    manga[id].toString()
                } ?: record.uid
            }.getOrNull() ?: insertAndGetId {
                record.run {
                    it[title] = getOrMissing("title")
                    it[sourceId] = getOrMissing("sourceId").toLong()
                    it[url] = getOrMissing("url")
                    it[coverUrl] = get("coverUrl")
                }
                record.update.let { date ->
                    it[updateDate] = date
                    it[createDate] = date
                }
                it[lastModified] = updateTime
                it[operation] = record.op
            }.value.toString()
        }
        Operation.DEL.id -> {
            record.uid?.also { uid ->
                update({ id eq uid.toUUID() }) {
                    it[updateDate] = updateTime
                    it[lastModified] = updateTime
                    it[operation] = record.op
                }
            }
        }
        else -> (HttpStatusCode.Conflict to "invalid operation id '${record.op}'").throwBase()
    }

    private fun getCategoryUpdates(userId: UUID, lastUpdate: Long): List<ResponseSyncDataItem> {
        val result = ArrayList<ResponseSyncDataItem>()
        (UserCategoryTable innerJoin CategoryTable)
            .slice(CategoryTable.columns)
            .select { (UserCategoryTable.userId eq userId) and (CategoryTable.lastModified greater lastUpdate) }
            .withDistinct()
            .forEach {
                val createDate = it[CategoryTable.createDate]
                val updateDate = it[CategoryTable.lastModified]
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
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable)
            .slice(MangaCategoryTable.columns)
            .select { (UserCategoryTable.userId eq userId) and (MangaCategoryTable.lastModified greater lastUpdate) }
            .withDistinct()
            .forEach {
                val createDate = it[MangaCategoryTable.createDate]
                val updateDate = it[MangaCategoryTable.lastModified]
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
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin MangaTable)
            .slice(MangaTable.columns)
            .select { (UserCategoryTable.userId eq userId) and (MangaTable.lastModified greater lastUpdate) }
            .withDistinct()
            .forEach {
                val createDate = it[MangaTable.createDate]
                val updateDate = it[MangaTable.lastModified]
                val operation = it[MangaTable.operation].value
                val updateMap = mapOf(
                    "title" to it[MangaTable.title],
                    "sourceId" to it[MangaTable.sourceId].toString(),
                    "url" to it[MangaTable.url],
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
        (UserCategoryTable innerJoin CategoryTable innerJoin MangaCategoryTable innerJoin MangaTable innerJoin ChapterTable)
            .slice(ChapterTable.columns)
            .select { (UserCategoryTable.userId eq userId) and (ChapterTable.lastModified greater lastUpdate) }
            .withDistinct()
            .forEach {
                val createDate = it[ChapterTable.createDate]
                val updateDate = it[ChapterTable.lastModified]
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
        val afterCreate = createDate > lastUpdate
        val afterUpdate = updateDate > lastUpdate
        when {
            afterCreate && operation != Operation.DEL.id -> ResponseSyncDataItem(uid, Operation.INS.id, updateMap)
            afterUpdate && operation != Operation.DEL.id -> ResponseSyncDataItem(uid, operation, updateMap)
            afterUpdate && operation == Operation.DEL.id -> ResponseSyncDataItem(uid, Operation.DEL.id)
            else -> null
        }?.let { syncData -> add(syncData) }
    }

    private infix fun String.missing(field: String): Nothing =
        (HttpStatusCode.BadRequest to "$this: missing $field").throwBase()

    private fun TableData.get(key: String): String? = second.data[key]
    private val TableData.op: Int get() = second.op
    private val TableData.uid: String? get() = second.uid
    private val TableData.update: Long get() = second.update

    private fun TableData.getOrMissing(field: String, default: String? = null): String =
        get(field) ?: default ?: (first missing field)

    private fun <T> List<T>.takeIfNotEmpty(): List<T>? = takeIf { it.isNotEmpty() }

    private val updateTime get() = Date().time
}
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
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class MangaRepository {
    fun update(userId: Int, data: List<DataUpdate>): List<ResponseDataUpdate> {
        val result = ArrayList<ResponseDataUpdate>()
        transaction {
            data.forEach { record ->
                when (val table = tables[record.tb]) {
                    is CategoryTable -> table.proceedTable(record)
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
                    is UserCategoryTable -> {
                        val categoryUID = result getCategoryId record
                        table.proceedTable(userId, record, categoryUID)
                    }
                    else -> (HttpStatusCode.Conflict to "invalid table '${record.tb}'").throwBase()
                }?.let { uid ->
                    result.add(ResponseDataUpdate(uid = uid, lid = record.lid, tb = record.tb))
                }
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

    private fun UserCategoryTable.proceedTable(userId: Int, record: DataUpdate, categoryUID: UUID?): String? {
        val data = record.data
        return when (record.op) {
            Operation.UPD.id -> {
                record.uid?.also { uid ->
                    update({ id eq uid.toUUID() }) {
                        it[categoryId] = data["categoryId"]?.toUUID() ?: categoryUID ?: ("UsrCat" missing "categoryId")
                        it[updateDate] = updateTime
                        it[operation] = Operation.UPD.id
                    }
                }
            }
            Operation.INS.id -> {
                insertAndGetId {
                    it[this.userId] = userId
                    it[categoryId] = data["categoryId"]?.toUUID() ?: categoryUID ?: ("UsrCat" missing "categoryId")
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

    fun sync(userId: Int, data: List<SyncData>): List<ResponseSyncData> {
        val result = ArrayList<ResponseSyncData>()
        transaction {

        }
        return result
    }

    private fun String.toUUID(): UUID = UUID.fromString(this)
    private infix fun String.missing(field: String): Nothing =
        (HttpStatusCode.BadRequest to "$this: missing $field").throwBase()

    private val updateTime = Date().time
}
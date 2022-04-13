package com.kwsilence.service

import com.kwsilence.db.repository.MangaRepository
import com.kwsilence.service.data.DataUpdate
import com.kwsilence.service.data.ResponseDataUpdate
import com.kwsilence.service.data.ResponseSyncData
import com.kwsilence.service.data.SyncData
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.TokenUtil
import com.kwsilence.util.TokenUtil.toUUID
import io.ktor.http.HttpStatusCode

class SyncService(private val mangaRepository: MangaRepository) {
    fun update(token: String?, data: List<DataUpdate>?): List<ResponseDataUpdate> {
        val userId = (TokenUtil.checkAuthToken(token)["usr"] as? String)?.toUUID()
        userId ?: HttpStatusCode.Unauthorized.throwBase()
        data ?: (HttpStatusCode.BadRequest to "update data not set").throwBase()
        return mangaRepository.update(userId, data)
    }

    fun sync(token: String?, data: SyncData?): List<ResponseSyncData> {
        val userId = (TokenUtil.checkAuthToken(token)["usr"] as? String)?.toUUID()
        userId ?: HttpStatusCode.Unauthorized.throwBase()
        data ?: (HttpStatusCode.BadRequest to "sync data not set").throwBase()
        return mangaRepository.sync(userId, data)
    }
}
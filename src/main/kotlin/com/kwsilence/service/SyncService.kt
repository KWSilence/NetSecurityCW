package com.kwsilence.service

import com.kwsilence.db.repository.MangaRepository
import com.kwsilence.service.data.DataUpdate
import com.kwsilence.service.data.ResponseDataUpdate
import com.kwsilence.util.ExceptionUtil.throwBase
import com.kwsilence.util.TokenUtil
import io.ktor.http.HttpStatusCode

class SyncService(private val mangaRepository: MangaRepository) {
    fun update(token: String?, data: List<DataUpdate>?): List<ResponseDataUpdate> {
        val userId = TokenUtil.checkAuthToken(token).parseClaimsJws(token).body["usr"] as? Int
        userId ?: HttpStatusCode.Unauthorized.throwBase()
        data ?: (HttpStatusCode.BadRequest to "update data not set").throwBase()
        return mangaRepository.update(userId, data)
    }
}
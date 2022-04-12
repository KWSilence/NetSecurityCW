package com.kwsilence.service.data

import kotlinx.serialization.Serializable


@Serializable
data class SyncData(
    val lastUpdate: Long
)

@Serializable
data class ResponseSyncData(
    val uid: String,
    val tb: String,
    val op: Int,
    val data: Map<String, String?>? = null
)
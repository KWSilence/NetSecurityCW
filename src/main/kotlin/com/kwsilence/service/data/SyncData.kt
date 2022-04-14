package com.kwsilence.service.data

import kotlinx.serialization.Serializable


@Serializable
data class SyncData(
    val lastUpdate: Long
)

@Serializable
data class ResponseSyncDataItem(
    val uid: String,
    val op: Int,
    val data: Map<String, String?>? = null
)
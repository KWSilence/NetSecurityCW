package com.kwsilence.service.data

import kotlinx.serialization.Serializable

typealias DataUpdate = Map<String, List<DataUpdateItem>?>
typealias TableData = Pair<String, DataUpdateItem>

@Serializable
data class DataUpdateItem(
    val uid: String? = null,
    val lid: Int,
    val op: Int,
    val update: Long,
    val data: Map<String, String?> = mapOf()
)

@Serializable
data class ResponseDataUpdateItem(
    val uid: String,
    val lid: Int
)

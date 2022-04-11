package com.kwsilence.service.data

import kotlinx.serialization.Serializable

@Serializable
data class DataUpdate(
    val uid: String? = null,
    val lid: Int,
    val tb: String,
    val op: Int,
    val data: Map<String, String?> = mapOf()
)

@Serializable
data class ResponseDataUpdate(
    val uid: String,
    val lid: Int,
    val tb: String
)

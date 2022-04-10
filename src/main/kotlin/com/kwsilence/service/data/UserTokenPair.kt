package com.kwsilence.service.data

@kotlinx.serialization.Serializable
data class UserTokenPair(
    val authToken: String,
    val refreshToken: String
)

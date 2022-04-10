package com.kwsilence.service.data

@kotlinx.serialization.Serializable
data class UserCred(
    val mail: String?,
    val pass: String?
)

package com.kwsilence.service.data

@kotlinx.serialization.Serializable
data class PasswordReset(
    val newPassword: String?
)

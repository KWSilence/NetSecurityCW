package com.kwsilence.db

enum class MangaStatus(val id: Int, val type: String) {
    UNKNOWN(0, "Unknown"),
    ONGOING(1, "Ongoing"),
    COMPLETED(2, "Completed"),
    LICENSED(3, "Licensed")
}
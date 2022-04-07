package com.kwsilence.db

enum class Tokens(val id: Int, val type: String) {
    RESET(1, "Reset"),
    CONFIRM(2, "Confirm"),
    REFRESH(3, "Refresh")
}
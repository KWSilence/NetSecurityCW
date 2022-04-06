package com.kwsilence.db

object DatabaseHelper {
    enum class Links(val id: Int, val type: String) {
        RESET_LINK(1, "ResetLink"), CONFIRM_LINK(2, "ConfirmLink")
    }
}
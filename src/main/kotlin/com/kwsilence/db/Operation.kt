package com.kwsilence.db

enum class Operation(val id: Int, val type: String) {
    INS(0, "Insert"),
    UPD(1, "Update"),
    DEL(2, "Delete")
}
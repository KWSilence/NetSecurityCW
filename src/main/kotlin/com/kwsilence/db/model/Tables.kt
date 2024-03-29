package com.kwsilence.db.model

import com.kwsilence.db.table.manga.CategoryTable
import com.kwsilence.db.table.manga.ChapterTable
import com.kwsilence.db.table.manga.MangaCategoryTable
import com.kwsilence.db.table.manga.MangaTable

val tableKeyOrder = listOf(
    listOf("category", "manga"),
    listOf("manga_category", "chapter")
)

val tables = mapOf(
    "category" to CategoryTable,
    "manga" to MangaTable,
    "manga_category" to MangaCategoryTable,
    "chapter" to ChapterTable
)
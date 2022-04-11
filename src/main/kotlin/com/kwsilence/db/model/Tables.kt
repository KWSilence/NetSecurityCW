package com.kwsilence.db.model

import com.kwsilence.db.table.manga.CategoryTable
import com.kwsilence.db.table.manga.ChapterTable
import com.kwsilence.db.table.manga.MangaCategoryTable
import com.kwsilence.db.table.manga.MangaTable
import com.kwsilence.db.table.manga.UserCategoryTable

val tables = mapOf(
    "category" to CategoryTable,
    "chapter" to ChapterTable,
    "manga_category" to MangaCategoryTable,
    "manga" to MangaTable,
    "user_category" to UserCategoryTable
)
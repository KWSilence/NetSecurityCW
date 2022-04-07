package com.kwsilence.util

object HtmlUtil {
    const val mailType = "text/html; charset=utf-8"
    fun hyperlink(link: String, text: String): String = "<a href=$link>$text</a>"
}
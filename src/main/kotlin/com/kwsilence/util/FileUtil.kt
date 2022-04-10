package com.kwsilence.util

import com.kwsilence.mserver.BuildConfig
import java.io.File

object FileUtil {
    private fun File.takeIfExist(): File? = takeIf { it.exists() && it.isFile }
    fun shared(path: String?): File? = path?.let { File("${BuildConfig.sharePath}/$path") }?.takeIfExist()
}
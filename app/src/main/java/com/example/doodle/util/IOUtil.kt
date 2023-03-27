package com.example.doodle.util

import java.io.Closeable
import java.io.IOException

object IOUtil {
    fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: IOException) {
                // ignore
            }
        }
    }
}
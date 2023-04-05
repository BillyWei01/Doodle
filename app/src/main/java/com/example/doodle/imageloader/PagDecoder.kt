package com.example.doodle.imageloader

import com.example.doodle.config.AppConfig
import io.github.doodle.DecodingInfo
import io.github.doodle.interfaces.AnimatedDecoder
import org.libpag.PAGFile

object PagDecoder : AnimatedDecoder {
    private const val ASSET_PREFIX = "file:///android_asset/"
    private val pagHeader = "PAG".toByteArray()

    private fun isPagFile(header: ByteArray): Boolean {
        return header[0] == pagHeader[0] &&
            header[1] == pagHeader[1] &&
            header[2] == pagHeader[2]
    }

    override fun decode(info: DecodingInfo): Any? {
        if (!isPagFile(info.header)) {
            return null
        }
        info.filePath?.let {
            return PAGFile.Load(it)
        }
        if (info.path.startsWith(ASSET_PREFIX)) {
            val assetName = info.path.substring(ASSET_PREFIX.length)
            return PAGFile.Load(AppConfig.appContext.assets, assetName)
        }
        return PAGFile.Load(info.data)
    }
}
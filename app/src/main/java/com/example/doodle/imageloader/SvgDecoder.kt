package com.example.doodle.imageloader

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import com.example.doodle.util.LogUtil
import io.github.doodle.DecodingInfo
import io.github.doodle.Doodle
import io.github.doodle.enums.DecodeFormat
import io.github.doodle.interfaces.BitmapDecoder
import java.io.ByteArrayInputStream
import kotlin.math.ceil
import kotlin.math.roundToInt

object SvgDecoder : BitmapDecoder {
    private val svgHeader = "<svg".toByteArray()
    private val xmlHeader = "<?xml".toByteArray()

    private fun isSVG(header: ByteArray): Boolean {
        return svgHeader[0] == header[0] &&
            svgHeader[1] == header[1] &&
            svgHeader[2] == header[2] &&
            svgHeader[3] == header[3]
    }

    private fun isXmlFile(header: ByteArray): Boolean {
        return xmlHeader[0] == header[0] &&
            xmlHeader[1] == header[1] &&
            xmlHeader[2] == header[2] &&
            xmlHeader[3] == header[3] &&
            xmlHeader[4] == header[4]
    }

    override fun decode(info: DecodingInfo): Bitmap? {
        if (!(isSVG(info.header) || isXmlFile(info.header))) {
            return null;
        }
        try {
            val svg = SVG.getFromInputStream(ByteArrayInputStream(info.data))
            val config = if (info.decodeFormat == DecodeFormat.RGB_565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            val sourceWidth = ceil(svg.documentWidth.toDouble()).toInt()
            val sourceHeight = ceil(svg.documentHeight.toDouble()).toInt()
            val width: Int
            val height: Int
            if (sourceWidth <= 0 || sourceWidth <= 0) {
                width = info.targetWidth
                height = info.targetHeight
            } else {
                // Scale up a svg image will not lose precision,
                // so we always set enableUpscale to be true.
                val scale = Doodle.getScale(sourceWidth, sourceHeight,
                    info.targetWidth, info.targetHeight, info.clipType, true/*info.enableUpscale*/)
                width = (scale * sourceWidth).roundToInt()
                height = (scale * sourceHeight).roundToInt()
            }
            val bitmap = Bitmap.createBitmap(width, height, config)
            val canvas = Canvas(bitmap)
            svg.documentWidth = width.toFloat()
            svg.documentHeight = height.toFloat()
            svg.renderToCanvas(canvas)
            return bitmap
        } catch (e: Exception) {
            LogUtil.e("SvgDecoder", e)
        }
        return null
    }
}
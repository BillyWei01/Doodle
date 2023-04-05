package com.example.doodle.imageloader

import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.ImageInfo
import android.graphics.ImageDecoder.OnPartialImageListener
import android.graphics.drawable.Drawable
import android.os.Build
import io.github.doodle.DecodingInfo
import io.github.doodle.enums.MediaType
import io.github.doodle.interfaces.AnimatedDecoder
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object AnimatedWebpDecoder : AnimatedDecoder {
    override fun decode(info: DecodingInfo): Any? {
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.mediaType == MediaType.WEBP_ANIMATED)) {
            return null
        }
        val targetWidth = info.targetWidth
        val targetHeight = info.targetHeight
        val source = ImageDecoder.createSource(ByteBuffer.wrap(info.data))
        return ImageDecoder.decodeDrawable(source) { decoder: ImageDecoder, imageInfo: ImageInfo, _: ImageDecoder.Source? ->
            decoder.onPartialImageListener = OnPartialImageListener { false }
            val size = imageInfo.size
            val sourceWidth = size.width
            val sourceHeight = size.height
            if (sourceWidth > 0 && sourceHeight > 0 && targetWidth > 0 && targetHeight > 0
                && (sourceWidth > targetWidth || sourceHeight > targetHeight)) {
                val scale = if (sourceWidth * targetHeight > targetWidth * sourceHeight) {
                    targetWidth.toFloat() / sourceWidth.toFloat()
                } else {
                    targetHeight.toFloat() / sourceHeight.toFloat()
                }
                val w = (scale * size.width).roundToInt()
                val h = (scale * size.height).roundToInt()
                decoder.setTargetSize(w, h)
            }
        }
    }
}

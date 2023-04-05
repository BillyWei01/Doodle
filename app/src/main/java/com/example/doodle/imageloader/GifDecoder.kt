package com.example.doodle.imageloader

import io.github.doodle.DecodingInfo
import io.github.doodle.enums.MediaType
import io.github.doodle.interfaces.AnimatedDecoder
import pl.droidsonroids.gif.GifDrawable

object GifDecoder : AnimatedDecoder {
    override fun decode(info: DecodingInfo): Any? {
        if (info.mediaType != MediaType.GIF) {
            return null
        }
        val gifDrawable = GifDrawable(info.data)
        if (gifDrawable.numberOfFrames == 1) {
            return gifDrawable.currentFrame
        }
        return gifDrawable
    }
}
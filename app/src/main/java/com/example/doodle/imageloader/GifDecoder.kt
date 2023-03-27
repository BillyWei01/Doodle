package com.example.doodle.imageloader

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.doodle.config.AppConfig
import io.github.doodle.DecodingInfo
import io.github.doodle.enums.MediaType
import io.github.doodle.interfaces.DrawableDecoder
import pl.droidsonroids.gif.GifDrawable

object GifDecoder : DrawableDecoder {
    override fun decode(info: DecodingInfo): Drawable? {
        if (info.mediaType != MediaType.GIF) {
            return null
        }
        val gifDrawable = GifDrawable(info.data)
        if (gifDrawable.numberOfFrames == 1) {
            return BitmapDrawable(AppConfig.appContext.resources, gifDrawable.currentFrame)
        }
        return gifDrawable
    }
}
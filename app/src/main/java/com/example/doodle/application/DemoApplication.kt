package com.example.doodle.application

import android.app.Application
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.doodle.config.AppConfig
import com.example.doodle.config.Logger
import com.example.doodle.album.AlbumImageLoader
import com.example.doodle.imageloader.AnimatedWebpDecoder
import com.example.doodle.imageloader.GifDecoder
import com.example.doodle.imageloader.IOExecutor
import com.example.doodle.imageloader.OkHttpSourceFetcher
import com.example.doodle.util.UncaughtExceptionInterceptor
import io.github.album.EasyAlbum
import io.github.doodle.Doodle


class DemoApplication : Application() {
    private val procName: String by lazy {
        ProcessUtil.getProcessName(this) ?: AppConfig.APPLICATION_ID
    }

    override fun onCreate() {
        super.onCreate()
        initApplication(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (procName == AppConfig.APPLICATION_ID) {
            Doodle.trimMemory(level)
        }
    }

    private fun initApplication(context: Application) {
        if (procName == AppConfig.APPLICATION_ID) {
            AppConfig.setAppContext(context)

            UncaughtExceptionInterceptor.init()

            Doodle.config()
                .setLogger(Logger)
                .setExecutor(IOExecutor)
                .setHttpSourceFetcher(OkHttpSourceFetcher)
                .addDrawableDecoders(GifDecoder)
                .addDrawableDecoders(AnimatedWebpDecoder)

            EasyAlbum.config()
                .setLogger(Logger)
                .setExecutor(IOExecutor)
                .setImageLoader(AlbumImageLoader)
                .setItemAnimator(DefaultItemAnimator())
        }
    }
}

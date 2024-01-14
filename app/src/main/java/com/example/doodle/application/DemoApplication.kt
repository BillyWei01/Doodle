package com.example.doodle.application

import android.app.Application
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.doodle.config.AppConfig
import com.example.doodle.config.Logger
import com.example.doodle.album.AlbumImageLoader
import com.example.doodle.imageloader.AnimatedWebpDecoder
import com.example.doodle.imageloader.GifDecoder
import com.example.doodle.imageloader.OkHttpSourceFetcher
import com.example.doodle.imageloader.PagDecoder
import com.example.doodle.imageloader.SvgDecoder
import com.example.doodle.util.UncaughtExceptionInterceptor
import io.github.album.EasyAlbum
import io.github.doodle.Doodle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor


class DemoApplication : Application() {
    private val appProcessName: String by lazy {
        ProcessUtil.getProcessName(this) ?: AppConfig.APPLICATION_ID
    }

    override fun onCreate() {
        super.onCreate()
        initApplication(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (appProcessName == AppConfig.APPLICATION_ID) {
            Doodle.trimMemory(level)
        }
    }

    private fun initApplication(context: Application) {
        if (appProcessName == AppConfig.APPLICATION_ID) {
            AppConfig.setAppContext(context)

            UncaughtExceptionInterceptor.init()

            val ioExecutor = Dispatchers.IO.asExecutor()

            Doodle.config()
                .setLogger(Logger)
                .setExecutor(ioExecutor)
                .setHttpSourceFetcher(OkHttpSourceFetcher)
                .addAnimatedDecoders(GifDecoder)
                .addAnimatedDecoders(AnimatedWebpDecoder)
                .addAnimatedDecoders(PagDecoder)
                .addBitmapDecoders(SvgDecoder)

            EasyAlbum.config()
                .setLogger(Logger)
                .setExecutor(ioExecutor)
                .setImageLoader(AlbumImageLoader)
                .setItemAnimator(DefaultItemAnimator())
        }
    }
}

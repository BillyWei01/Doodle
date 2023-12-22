package com.example.doodle.config

import android.util.Log
import com.example.doodle.util.LogUtil
import com.example.doodle.BuildConfig
import io.fastkv.interfaces.FastLogger
import io.github.album.interfaces.AlbumLogger
import io.github.doodle.interfaces.DLogger
import java.lang.Exception

object Logger : DLogger, FastLogger, AlbumLogger {
    override fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }

    override fun d(tag: String, message: String) {
        LogUtil.d(tag, message)
    }

    override fun i(name: String, message: String) {
        Log.i(name, message)
    }

    override fun w(name: String, e: Exception) {
        LogUtil.e(name, e)
    }

    override fun e(tag: String, e: Throwable) {
        LogUtil.e(tag, e)
    }

    override fun e(name: String, e: Exception) {
        LogUtil.e(name, e)
    }
}
package com.example.doodle.util

import android.util.Log
import com.example.doodle.config.AppConfig

object LogUtil {
    fun d(tag: String, msg: String) {
        if (AppConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    fun e(tag: String, e: Throwable) {
        Log.e(tag, e.message, e)
    }
}
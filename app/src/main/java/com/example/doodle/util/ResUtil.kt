package com.example.doodle.util

import com.example.doodle.config.AppConfig.appContext
import androidx.annotation.StringRes
import com.example.doodle.config.AppConfig

object ResUtil {
    fun getStr(@StringRes resId: Int): String {
        return appContext.getString(resId)
    }

    fun getStr(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return appContext.getString(resId, *formatArgs)
    }
}
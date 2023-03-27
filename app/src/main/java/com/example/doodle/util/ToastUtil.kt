package com.example.doodle.util

import com.example.doodle.config.AppConfig.appContext
import com.example.doodle.util.ResUtil.getStr
import android.widget.Toast
import androidx.annotation.StringRes

object ToastUtil {
    fun showTips(tips: String) {
        Toast.makeText(appContext, tips, Toast.LENGTH_SHORT).show()
    }

    fun showTips(@StringRes resID: Int) {
        Toast.makeText(appContext, getStr(resID), Toast.LENGTH_SHORT).show()
    }
}
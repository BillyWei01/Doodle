package com.example.doodle.util

import android.content.Context
import com.example.doodle.config.AppConfig.appContext
import android.net.ConnectivityManager
import com.example.doodle.config.AppConfig
import android.net.NetworkInfo

object NetworkUtil {
    val isConnected: Boolean
        get() {
            val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = manager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
}
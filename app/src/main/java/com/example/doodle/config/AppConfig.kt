package com.example.doodle.config

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.doodle.BuildConfig


object AppConfig {
    const val APPLICATION_ID = BuildConfig.APPLICATION_ID

    val uiHandler = Handler(Looper.getMainLooper())

    @JvmField
    val DEBUG = BuildConfig.DEBUG

    lateinit var appContext: Context
        private set

    fun setAppContext(context: Application) {
        appContext = context
    }
}
package com.example.doodle.config

import com.example.doodle.config.AppConfig


object PathManager {
    private val filesDir: String = AppConfig.appContext.filesDir.absolutePath
    val fastKVDir: String = "$filesDir/fastkv"
}
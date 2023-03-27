package com.example.doodle.remote.setting

import com.example.doodle.config.KVData

object SettingData : KVData("user_setting") {
    var showHidden by boolean("showHidden")
    var channels by string("channels")
    var collectPath by string("collectPath")
    var lastShowingFragment by string("lastShowingFragment")
}
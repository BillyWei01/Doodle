package com.example.doodle.remote.setting.path

import java.io.File
import java.io.FileFilter

class PathFilter : FileFilter {
    private var mShowHidden = false
    fun setShowHidden(showHidden: Boolean) {
        mShowHidden = showHidden
    }

    override fun accept(file: File): Boolean {
        return if (file.isDirectory) {
            mShowHidden || file.name[0] != '.'
        } else false
    }
}
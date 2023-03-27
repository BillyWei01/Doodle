package com.example.doodle.util


object UncaughtExceptionInterceptor : Thread.UncaughtExceptionHandler {
    private val mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, t: Throwable) {
        LogUtil.e("ExceptionInterceptor", t)
        if (mDefaultHandler !== this) {
            mDefaultHandler?.uncaughtException(thread, t)
        }
    }
}
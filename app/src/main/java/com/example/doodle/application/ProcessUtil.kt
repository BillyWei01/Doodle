package com.example.doodle.application

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.text.TextUtils
import android.util.Log
import com.example.doodle.config.AppConfig
import java.io.BufferedReader
import java.io.FileReader

object ProcessUtil {
    private const val TAG = "ProcessUtil"

    fun getProcessName(context: Context): String? {
        try {
            val name = currentProcessNameByApplication
            if (!name.isNullOrEmpty()) {
                return name
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
        }

        try {
            val name = getCurrentProcessNameByActivityManager(context)
            if (!name.isNullOrEmpty()) {
                return name
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
        }

        try {
            val name = processNameByCmd
            if (!name.isNullOrEmpty()) {
                return name
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString(), e)
        }

        try {
            val name = currentProcessNameByActivityThread
            if (!name.isNullOrEmpty()) {
                return name
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
        }
        return null
    }

    private fun getCurrentProcessNameByActivityManager(context: Context): String? {
        val pid = Process.myPid()
        val mActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (appProcess in mActivityManager.runningAppProcesses) {
            if (appProcess.pid == pid) {
                return appProcess.processName
            }
        }
        return null
    }

    private val currentProcessNameByApplication: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else null

    private val processNameByCmd: String?
        get() {
            try {
                BufferedReader(FileReader("/proc/" + Process.myPid() + "/cmdline")).use { reader ->
                    var processName = reader.readLine()
                    if (!TextUtils.isEmpty(processName)) {
                        processName = processName.trim { it <= ' ' }
                    }
                    return processName
                }
            } catch (e: Exception) {
                Log.e(TAG, "getProcessName read is fail. exception=$e")
            }
            return null
        }

    private val currentProcessNameByActivityThread: String?
        @SuppressLint("DiscouragedPrivateApi")
        get() {
            var processName: String? = null
            try {
                @SuppressLint("PrivateApi") val declaredMethod =
                    Class.forName("android.app.ActivityThread",
                        false, Application::class.java.classLoader)
                        .getDeclaredMethod("currentProcessName", *arrayOfNulls<Class<*>?>(0))
                declaredMethod.isAccessible = true
                val args = arrayOfNulls<Any>(0)
                val invoke = declaredMethod.invoke(null, *args)
                if (invoke is String) {
                    processName = invoke
                }
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
            }
            return processName
        }
}

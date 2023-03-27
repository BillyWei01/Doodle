package com.example.doodle.base

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.example.doodle.config.AppConfig
import com.example.doodle.util.ResUtil

abstract class BaseActivity : AppCompatActivity(){
    protected val mTag = this.javaClass.simpleName ?: "Activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppConfig.DEBUG) {
            Log.d(mTag, "onCreate")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AppConfig.DEBUG) {
            Log.d(mTag, "onDestroy")
        }
    }

    fun startActivity(activityClazz: Class<*>?) {
        val intent = Intent(this, activityClazz)
        startActivity(intent)
    }

    companion object {
        @JvmStatic
        protected fun getStr(resId: Int): String {
            return ResUtil.getStr(resId)
        }

        fun getStr(@StringRes resId: Int, vararg formatArgs: Any?): String {
            return ResUtil.getStr(resId, *formatArgs)
        }
    }
}
package com.example.doodle.util

import android.view.View


const val DELAY_TAG = 1123461123
const val DELAY_LAST_TAG = 1123460103
const val DEFAULT_DELAY = 400L

/**
 * Trigger click without duplicated click action.
 */
fun <T : View> T.onClick(time: Long = DEFAULT_DELAY, block: (T) -> Unit) {
    triggerDelay = time
    setOnClickListener {
        if (clickEnable()) {
            block(this)
        }
    }
}

private var <T : View> T.triggerLastTime: Long
    get() = if (getTag(DELAY_LAST_TAG) != null) getTag(DELAY_LAST_TAG) as Long else -(DEFAULT_DELAY+1)
    set(value) {
        setTag(DELAY_LAST_TAG, value)
    }

private var <T : View> T.triggerDelay: Long
    get() = if (getTag(DELAY_TAG) != null) getTag(DELAY_TAG) as Long else DEFAULT_DELAY
    set(value) {
        setTag(DELAY_TAG, value)
    }

private fun <T : View> T.clickEnable(): Boolean {
    var flag = false
    val currentClickTime = System.currentTimeMillis()
    if (currentClickTime - triggerLastTime >= triggerDelay) {
        flag = true
    }
    triggerLastTime = currentClickTime
    return flag
}
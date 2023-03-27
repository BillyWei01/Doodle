package com.example.doodle.util

import android.app.Activity
import android.content.ContextWrapper
import android.content.res.Resources
import android.view.View
import kotlin.math.roundToInt

object Utils {
    private var density = 1f

    private fun takeDensity(): Float {
        if (density == 1f) {
            density = Resources.getSystem().displayMetrics.density
        }
        return density
    }

    fun dp2px(dp: Float): Int {
        return (dp * takeDensity()).roundToInt()
    }

    fun pickActivity(view: View?): Activity? {
        var context = view?.context ?: return null
        if (context is Activity) {
            return context
        }
        if (context is ContextWrapper && context.baseContext is Activity) {
            return context.baseContext as Activity
        }
        context = view.rootView.context
        return if (context is Activity) context else null
    }
}
package com.example.doodle.imageloader

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import io.github.doodle.interfaces.CustomView
import org.libpag.PAGFile
import org.libpag.PAGView

/**
 * Compose ImageView and PAGView to support both of pag file and general image.
 */
class CompositePagView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), CustomView, Animatable {

    private val imageView: ImageView = ImageView(context)
    private val pagView: PAGView = PAGView(context)

    init {
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(imageView, params)
        addView(pagView, params)
    }

    override fun start() {
        pagView.play()
    }

    override fun stop() {
        pagView.stop()
    }

    override fun isRunning(): Boolean {
        return pagView.isPlaying
    }

    override fun setDrawable(drawable: Drawable?) {
        imageView.visibility = View.VISIBLE
        pagView.visibility = View.GONE
        imageView.setImageDrawable(drawable)
    }

    override fun getDrawable(): Drawable? {
        return imageView.drawable
    }

    override fun handleResult(result: Any?) {
        if (result is PAGFile) {
            imageView.visibility = View.GONE
            pagView.visibility = View.VISIBLE
            pagView.composition = result
            pagView.setRepeatCount(0)
        }
    }
}

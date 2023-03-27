package com.example.doodle.remote.widget

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet

class FlowImageView : AppCompatImageView {
    private var srcWidth = 0
    private var srcHeight = 0
    private var mSrcBitmap: Bitmap? = null
    private val mRect = Rect()
    private var mPieces: Array<Bitmap?>? = null
    private var m = 0
    private var n = 0
    var dw = 0
    var dh = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun setSourceSize(srcWidth: Int, srcHeight: Int) {
        this.srcWidth = srcWidth
        this.srcHeight = srcHeight
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        if (bm == mSrcBitmap) {
            return
        }
        if (bm == null) {
            mSrcBitmap = null
            mPieces = null
            return
        }
        dw = bm.width
        dh = bm.height
        if (dw > MAX_LENGTH || dh > MAX_LENGTH) {
            mSrcBitmap = bm
            m = (dw shr 12) + if (dw and 0xFFF == 0) 0 else 1
            n = (dh shr 12) + if (dh and 0xFFF == 0) 0 else 1
            val pieces: Array<Bitmap?> = arrayOfNulls(m * n)
            for (i in 0 until m) {
                for (j in 0 until n) {
                    val bw = if (i < m - 1) MAX_LENGTH else dw and 0xFFF
                    val bh = if (j < n - 1) MAX_LENGTH else dh and 0xFFF
                    pieces[m * i + j] = Bitmap.createBitmap(bm, i shl 12, j shl 12, bw, bh)
                }
            }
            mPieces = pieces
        } else {
            mSrcBitmap = null
            mPieces = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        val drawable = drawable
        if (drawable is BitmapDrawable) {
            val srcBitmap = drawable.bitmap
            if (srcBitmap != mSrcBitmap) {
                mSrcBitmap = null
                mPieces = null
            }
        } else {
            mSrcBitmap = null
            mPieces = null
        }
        val pieces = mPieces
        if (pieces != null) {
            for (i in 0 until m) {
                for (j in 0 until n) {
                    mRect.left = 0
                    mRect.top = 0
                    mRect.right = if (i < m - 1) MAX_LENGTH else dw and 0xFFF
                    mRect.bottom = if (j < n - 1) MAX_LENGTH else dh and 0xFFF
                    canvas.save()
                    canvas.translate((i shl 12).toFloat(), (j shl 12).toFloat())
                    val bitmap = pieces[m * i + j]
                    if (bitmap != null) {
                        canvas.drawBitmap(bitmap, null, mRect, null)
                    }
                    canvas.restore()
                }
            }
        } else {
            super.onDraw(canvas)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (srcWidth > 0 && srcHeight > 0) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height: Int = if (width > 0) {
                width * srcHeight / srcWidth
            } else {
                MeasureSpec.getSize(heightMeasureSpec)
            }
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    companion object {
        private const val MAX_LENGTH = 4096
    }
}

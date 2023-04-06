package com.example.doodle.main.test

import android.widget.ImageView

import io.github.doodle.Doodle
import com.example.doodle.R
import pl.droidsonroids.gif.GifDrawable

class TestGifActivity : BaseTestActivity()  {
    override fun loadImage(testIv: ImageView) {
        // Doodle cache the drawable by default,
        // and will stop the animated drawable when the ImageView detached from window.
        // You could reset the drawable after setting drawable to imageview if necessary.

//        Doodle.load(R.raw.fish)
//            .listen {
//                (testIv.drawable as? GifDrawable)?.seekToFrame(0)
//            }
//            .into(testIv)

        Doodle.load(R.raw.fish).into(testIv)
    }
}

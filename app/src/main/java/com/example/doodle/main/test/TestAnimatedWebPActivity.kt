package com.example.doodle.main.test

import android.widget.ImageView
import com.example.doodle.R
import io.github.doodle.Doodle

class TestAnimatedWebPActivity : BaseTestActivity()  {
    override fun loadImage(testIv: ImageView) {
        Doodle.load(R.raw.boat).into(testIv)
    }
}
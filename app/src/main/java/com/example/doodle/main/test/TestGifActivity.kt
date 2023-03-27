package com.example.doodle.main.test

import android.widget.ImageView

import io.github.doodle.Doodle
import com.example.doodle.R

class TestGifActivity : BaseTestActivity()  {
    override fun loadImage(testIv: ImageView) {
        Doodle.load(R.raw.fish).into(testIv)
    }
}

package com.example.doodle.main.test

import android.widget.ImageView
import com.example.doodle.R
import io.github.doodle.Doodle

class TestSvgActivity : BaseTestActivity() {
    override fun loadImage(testIv: ImageView) {
       Doodle.load(R.raw.setting).into(testIv)
    }
}

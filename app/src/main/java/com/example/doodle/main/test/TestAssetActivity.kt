package com.example.doodle.main.test

import android.widget.ImageView
import io.github.doodle.Doodle

class TestAssetActivity : BaseTestActivity() {
    companion object {
        private const val ASSET_PREFIX = "file:///android_asset/"
    }

    override fun loadImage(testIv: ImageView) {
        Doodle.load(ASSET_PREFIX + "lenna.jpg").into(testIv)
    }
}

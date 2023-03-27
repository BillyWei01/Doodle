package com.example.doodle.main.test

import android.widget.ImageView
import com.example.doodle.R
import io.github.doodle.Doodle

// Test handle exif orientation
class TestRotatedActivity : BaseTestActivity() {
    override fun loadImage(testIv: ImageView) {
        // Image src https://github.com/recurser/exif-orientation-examples
        // orientation range 0 to 8 (change the number after "Landscape_")
        // val rotatedImageUrl = "https://github.com/recurser/exif-orientation-examples/blob/master/Landscape_8.jpg?raw=true"

        val rotatedImageUrl = "http://7xt44n.com2.z0.glb.qiniucdn.com/exif.png"

        Doodle.load(rotatedImageUrl)
            .placeholder(R.color.black)
            .error(R.color.loading_fail)
            .into(testIv)
    }
}

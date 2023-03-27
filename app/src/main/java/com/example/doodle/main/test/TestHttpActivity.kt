package com.example.doodle.main.test

import android.widget.ImageView
import com.example.doodle.R
import io.github.doodle.Doodle
import io.github.doodle.enums.DiskCacheStrategy
import io.github.doodle.enums.MemoryCacheStrategy

class TestHttpActivity : BaseTestActivity()   {
    override fun loadImage(testIv: ImageView) {
        // Video source from: https://sample-videos.com/index.php#sample-mp4-video
        val videoUrl = "https://sample-videos.com/video123/mp4/360/big_buck_bunny_360p_1mb.mp4"
        val imageUrl = "https://gd-hbimg.huaban.com/dfe6579f1f2f1cdd2d2f6f836c98aabef99b0aeb4071-4niAii_fw658"
        Doodle.load(imageUrl)
            .placeholder(R.color.black)
            .error(R.color.loading_fail)
            .into(testIv)
    }
}

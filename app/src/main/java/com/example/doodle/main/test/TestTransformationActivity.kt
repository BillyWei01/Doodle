package com.example.doodle.main.test

import android.widget.ImageView
import com.example.doodle.R
import com.example.doodle.util.Utils.dp2px
import io.github.doodle.Doodle
import io.github.doodle.transformations.CircleTransformation
import io.github.doodle.transformations.RoundedTransformation

class TestTransformationActivity: BaseTestActivity() {
    override fun loadImage(testIv: ImageView) {
        Doodle.load(R.drawable.ez)
            //.transform(RoundedTransformation(dp2px(5f)))
            .transform(CircleTransformation())
            .into(testIv)
    }
}
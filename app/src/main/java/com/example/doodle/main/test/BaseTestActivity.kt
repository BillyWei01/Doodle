package com.example.doodle.main.test

import android.os.Bundle
import android.widget.ImageView
import com.example.doodle.R
import com.example.doodle.base.BaseActivity

abstract class BaseTestActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_image)
        loadImage(findViewById(R.id.test_iv))
    }

    abstract fun loadImage(testIv: ImageView);
}

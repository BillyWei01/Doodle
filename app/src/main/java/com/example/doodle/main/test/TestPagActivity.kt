package com.example.doodle.main.test

import android.os.Bundle
import com.example.doodle.R
import com.example.doodle.base.BaseActivity
import com.example.doodle.imageloader.CompositePagView
import io.github.doodle.Doodle

class TestPagActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_pag)

        val pagView = findViewById<CompositePagView>(R.id.pag_view)

//        val imageUrl = "https://gd-hbimg.huaban.com/dfe6579f1f2f1cdd2d2f6f836c98aabef99b0aeb4071-4niAii_fw658"
//        val pagUrl = "https://github.com/Tencent/libpag/blob/main/assets/PAG_LOGO.pag?raw=true"

        val path = "file:///android_asset/pag_logo.pag"
        Doodle.load(path).into(pagView)
    }
}

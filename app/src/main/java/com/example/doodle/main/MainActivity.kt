package com.example.doodle.main

import android.os.Bundle
import com.example.doodle.R
import com.example.doodle.album.AlbumTestActivity
import com.example.doodle.base.BaseActivity
import com.example.doodle.main.test.ExampleActivity
import com.example.doodle.remote.RemoteImageActivity
import com.example.doodle.util.onClick
import kotlinx.android.synthetic.main.activity_main.test_album
import kotlinx.android.synthetic.main.activity_main.test_remote
import kotlinx.android.synthetic.main.activity_main.test_single_loading

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        test_single_loading.onClick {
            startActivity(ExampleActivity::class.java)
        }
        test_album.onClick {
            startActivity(AlbumTestActivity::class.java)
        }
        test_remote.onClick {
            startActivity(RemoteImageActivity::class.java)
        }
    }
}

package com.example.doodle.main.test

import android.os.Build
import android.os.Bundle
import android.view.View
import com.example.doodle.R
import com.example.doodle.base.BaseActivity
import com.example.doodle.util.onClick
import kotlinx.android.synthetic.main.activity_example.*

class ExampleActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        test_asset_btn.onClick {
            startActivity(TestAssetActivity::class.java)
        }

        test_gif_btn.onClick {
            startActivity(TestGifActivity::class.java)
        }

        test_svg_btn.onClick {
            startActivity(TestSvgActivity::class.java)
        }

        // ImageDecoder available on Android P.
        // ImageDecoder support decoding animated webp image.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            test_animated_webp_btn.visibility = View.VISIBLE
            test_animated_webp_btn.onClick {
                startActivity(TestAnimatedWebPActivity::class.java)
            }
        }

        test_http_btn.onClick {
            startActivity(TestHttpActivity::class.java)
        }

        test_rotated_btn.onClick {
            startActivity(TestRotatedActivity::class.java)
        }

        test_transformation_btn.onClick {
            startActivity(TestTransformationActivity::class.java)
        }
    }
}

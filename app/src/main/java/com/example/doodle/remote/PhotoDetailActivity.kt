package com.example.doodle.remote

import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.doodle.R
import com.example.doodle.base.ToolbarActivity
import com.example.doodle.config.AppConfig
import com.example.doodle.config.Constants
import com.example.doodle.remote.data.DataBuffer
import com.example.doodle.remote.data.ImageData
import com.example.doodle.remote.media.MediaUtil
import com.example.doodle.remote.setting.SettingActivity
import com.example.doodle.remote.setting.SettingData
import com.example.doodle.util.LogUtil
import com.example.doodle.util.ToastUtil
import com.example.doodle.util.Utils
import com.github.chrisbanes.photoview.PhotoView
import io.github.album.ui.PreviewViewPager
import io.github.doodle.enums.DecodeFormat
import io.github.doodle.enums.DiskCacheStrategy
import io.github.doodle.Doodle
import io.github.doodle.enums.ClipType
import io.github.doodle.enums.MemoryCacheStrategy
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoDetailActivity : ToolbarActivity() {
    companion object {
        private val MAX_MEMORY = Runtime.getRuntime().maxMemory()
        private val largeMemory = MAX_MEMORY > (256 shl 20)
        private val lowMemory = MAX_MEMORY < (128 shl 20)
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var from: String? = null
    private val dataList = ArrayList<ImageData>()

    private var currentPosition: Int = 0

    override val contentLayout: Int
        get() = R.layout.activity_pin_detail

    override fun initView() {
        val list = DataBuffer.dataList
        if (list.isNullOrEmpty()) {
            DataBuffer.dataList = null
            finish()
            return
        }

        hideTitle()

        dataList.clear()
        dataList.addAll(list)
        DataBuffer.dataList = null

        from = intent.getStringExtra(Constants.KEY_PAGE_FROM) ?: return
        currentPosition = intent.getIntExtra(Constants.KEY_POSITION, 0)

        if (currentPosition >= dataList.size) {
            finish()
            return
        }

        val viewPager: PreviewViewPager = findViewById(R.id.preview_view_pager)
        viewPager.adapter = DetailPagerAdapter()
        viewPager.currentItem = currentPosition
        viewPager.offscreenPageLimit = if (largeMemory) 2 else 1
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                currentPosition = position
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
    }

    override fun onDestroy() {
        dataList.clear()
        super.onDestroy()
    }

    private inner class DetailPagerAdapter : PagerAdapter() {
        override fun getCount(): Int {
            return dataList.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val root = LayoutInflater.from(container.context)
                .inflate(R.layout.preview_item_view, container, false)
            val data = dataList[position]
            container.addView(root)
            setViews(root, data)
            return root
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view = `object` as? View
            if (view != null) {
                container.removeView(view)
            }
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        private fun setViews(root: View, data: ImageData) {
            val path = data.path
            val width = data.width
            val height = data.height

            val photoView = root.findViewById<PhotoView>(R.id.photo_view)
            val scaleImageView = root.findViewById<SubsamplingScaleImageView>(R.id.scale_iv)
            val progressBar = root.findViewById<ProgressBar>(R.id.progress_bar)

            val sizeLimit = if (largeMemory) 4096 else 2048
            if (width > sizeLimit || height > sizeLimit) {
                photoView.visibility = View.GONE
                scaleImageView.visibility = View.VISIBLE
                if (path.startsWith("http")) {
                    var file = Doodle.getCacheFile(path)
                    if (file != null) {
                        scaleImageView.setImage(ImageSource.uri(Uri.fromFile(file)))
                    } else {
                        progressBar.visibility = View.VISIBLE
                        scope.launch {
                            file = withContext(Dispatchers.IO) {
                                Doodle.downloadOnly(path)
                            }
                            progressBar.visibility = View.GONE
                            if (file != null) {
                                scaleImageView.setImage(ImageSource.uri(Uri.fromFile(file)))
                            } else {
                                LogUtil.d(mTag, "loading image failed, uri")
                            }
                        }
                    }
                } else {
                    scaleImageView.setImage(ImageSource.uri(Uri.parse(path)))
                }
            } else {
                photoView.visibility = View.VISIBLE
                scaleImageView.visibility = View.GONE
                postProgress(root)
                val decodeFormat = if (lowMemory) DecodeFormat.RGB_565 else DecodeFormat.ARGB_8888
                Doodle.load(path)
                    .decodeFormat(decodeFormat)
                    .clipType(ClipType.NO_CLIP)
                    .memoryCacheStrategy(MemoryCacheStrategy.WEAK)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .fadeIn(500)
                    .error(R.color.loading_fail)
                    .listen {
                        // Mark loading finish
                        root.tag = it
                        // Hide progress
                        if (progressBar.visibility == View.VISIBLE) {
                            progressBar.visibility = View.GONE
                        }
                    }
                    .into(photoView)
            }
        }
    }

    // Show progress if Doodle can't get result in 50ms.
    private fun postProgress(rootView: View) {
        val rootRef = WeakReference(rootView)
        AppConfig.uiHandler.postDelayed(
            {
                val root = rootRef.get()
                val activity = Utils.pickActivity(root)
                if (root != null && activity != null && !(activity.isFinishing || activity.isDestroyed)) {
                    if (root.tag == null) {
                        root.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.VISIBLE
                    }
                }
            },
            50L
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (from == Constants.REMOTE) {
            menuInflater.inflate(R.menu.photo_detail, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_download -> {
                download()
                return true
            }
            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    private fun download() {
        if (TextUtils.isEmpty(SettingData.collectPath)) {
            startActivity(SettingActivity::class.java)
        } else {
            val path = dataList[currentPosition].path
            if (path.isNotEmpty() && path.startsWith("http")) {
                scope.launch {
                    val result = downloadFile(path)
                    ToastUtil.showTips(
                        if (result) getStr(R.string.download_success)
                        else getStr(R.string.download_failed)
                    )
                }
            } else {
                ToastUtil.showTips(getStr(R.string.downloaded))
            }
        }
    }

    private suspend fun downloadFile(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var cacheFile = Doodle.getCacheFile(url)
            if (cacheFile == null) {
                cacheFile = Doodle.downloadOnly(url)
            }
            if (cacheFile != null) {
                MediaUtil.insertImage(cacheFile, SettingData.collectPath)
            } else {
                false
            }
        } catch (t: Throwable) {
            LogUtil.e(mTag, t)
            false
        }
    }
}

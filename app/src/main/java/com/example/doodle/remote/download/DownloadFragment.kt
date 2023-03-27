package com.example.doodle.remote.download

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doodle.event.Event
import com.example.doodle.event.Observer
import com.example.doodle.base.BaseFragment
import com.example.doodle.R
import com.example.doodle.event.EventLifecycleObserver
import com.example.doodle.remote.data.ImageData
import com.example.doodle.remote.media.MediaUtil
import com.example.doodle.remote.setting.SettingData.collectPath
import com.example.doodle.remote.setting.path.PickPathActivity
import com.example.doodle.util.Utils
import io.github.album.ui.GridItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadFragment : BaseFragment(), Observer {
    companion object {
        const val TAG = "DownloadFragment"
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var mAdapter: DownloadAdapter? = null

    override val layoutResource: Int
        get() = R.layout.fragment_download

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        lifecycle.addObserver(EventLifecycleObserver(this))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun initView() {
        val ctx = (activity ?: this.context) ?: return
        val adapter = DownloadAdapter(ctx, emptyList())
        val recyclerView = findViewById(R.id.download_rv) as RecyclerView
        val layoutManage = GridLayoutManager(ctx, 3)
        recyclerView.layoutManager = layoutManage
        recyclerView.addItemDecoration(GridItemDecoration(3, Utils.dp2px(2.5f)));
        recyclerView.adapter = adapter
        mAdapter = adapter

        if (collectPath.isEmpty()) {
            recyclerView.post {
                AlertDialog.Builder(ctx)
                    .setTitle(R.string.select_photo_path)
                    .setPositiveButton(R.string.go_to_setting) { _, _ ->
                        startActivity(Intent(ctx, PickPathActivity::class.java))
                    }.show()
            }
        } else {
            getImages()
        }
    }

    private fun getImages() {
        if (collectPath.isNotEmpty()) {
            scope.launch {
                val pathList = withContext(Dispatchers.IO) {
                    MediaUtil.loadImages(collectPath)
                }
                mAdapter?.update(pathList)
            }
        }
    }

    override fun onEvent(event: Int, vararg args: Any?) {
        when (event) {
            Event.CHOSE_PATH ->{
                getImages()
            }
            Event.DOWNLOAD_SUCCESS -> {
                val imageData = args[0] as ImageData
                mAdapter?.appendMediaData(imageData)
            }
        }
    }

    override fun listenEvents(): IntArray {
        return intArrayOf(
            Event.CHOSE_PATH,
            Event.DOWNLOAD_SUCCESS
        )
    }
}
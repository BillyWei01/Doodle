package com.example.doodle.remote

import android.os.Bundle
import android.text.TextUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doodle.R
import com.example.doodle.base.BaseAdapter.OnLoadMoreListener
import com.example.doodle.base.BaseFragment
import com.example.doodle.config.HttpClient
import com.example.doodle.remote.channel.Channel
import com.example.doodle.remote.data.ImageData
import com.example.doodle.util.LogUtil
import com.example.doodle.util.MHash
import com.example.doodle.util.NetworkUtil
import com.example.doodle.util.ResUtil
import com.example.doodle.util.ToastUtil
import io.github.doodle.Doodle
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import org.json.JSONObject

open class ChannelFragment : BaseFragment() {
    companion object {
        protected const val MODE_REFRESH = 0
        protected const val MODE_LOAD_MORE = 1
        protected const val ARG_CHANNEL = "channel"

        protected fun initFragment(fragment: ChannelFragment, channel: Channel) {
            val arguments = Bundle()
            arguments.putParcelable(ARG_CHANNEL, channel)
            fragment.arguments = arguments
            fragment.init(channel)
        }

        @JvmStatic
        fun newInstance(channel: Channel): ChannelFragment {
            val fragment = ChannelFragment()
            initFragment(fragment, channel)
            return fragment
        }
    }

    protected var mLastPage = 1
    protected var mNextPage = 2
    protected var mLoadingMode = MODE_REFRESH
    private var mChannel: Channel? = null

    var fragmentID: Long = 0
        private set

    private val mIdSet = HashSet<String>()

    val channelID: String
        get() = mChannel?.id ?: ""

    val title: String
        get() = mChannel?.name ?: ""

    private fun init(channel: Channel) {
        mChannel = channel
        name = "ChannelFragment" + "_" + channel.id
        fragmentID = MHash.hash64(name)
    }

    private var mAdapter: RemoteAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val loading = AtomicBoolean()

    override val layoutResource: Int
        get() = R.layout.fragement_channel_page

    override fun initView() {
        if (mChannel == null && arguments != null) {
            requireArguments().getParcelable<Channel>(ARG_CHANNEL)?.let { init(it) }
        }
        val context = activity ?: return
        val adapter = RemoteAdapter(this, context, ArrayList(), true)
        mAdapter = adapter
        adapter.setLoadingFooter(R.layout.footer_loading)
        adapter.setOnLoadMoreListener(object : OnLoadMoreListener {
            override fun onLoadMore(forceReload: Boolean) {
                if (mLastPage == mNextPage && !forceReload) {
                    return
                }
                mLastPage = mNextPage
                mLoadingMode = MODE_LOAD_MORE
                loadData()
            }
        })

        mSwipeRefreshLayout = findViewById(R.id.page_sfl) as SwipeRefreshLayout
        mSwipeRefreshLayout?.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent, R.color.colorPrimaryDark)
        mSwipeRefreshLayout?.setOnRefreshListener {
            mLoadingMode = MODE_REFRESH
            loadData()
        }
        val layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        val recyclerView = findViewById(R.id.page_rv) as RecyclerView
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = mAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Doodle.resumeRequests()
                } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    Doodle.pauseRequests()
                }
            }
        })
    }

    override fun loadData() {
        if (NetworkUtil.isConnected) {
            if (loading.compareAndSet(false, true)) {
                fetchPins()
            }
        } else {
            ToastUtil.showTips(R.string.connect_tips)
        }
    }

    private fun fetchPins() {
        var pinID = ""
        var hasData = true
        if (mLoadingMode == MODE_LOAD_MORE) {
            val lastPin = mAdapter?.lastItem
            if (lastPin != null) {
                pinID = lastPin.id
            }
        }
        scope.launch {
            val pinList = withContext(Dispatchers.IO) {
                kotlin.runCatching {
                    val list = fetchPins(channelID, pinID)
                    hasData = list.isNotEmpty()
                    val iterator = list.iterator()
                    while (iterator.hasNext()) {
                        val id = iterator.next().id
                        if (mIdSet.contains(id)) {
                            iterator.remove()
                        } else {
                            mIdSet.add(id)
                        }
                    }
                    list
                }.onFailure {
                    LogUtil.e(name, it)
                }.getOrNull()
            }
            loading.set(false)

            val adapter = mAdapter ?: return@launch

            if (pinList == null) {
                ToastUtil.showTips(ResUtil.getStr(R.string.get_image_list_failed))
                mSwipeRefreshLayout?.isRefreshing = false
            }
            if (mLoadingMode == MODE_LOAD_MORE) {
                if (pinList == null) {
                    adapter.setFailedFooter(R.layout.footer_failed)
                } else if (!hasData) {
                    adapter.setEndFooter(R.layout.footer_end)
                } else {
                    mNextPage++
                    adapter.appendData(pinList)
                }
            } else {
                if (!pinList.isNullOrEmpty()) {
                    if (adapter.dataSize == 0) {
                        adapter.data = pinList
                    } else {
                        adapter.insertFront(pinList)
                    }
                }
                mSwipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun fetchPins(channelId: String, lastPinId: String): ArrayList<ImageData> {
        val pinList: ArrayList<ImageData> = ArrayList()
        if (channelId.isNotEmpty()) {
            var url = "https://api.huaban.com/favorite/$channelId?limit=30"
            if (!TextUtils.isEmpty(lastPinId)) {
                url += "&max=$lastPinId"
            }
            val content = HttpClient.request(url)
            if (content.isEmpty()) {
                return pinList
            }
            val jsonObject = JSONObject(content)
            val pinArray = jsonObject.getJSONArray("pins")
            val n = pinArray.length()
            for (i in 0 until n) {
                val pinObject = pinArray.getJSONObject(i)
                val pinId = pinObject.getString("pin_id")
                val fileObject = pinObject.getJSONObject("file")
                val width = fileObject.getInt("width")
                val height = fileObject.getInt("height")
                val key = fileObject.getString("key")
                pinList.add(ImageData("http://img.hb.aicdn.com/$key", width, height, pinId))
            }
        }
        return pinList
    }

    override fun onDestroy() {
        super.onDestroy()
        kotlin.runCatching {
            scope.cancel()
        }
    }
}


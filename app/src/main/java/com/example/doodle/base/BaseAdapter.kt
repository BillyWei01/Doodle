package com.example.doodle.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * This class was written several years ago.
 * When I look back, I think it's a bad idea to make a 'BaseAdapter' , but I don't have time to reconstruct this.
 * Just ignore this bad pattern.
 */
abstract class BaseAdapter<T, VH : RecyclerView.ViewHolder>(protected val mContext: Context, data: List<T>, loadMoreFlag: Boolean)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface OnLoadMoreListener {
        fun onLoadMore(forceReload: Boolean)
    }

    private class DefaultHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var mLoadMoreListener: OnLoadMoreListener? = null
    @JvmField
    protected val mData: MutableList<T>
    private val mLoadMoreFlag: Boolean
    private var mFooterLayout: FrameLayout? = null
    private var mLoadingFooter: View? = null
    private var mEndFooter: View? = null
    private var mEmptyView: View? = null

    val dataSize: Int
        get() = mData.size

    fun setOnLoadMoreListener(loadMoreListener: OnLoadMoreListener) {
        mLoadMoreListener = loadMoreListener
    }

    protected abstract fun getItemHolder(parent: ViewGroup): VH
    protected abstract fun bindHolder(item: T, position: Int, holder: VH)

    override fun getItemCount(): Int {
        return if (mData.isEmpty()) {
            if (mEmptyView == null) 0 else 1
        } else {
            mData.size + if (mLoadMoreFlag) 1 else 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (mData.isEmpty()) {
            return if (mEmptyView == null) TYPE_UNKNOWN else TYPE_EMPTY
        }
        return if (isFooter(position)) {
            TYPE_FOOTER
        } else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_EMPTY -> DefaultHolder(mEmptyView!!)
            TYPE_FOOTER -> {
                if (mFooterLayout == null) {
                    mFooterLayout = FrameLayout(mContext)
                }
                DefaultHolder(mFooterLayout!!)
            }
            TYPE_UNKNOWN -> DefaultHolder(View(mContext))
            else -> getItemHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_ITEM) {
            bindHolder(mData[position], position, holder as VH)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (isFooter(holder.layoutPosition)) {
            val lp = holder.itemView.layoutParams
            if (lp is StaggeredGridLayoutManager.LayoutParams) {
                lp.isFullSpan = true
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {
            layoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (isFooter(position)) {
                        layoutManager.spanCount
                    } else 1
                }
            }
        }
        if (mLoadMoreFlag && mLoadMoreListener != null) {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (mFooterLayout!!.getChildAt(0) !== mEndFooter
                            && findLastVisibleItem(layoutManager) + 1 == itemCount) {
                            mLoadMoreListener!!.onLoadMore(false)
                        }
                    }
                }
            })
        }
    }

    private fun isFooter(position: Int): Boolean {
        return mLoadMoreFlag && itemCount > 1 && position >= itemCount - 1
    }

    private fun findLastVisibleItem(layoutManager: RecyclerView.LayoutManager?): Int {
        if (layoutManager is LinearLayoutManager) {
            return layoutManager.findLastVisibleItemPosition()
        } else if (layoutManager is StaggeredGridLayoutManager) {
            val positions = layoutManager.findLastVisibleItemPositions(null)
            var max = -1
            for (position in positions) {
                if (position > max) {
                    max = position
                }
            }
            return max
        }
        return -1
    }

    open var data: List<T>
        get() = mData
        set(data) {
            mData.clear()
            mData.addAll(data)
            notifyDataSetChanged()
        }

    fun appendData(data: List<T>) {
        if(data.isNotEmpty()){
            val position = mData.size
            mData.addAll(data)
            notifyItemRangeInserted(position, data.size)
        }
    }

    fun insertFront(data: List<T>) {
        if(data.isNotEmpty()){
            mData.addAll(0, data)
            notifyItemRangeInserted(0, data.size)
        }
    }

    val lastItem: T?
        get() = if (mData.isNotEmpty()) {
            mData[mData.size - 1]
        } else null

    fun setEmptyView(@LayoutRes emptyId: Int) {
        mEmptyView = inflate(emptyId)
    }

    fun setLoadingFooter(@LayoutRes loadingId: Int) {
        mLoadingFooter = inflate(loadingId)
        replaceFooter(mLoadingFooter)
    }

    fun setEndFooter(@LayoutRes endID: Int) {
        mEndFooter = inflate(endID)
        replaceFooter(mEndFooter)
    }

    fun setFailedFooter(@LayoutRes failedId: Int) {
        val failedView = inflate(failedId)
        failedView.setOnClickListener {
            replaceFooter(mLoadingFooter)
            mLoadMoreListener!!.onLoadMore(true)
        }
        replaceFooter(failedView)
    }

    private fun replaceFooter(footer: View?) {
        if (footer == null) {
            return
        }
        if (mFooterLayout == null) {
            mFooterLayout = FrameLayout(mContext)
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        mFooterLayout!!.removeAllViews()
        mFooterLayout!!.addView(footer, params)
    }

    protected fun inflate(@LayoutRes layoutId: Int): View {
       return LayoutInflater.from(mContext).inflate(layoutId, null)
    }

    protected fun inflate(@LayoutRes layoutId: Int, parent: ViewGroup?): View {
        return LayoutInflater.from(mContext).inflate(layoutId, parent, false)
    }

    companion object {
        private const val TYPE_ITEM = 1
        private const val TYPE_FOOTER = 2
        private const val TYPE_EMPTY = 4
        private const val TYPE_UNKNOWN = 3
    }

    init {
        mData = ArrayList(data)
        mLoadMoreFlag = loadMoreFlag
    }
}

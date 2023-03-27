package com.example.doodle.remote

import android.content.Context
import com.example.doodle.remote.RemoteAdapter.ItemHolder
import android.view.ViewGroup
import com.example.doodle.R
import androidx.recyclerview.widget.RecyclerView
import com.example.doodle.remote.widget.FlowImageView
import android.content.Intent
import android.view.View
import com.example.doodle.base.BaseAdapter
import com.example.doodle.config.Constants
import com.example.doodle.remote.data.DataBuffer
import com.example.doodle.remote.data.ImageData
import com.example.doodle.util.onClick
import io.github.doodle.Doodle
import kotlin.math.roundToInt

class RemoteAdapter(private val host: Any, context: Context, data: List<ImageData>, isLoadMoreOpen: Boolean)
    : BaseAdapter<ImageData, ItemHolder>(context, data, isLoadMoreOpen) {
    override fun getItemHolder(parent: ViewGroup): ItemHolder {
        return ItemHolder(inflate(R.layout.item_flow, parent))
    }

    override fun bindHolder(item: ImageData, position: Int, holder: ItemHolder) {
        holder.flowIv.setSourceSize(item.width, item.height)
        holder.flowIv.requestLayout()
        val desWidth: Int = if (holder.flowIv.width > 0) {
            holder.flowIv.width
        } else {
            val resources = mContext.resources
            val margin = resources.getDimensionPixelSize(R.dimen.flow_item_margin)
            val width = resources.displayMetrics.widthPixels
            (width - 6 * margin) / 3
        }
        val rate = item.height.toFloat() / item.width.toFloat()
        val desHeight = if (desWidth > 0) (desWidth * rate).roundToInt() else 0
        Doodle.load(item.path)
            .override(desWidth, desHeight)
            .error(R.color.loading_fail)
            .observeHost(host)
            .into(holder.flowIv)
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flowIv: FlowImageView
        private fun toPinDetail(position: Int) {
            if (position >= 0 && position < mData.size) {
                DataBuffer.dataList = mData
                val intent = Intent(mContext, PhotoDetailActivity::class.java)
                intent.putExtra(Constants.KEY_PAGE_FROM, Constants.REMOTE)
                intent.putExtra(Constants.KEY_POSITION, position)
                mContext.startActivity(intent)
            }
        }

        init {
            itemView.onClick { toPinDetail(adapterPosition) }
            flowIv = itemView.findViewById(R.id.flow_iv)
        }
    }
}
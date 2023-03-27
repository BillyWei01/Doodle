package com.example.doodle.remote.download

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.doodle.R
import com.example.doodle.base.BaseAdapter
import com.example.doodle.config.Constants
import com.example.doodle.remote.PhotoDetailActivity
import com.example.doodle.remote.data.DataBuffer
import com.example.doodle.remote.data.ImageData
import com.example.doodle.remote.download.DownloadAdapter.DownloadHolder
import io.github.doodle.Doodle

class DownloadAdapter(context: Context, data: List<ImageData>) :
    BaseAdapter<ImageData, DownloadHolder>(context, data, false) {
    override fun getItemHolder(parent: ViewGroup): DownloadHolder {
        return DownloadHolder(inflate(R.layout.item_download, parent))
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(list: List<ImageData>) {
        mData.clear()
        mData.addAll(list)
        notifyDataSetChanged()
    }

    fun appendMediaData(imageData: ImageData) {
        if (mData.find { it.path == imageData.path } == null) {
            mData.add(imageData)
            notifyItemInserted(itemCount)
        }
    }

    inner class DownloadHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val itemIv: ImageView

        private fun toPinDetail(position: Int) {
            if (position >= 0 && position < mData.size) {
                DataBuffer.dataList = mData
                val intent = Intent(mContext, PhotoDetailActivity::class.java)
                intent.putExtra(Constants.KEY_PAGE_FROM, Constants.LOCAL)
                intent.putExtra(Constants.KEY_POSITION, position)
                mContext.startActivity(intent)
            }
        }

        init {
            itemView.setOnClickListener { toPinDetail(adapterPosition) }
            itemIv = itemView.findViewById(R.id.item_iv)
        }
    }

    override fun bindHolder(item: ImageData, position: Int, holder: DownloadHolder) {
        Doodle.load(item.path).into(holder.itemIv)
    }
}

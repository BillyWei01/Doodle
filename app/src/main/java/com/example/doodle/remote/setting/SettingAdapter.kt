package com.example.doodle.remote.setting

import android.content.Context
import com.example.doodle.remote.setting.SettingAdapter.SettingHolder
import android.view.ViewGroup
import com.example.doodle.R
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.example.doodle.base.BaseAdapter

class SettingAdapter internal constructor(context: Context,
                                          data: List<SettingItem>,
                                          loadMoreFlag: Boolean,
                                          private val mListener: OnItemClickListener)
    : BaseAdapter<SettingItem, SettingHolder>(context, data, loadMoreFlag) {
    internal interface OnItemClickListener {
        fun onItemClick(id: Int)
    }

    override fun getItemHolder(parent: ViewGroup): SettingHolder {
        return SettingHolder(inflate(R.layout.item_setting, parent))
    }

    override fun bindHolder(item: SettingItem, position: Int, holder: SettingHolder) {
        holder.titleTv.text = item.title
        if (!TextUtils.isEmpty(item.subtitle)) {
            holder.subtitleTv.text = item.subtitle
            holder.subtitleTv.visibility = View.VISIBLE
        } else {
            holder.subtitleTv.visibility = View.GONE
        }
    }

    inner class SettingHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv: TextView
        val subtitleTv: TextView

        init {
            titleTv = itemView.findViewById(R.id.title_tv)
            subtitleTv = itemView.findViewById(R.id.subtitle_tv)
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position >= 0 && position < mData.size) {
                    mListener.onItemClick(mData[position].id)
                }
            }
        }
    }
}
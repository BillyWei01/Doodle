package com.example.doodle.remote.setting.path

import android.content.Context
import android.view.View
import com.example.doodle.remote.setting.path.PathAdapter.FolderHolder
import android.view.ViewGroup
import com.example.doodle.R
import com.example.doodle.util.ResUtil
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.CheckBox
import android.widget.CompoundButton
import com.example.doodle.base.BaseAdapter
import java.io.File

class PathAdapter internal constructor(
    context: Context,
    data: List<File>,
    loadMoreFlag: Boolean,
    private val mPathFiler: PathFilter)
    : BaseAdapter<File, FolderHolder>(context, data, loadMoreFlag) {
    interface OnItemClickListener {
        fun onItemClick(file: File)
    }

    private var mOnItemClickListener: OnItemClickListener? = null
    private var mCheckPosition = -1
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        mOnItemClickListener = listener
    }

    val checkFile: File?
        get() = if (mCheckPosition == -1) null else mData[mCheckPosition]
    override var data: List<File>
        get() = super.data
        set(data) {
            super.data = data
            mCheckPosition = -1
        }

    override fun getItemHolder(parent: ViewGroup): FolderHolder {
        return FolderHolder(inflate(R.layout.item_path, parent))
    }

    override fun bindHolder(item: File, position: Int, holder: FolderHolder) {
        holder.nameTv.text = item.name
        val files = item.listFiles(mPathFiler)
        val fileCount = files?.size ?: 0
        holder.detailTv.text = ResUtil.getStr(R.string.item, fileCount)
        holder.pathCb.isChecked = mCheckPosition == position
    }

    inner class FolderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTv: TextView
        val detailTv: TextView
        val pathCb: CheckBox

        init {
            nameTv = itemView.findViewById(R.id.name_tv)
            detailTv = itemView.findViewById(R.id.detail_tv)
            pathCb = itemView.findViewById(R.id.path_cb)
            pathCb.setOnCheckedChangeListener { compoundButton: CompoundButton?, isChecked: Boolean ->
                val position = adapterPosition
                if (isChecked) {
                    val lastPos = mCheckPosition
                    mCheckPosition = position
                    if (lastPos >= 0 && lastPos != position) {
                        notifyItemChanged(lastPos)
                    }
                } else if (mCheckPosition == position) {
                    mCheckPosition = -1
                }
            }
            itemView.setOnClickListener { v: View? ->
                val position = adapterPosition
                mOnItemClickListener?.onItemClick(mData[position])
            }
        }
    }
}

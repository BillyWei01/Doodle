package com.example.doodle.remote.channel

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.doodle.R
import com.example.doodle.remote.channel.ChannelAdapter.FinishEditListener

class ChannelDialog(context: Context,
                    private val mMyChannels: MutableList<Channel>,
                    private val mOtherChannel: MutableList<Channel>)
    : Dialog(context, R.style.ChannelDialog), FinishEditListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_channel)
        val window = window
        window?.setWindowAnimations(R.style.SlideInOutAnim)
        val channelRv = findViewById<RecyclerView>(R.id.channel_rv)
        val manager = GridLayoutManager(context, 4)
        channelRv.layoutManager = manager
        val callback = ItemDragHelperCallback()
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(channelRv)
        val adapter = ChannelAdapter(context, helper, mMyChannels, mOtherChannel, this)
        manager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = adapter.getItemViewType(position)
                return if (viewType == ChannelAdapter.Companion.TYPE_MY
                    || viewType == ChannelAdapter.Companion.TYPE_OTHER) 1 else 4
            }
        }
        channelRv.adapter = adapter
    }

    override fun onFinishEdit() {
        dismiss()
    }
}
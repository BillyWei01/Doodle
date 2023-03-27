package com.example.doodle.remote.channel

import android.text.TextUtils
import com.example.doodle.R
import com.example.doodle.base.BaseActivity.Companion.getStr
import com.example.doodle.remote.setting.SettingData
import java.lang.StringBuilder
import java.util.ArrayList

object ChannelManager {
    var myChannels: MutableList<Channel>
    var otherChannels: MutableList<Channel>

    init {
        val str = SettingData.channels
        if (TextUtils.isEmpty(str)) {
            val channels = getDefaultChannels()
            myChannels = channels[0]
            otherChannels = channels[1]
            saveChannels()
        } else {
            val result = str.split('&').toTypedArray()
            myChannels = decodeChannels(result[0])
            otherChannels = if (result.size > 1) {
                decodeChannels(result[1])
            } else {
                ArrayList()
            }
        }
    }

    fun saveChannels() {
        val builder = StringBuilder()
        encodeChannels(myChannels, builder)
        builder.setCharAt(builder.length - 1, '&')
        encodeChannels(otherChannels, builder)
        builder.setLength(builder.length - 1)
        val channels = builder.toString()
        SettingData.channels = channels
    }

    private fun encodeChannels(channels: List<Channel>, builder: StringBuilder) {
        for (channel in channels) {
            builder.append(channel.id).append(':').append(channel.name).append(',')
        }
    }

    private fun decodeChannels(result: String): MutableList<Channel> {
        val channels: MutableList<Channel> = ArrayList()
        if (TextUtils.isEmpty(result)) {
            return channels
        }
        val cs = result.split(",".toRegex()).toTypedArray()
        for (c in cs) {
            val index = c.indexOf(':')
            val id = c.substring(0, index)
            val name = c.substring(index + 1)
            channels.add(Channel(id, name))
        }
        return channels
    }

    private fun getDefaultChannels(): List<MutableList<Channel>> {
        val myChannels: MutableList<Channel> = ArrayList()
        val otherChannels: MutableList<Channel> = ArrayList()
        myChannels.add(Channel("anime", getStr(R.string.channel_anime)))
        myChannels.add(Channel("travel_places", getStr(R.string.channel_travel_places)))
        myChannels.add(Channel("pets", getStr(R.string.channel_pets)))
        otherChannels.add(Channel("photography", getStr(R.string.channel_photography)))
        otherChannels.add(Channel("apparel", getStr(R.string.channel_apparel)))
        val channels: MutableList<MutableList<Channel>> = ArrayList(2)
        channels.add(myChannels)
        channels.add(otherChannels)
        return channels
    }
}

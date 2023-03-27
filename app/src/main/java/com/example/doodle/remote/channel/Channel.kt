package com.example.doodle.remote.channel

import android.os.Parcel
import android.os.Parcelable

open class Channel : Parcelable {
    val id: String
    val name: String

    constructor(id: String, name: String) {
        this.id = id
        this.name = name
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(name)
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readString()!!
        name = `in`.readString()!!
    }

    companion object CREATOR : Parcelable.Creator<Channel> {
        override fun createFromParcel(parcel: Parcel): Channel {
            return Channel(parcel)
        }

        override fun newArray(size: Int): Array<Channel?> {
            return arrayOfNulls(size)
        }
    }
}
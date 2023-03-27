package com.example.doodle.imageloader

import com.example.doodle.config.HttpClient
import io.github.doodle.interfaces.HttpSourceFetcher
import okhttp3.CacheControl
import okhttp3.Request
import java.io.IOException
import java.io.InputStream

object OkHttpSourceFetcher : HttpSourceFetcher {
    override fun getInputStream(url: String): InputStream {
        val request: Request = Request.Builder().url(url)
            .addHeader("Connection", "close")
            .cacheControl(CacheControl.Builder().noStore().noCache().build())
            .build()
        val response = HttpClient.client.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body() ?: throw IOException("Request failed.")
            return body.byteStream()
        } else {
            throw IOException("Request failed, code:" + response.code())
        }
    }
}

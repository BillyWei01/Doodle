package com.example.doodle.config

import com.example.doodle.util.IOUtil
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import okhttp3.Dispatcher

object HttpClient {
    internal val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(Cache(File(AppConfig.appContext.cacheDir.absolutePath, "http"), (64L shl 20)))
            .dispatcher(Dispatcher().apply {
                maxRequestsPerHost = 8
            })
            .build()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun request(url: String): String {
        val request: Request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                return try {
                    body.string()
                } finally {
                    IOUtil.closeQuietly(body)
                }
            }
        }
        throw IOException("Request failed, status code:" + response.code())
    }
}

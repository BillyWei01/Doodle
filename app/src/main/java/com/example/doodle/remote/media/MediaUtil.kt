package com.example.doodle.remote.media

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import com.example.doodle.config.AppConfig
import com.example.doodle.event.Event
import com.example.doodle.event.EventManager.notify
import com.example.doodle.remote.data.ImageData
import com.example.doodle.util.IOUtil
import com.example.doodle.util.LogUtil
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

object MediaUtil {
    private const val TAG = "MediaUtil"
    private const val APP_FOLDER_NAME = "Doodle"

    private val externalStoragePath: String by lazy {
        val path = Environment.getExternalStorageDirectory()?.absolutePath
        if (!path.isNullOrEmpty()) {
            path
        } else {
            AppConfig.appContext.getExternalFilesDir(null)?.path?.let {
                it.substring(0, it.indexOf("Android") - 1)
            } ?: "/storage/emulated/0"
        }
    }

    val picturePath: String by lazy {
        "$externalStoragePath/${Environment.DIRECTORY_PICTURES}/$APP_FOLDER_NAME/"
    }

    fun loadImages(dir: String): List<ImageData> {
        if (dir.isEmpty()) return emptyList()

        val contentResolver = AppConfig.appContext.contentResolver
        val baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor = contentResolver.query(
            baseUri,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT
            ),
            "${MediaStore.MediaColumns.DATA} like '$dir%'",
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} desc")

        val imageList = mutableListOf<ImageData>()
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    var width = cursor.getInt(1)
                    var height = cursor.getInt(2)
                    val uri = ContentUris.withAppendedId(baseUri, id)
                    if (width <= 0 || height <= 0) {
                        getImageSize(uri)?.let {
                            width = it.width
                            height = it.height
                        }
                    }
                    imageList.add(ImageData(uri.toString(), width, height))
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, e)
            } finally {
                cursor.close()
            }
        }
        LogUtil.d(TAG, "dir: $dir  ${File(dir).exists()}")
//        imageList.forEach{
//            LogUtil.d(TAG, "dir: ${it.path}  ${File(it.path).exists()}")
//        }
        return imageList
    }

    fun insertImage(file: File, desPath: String): Boolean {
        if (desPath.isEmpty() || !desPath.startsWith(externalStoragePath)) {
            LogUtil.e(TAG, "error desPath:$desPath")
            return false
        }
        val mediaType = getMediaType(file)
        if (mediaType == MediaType.UNKNOWN) {
            return false
        }
        val size = getImageSize(file.path)
        val cacheName = file.name
        val name = if (cacheName.length >= 16) cacheName.substring(0, 16) else cacheName
        val displayName = name + '.' + mediaType.extension
        val fullPath = if (desPath.endsWith('/')) desPath + displayName else "$desPath/$displayName"

        val values = ContentValues()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = desPath.substring(externalStoragePath.length + 1)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        } else {
            kotlin.runCatching {
                File(desPath).run {
                    if (!exists()) {
                        mkdir()
                    }
                }
            }
            values.put(MediaStore.MediaColumns.DATA, fullPath)
        }
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        values.put(MediaStore.Images.Media.MIME_TYPE, mediaType.mime)
        if (size != null) {
            values.put(MediaStore.MediaColumns.WIDTH, size.width)
            values.put(MediaStore.MediaColumns.HEIGHT, size.height)
        }
        // TimeUnit of DATE_ADDED is seconds
        val time = System.currentTimeMillis() / 1000L
        values.put(MediaStore.Images.Media.DATE_ADDED, time)
        values.put(MediaStore.Images.Media.DATE_MODIFIED, time)

        val contentResolver = AppConfig.appContext.contentResolver ?: return false
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            val outStream = contentResolver.openOutputStream(uri) ?: return false
            file.inputStream().use { input ->
                outStream.use { output ->
                    input.copyTo(output)
                }
            }
            val imageData = ImageData(uri.toString(), size?.width ?: 0, size?.height ?: 0)
            notify(Event.DOWNLOAD_SUCCESS, imageData)
            return true
        }
        return false
    }

    private fun getImageSize(uri: Uri): Size? {
        var inputStream: InputStream? = null
        try {
            inputStream = AppConfig.appContext.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(inputStream, null, options)
                return Size(options.outWidth, options.outHeight)
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        } finally {
            IOUtil.closeQuietly(inputStream)
        }
        return null
    }

    private fun getImageSize(filePath: String): Size? {
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, options)
            return Size(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
        return null
    }

    private fun getMediaType(file: File): MediaType {
        var accessFile: RandomAccessFile? = null
        try {
            accessFile = RandomAccessFile(file, "r")
            val bytes = ByteArray(12)
            accessFile.readFully(bytes)
            return MediaParser.parse(bytes)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        } finally {
            IOUtil.closeQuietly(accessFile)
        }
        return MediaType.UNKNOWN
    }
}

package com.example.doodle.remote.media

enum class MediaType(val mime: String, val extension: String) {
    UNKNOWN("", ""),
    MP4("video/mp4", "mp4"),
    MOV("video/quicktime", "mov"),
    _3GP("video/3gpp", "3gp"),
    MKV("video/x-matroska", "mkv"),
    GIF("image/gif", "gif"),
    JPG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    BMP("image/bmp", "bmp"),
    HEIC("image/heic", "heic"),
    HEIF("image/heif", "heif");
}
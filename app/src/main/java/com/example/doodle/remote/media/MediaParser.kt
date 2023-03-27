package com.example.doodle.remote.media

object MediaParser {
    private const val JPG_HEADER = 0xFFD8FF
    private const val GIF_HEADER = 0x474946
    private const val PNG_HEADER = -0x76afb1b9
    private const val BMP_HEADER = 0x424D

    // webp
    private const val RIFF = 0x52494646
    private const val WEBP = 0x57454250

    // mkv
    private const val EBML = 0x1A45DFA3

    // https://www.ftyps.com/
    private const val FILE_TYPE = 0x66747970 // ftyp

    // video/mp4
    private const val ISO = 0x69736F // isox, to isom, iso2,
    private const val MP4 = 0x6D7034 // mp4x, to mp41, mp42
    private const val ND = 0x4E44 // NDxx

    // video/quicktime (Apple QuickTime, .MOV)
    private const val QT = 0x7174

    // video/3gpp2, video/3gpp
    private const val _3G = 0x3367

    // image/heic, image/heif
    private const val HE = 0x6865 // heic, heis, heix, hevc, hevx
    private const val HXF1 = 0x6D006631 // mif1, msf1

    /**
     * Parse MediaType
     *
     * @param header first 12 bytes of file.
     * @return MediaType
     */
    fun parse(header: ByteArray): MediaType {
        val h0 = readInt(header, 0)
        if (h0 ushr 8 == JPG_HEADER) {
            return MediaType.JPG
        }
        if (h0 == PNG_HEADER) {
            return MediaType.PNG
        }
        if (h0 ushr 8 == GIF_HEADER) {
            return MediaType.GIF
        }
        if (h0 ushr 16 == BMP_HEADER) {
            return MediaType.BMP
        }
        if (h0 == EBML) {
            return MediaType.MKV
        }
        val h2 = readInt(header, 2)
        if (h0 == RIFF && h2 == WEBP) {
            return MediaType.WEBP
        }
        val h1 = readInt(header, 1)
        if (h1 == FILE_TYPE) {
            val a = h2 ushr 8
            val b = h2 ushr 16
            if (a == ISO || a == MP4 || b == ND) {
                return MediaType.MP4
            }
            if (b == QT) {
                return MediaType.MOV
            }
            if (b == _3G) {
                return MediaType._3GP
            }
            if (b == HE) {
                return MediaType.HEIC
            }
            if (h2 and -0xff0001 == HXF1) {
                return MediaType.HEIF
            }
        }
        return MediaType.UNKNOWN
    }

    private fun readInt(header: ByteArray, i: Int): Int {
        val j = i shl 2
        return header[j].toInt() shl 24 or
            (header[j + 1].toInt() and 0xFF shl 16) or
            (header[j + 2].toInt() and 0xFF shl 8) or
            (header[j + 3].toInt() and 0xFF)
    }
}
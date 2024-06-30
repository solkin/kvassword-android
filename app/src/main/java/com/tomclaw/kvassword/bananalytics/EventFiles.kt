package com.tomclaw.kvassword.bananalytics

import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Comparator

fun generateEventFileName(event: String, time: Long): String {
    return time.toString() + "-" + md5(event) + ".event"
}

fun getFileNameTime(fileName: String): Long {
    val timeDivider = fileName.indexOf('-')
    return if (timeDivider > 0) {
        fileName.substring(0, timeDivider).toLong()
    } else 0
}

private fun md5(s: String): String {
    try {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(s.toByteArray())
        val messageDigest = digest.digest()
        val hexString = StringBuilder()
        for (aMessageDigest in messageDigest) {
            val h = StringBuilder(Integer.toHexString(0xFF and aMessageDigest.toInt()))
            while (h.length < 2) {
                h.insert(0, "0")
            }
            hexString.append(h)
        }
        return hexString.toString()
    } catch (ignored: NoSuchAlgorithmException) {
    }
    return ""
}

class EventFileComparator : Comparator<File> {

    override fun compare(o1: File, o2: File): Int {
        return compare(getFileNameTime(o1.name), getFileNameTime(o2.name))
    }

    private fun compare(x: Long, y: Long): Int {
        return if (x < y) -1 else if (x == y) 0 else 1
    }

}

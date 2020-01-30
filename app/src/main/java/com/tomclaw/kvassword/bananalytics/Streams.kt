package com.tomclaw.kvassword.bananalytics

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream

@Throws(IOException::class)
fun InputStream.streamToString(): String {
    return String(streamToArray())
}

@Throws(IOException::class)
fun InputStream.streamToArray(): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var read: Int
    while (read(buffer).also { read = it } != -1) {
        byteArrayOutputStream.write(buffer, 0, read)
    }
    return byteArrayOutputStream.toByteArray()
}

fun DataOutputStream.writeNullableUTF(str: String?) {
    writeBoolean(str != null)
    str?.let { writeUTF(it) }
}

fun DataInputStream.readNullableUTF(): String? {
    val isNotNull = readBoolean()
    if (isNotNull) {
        return readUTF()
    }
    return null
}

fun Closeable?.safeClose() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}

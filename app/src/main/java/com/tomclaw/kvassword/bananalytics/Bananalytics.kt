package com.tomclaw.kvassword.bananalytics

import android.util.Log
import com.google.gson.Gson
import com.tomclaw.kvassword.bananalytics.dto.AnalyticsEvent
import com.tomclaw.kvassword.bananalytics.dto.EventsBatch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Bananalytics(
        private val filesDir: File,
        private val infoProvider: InfoProvider,
        private val gson: Gson
) {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    @Suppress("unused")
    fun trackEvent(event: String, payload: String? = null, isImmediate: Boolean = false) {
        executor.execute {
            val file = writeEvent(createEvent(event, payload))
            if (isImmediate) {
                sendEventImmediate(file)
            } else {
                flushEventsSync()
            }
        }
    }

    @Suppress("unused")
    fun flushEvents() {
        executor.execute {
            flushEventsSync()
        }
    }

    private fun flushEventsSync() {
        val files = eventsFiles().toMutableList()
        sendEvents(files)
    }

    private fun writeEvent(event: AnalyticsEvent): File {
        val time = System.currentTimeMillis()
        var output: DataOutputStream? = null
        val file = File(eventsDir(), generateEventFileName(event.event, time))
        try {
            val version: Byte = 1
            output = DataOutputStream(FileOutputStream(file))
            output.run {
                writeByte(version.toInt())
                writeLong(event.time)
                writeUTF(event.event)
                writeNullableUTF(event.payload)
                flush()
            }
        } catch (e: IOException) {
            file.delete()
        } finally {
            output.safeClose()
        }
        return file
    }

    private fun readEvent(file: File): AnalyticsEvent? {
        var input: DataInputStream? = null
        try {
            input = DataInputStream(FileInputStream(file))
            input.run {
                val version = readByte()
                if (version.toInt() == 1) {
                    val time = readLong()
                    val event = readUTF()
                    val payload = readNullableUTF()
                    return createEvent(event, payload, time)
                }
            }
        } catch (ignored: IOException) {
        } finally {
            input.safeClose()
        }
        return null
    }

    private fun sendEventImmediate(file: File) {
        sendEvents(mutableListOf(file), 1)
    }

    private fun sendEvents(files: MutableList<File>, batchSize: Int = BATCH_SIZE) {
        if (files.size >= batchSize) {
            val info = infoProvider.createInfo()
            Collections.sort(files, EventFileComparator())
            val sendEvents: MutableList<AnalyticsEvent> = ArrayList()
            val filesToRemove: MutableList<File> = ArrayList()
            do {
                val file: File = files.removeAt(0)
                val event = readEvent(file)
                if (event != null) {
                    sendEvents.add(event)
                }
                filesToRemove.add(file)
                if (sendEvents.size >= batchSize) {
                    val batch = EventsBatch(info, sendEvents)
                    val data = gson.toJson(batch)
                    log("batch data: $data")
                    try {
                        val result = executePost(API_URL, data)
                        log("batch result: $result")
                    } catch (ex: IOException) {
                        log("error sending analytics track")
                        return
                    }
                    for (f in filesToRemove) {
                        f.delete()
                        log("remove event file: " + f.name)
                    }
                    sendEvents.clear()
                    filesToRemove.clear()
                }
            } while (files.size + filesToRemove.size >= batchSize)
        }
    }

    private fun eventsDir(): File {
        val dir = File(filesDir, "bananalytics")
        dir.mkdirs()
        return dir
    }

    private fun eventsFiles(): List<File> {
        val dir = eventsDir()
        return dir.listFiles()?.asList() ?: emptyList()
    }

    private fun createEvent(
            event: String,
            payload: String?,
            time: Long = System.currentTimeMillis()
    ) = AnalyticsEvent(event, payload, TimeUnit.MILLISECONDS.toSeconds(time))


    @Throws(IOException::class)
    fun executePost(urlString: String, data: String): String {
        var responseStream: InputStream? = null
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_CONNECTION
                readTimeout = TIMEOUT_SOCKET
                requestMethod = "POST"
                doOutput = true
                doInput = true
            }
            connection.outputStream.run {
                write(data.toByteArray())
                flush()
            }
            connection.connect()
            getResponse(connection).streamToString()
        } catch (ex: IOException) {
            throw IOException(ex)
        } finally {
            try {
                responseStream.safeClose()
                connection?.disconnect()
            } catch (ignored: IOException) {
            }
        }
    }

    @Throws(IOException::class)
    private fun getResponse(connection: HttpURLConnection): InputStream {
        val responseCode = connection.responseCode
        return if (responseCode >= 400) {
            connection.errorStream
        } else {
            connection.inputStream
        }
    }

    private fun log(s: String) {
        Log.d("[bananalytics]", s)
    }

}

private const val API_URL = "https://zibuhoker.ru/api/track.php"
private const val BATCH_SIZE = 20

private const val TIMEOUT_SOCKET = 70 * 1000
private const val TIMEOUT_CONNECTION = 60 * 1000

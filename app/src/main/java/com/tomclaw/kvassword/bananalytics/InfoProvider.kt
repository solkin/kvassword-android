package com.tomclaw.kvassword.bananalytics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tomclaw.kvassword.bananalytics.dto.EnvironmentInfo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class InfoProvider(private val context: Context) {

    private var uniqueId: String? = null

    fun createInfo(): EnvironmentInfo {
        val packageName = context.packageName
        var versionCode = 0
        val manager = context.packageManager
        try {
            val info = manager.getPackageInfo(packageName, 0)
            versionCode = info.versionCode
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        val deviceName = Build.MANUFACTURER + " " + Build.MODEL
        return EnvironmentInfo(
            packageName,
            versionCode,
            Build.VERSION.SDK_INT,
            getUniqueId(),
            deviceName
        )
    }

    private fun getUniqueId(): String {
        val uuid = uniqueId ?: readUniqueId()
        uniqueId = uuid
        return uuid
    }

    private fun readUniqueId(): String {
        var uuid: String? = null
        var input: DataInputStream? = null
        val file = uuidFile()
        try {
            if (file.exists()) {
                input = DataInputStream(FileInputStream(file))
                uuid = input.readUTF()
            }
        } catch (ignored: IOException) {
        } finally {
            input.safeClose()
        }
        return uuid ?: generateUuid()
    }

    private fun generateUuid(): String {
        val uuid = UUID.randomUUID().toString()
        var os: DataOutputStream? = null
        val file = uuidFile()
        try {
            os = DataOutputStream(FileOutputStream(file))
            os.writeUTF(uuid)
        } catch (ignored: IOException) {
        } finally {
            os.safeClose()
        }
        return uuid
    }

    private fun uuidFile() = File(context.filesDir, "bananalytics.uuid")

}

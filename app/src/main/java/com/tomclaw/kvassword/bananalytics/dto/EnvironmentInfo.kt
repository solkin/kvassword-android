package com.tomclaw.kvassword.bananalytics.dto

import com.google.gson.annotations.SerializedName

data class EnvironmentInfo(
    @SerializedName("app_id") val appId: String,
    @SerializedName("app_version") val appVersion: Int,
    @SerializedName("os_version") val osVersion: Int,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
)

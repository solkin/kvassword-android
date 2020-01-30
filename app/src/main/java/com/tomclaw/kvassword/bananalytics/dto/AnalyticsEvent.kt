package com.tomclaw.kvassword.bananalytics.dto

import com.google.gson.annotations.SerializedName

class AnalyticsEvent(
        @SerializedName("event") val event: String,
        @SerializedName("payload") val payload: String?,
        @SerializedName("time") val time: Long
)
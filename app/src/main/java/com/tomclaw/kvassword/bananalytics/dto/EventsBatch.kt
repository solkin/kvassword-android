package com.tomclaw.kvassword.bananalytics.dto

import com.google.gson.annotations.SerializedName

data class EventsBatch(
    @SerializedName("info") val info: EnvironmentInfo,
    @SerializedName("events") val events: List<AnalyticsEvent>,
)

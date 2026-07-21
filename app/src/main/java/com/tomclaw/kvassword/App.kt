package com.tomclaw.kvassword

import android.app.Application
import android.os.Build
import com.tomclaw.bananalytics.Bananalytics
import com.tomclaw.bananalytics.BananalyticsConfig
import com.tomclaw.bananalytics.BananalyticsImpl
import com.tomclaw.bananalytics.EnvironmentProvider
import com.tomclaw.bananalytics.api.Environment
import java.util.Locale

class App : Application() {

    lateinit var bananalytics: Bananalytics
        private set

    override fun onCreate() {
        super.onCreate()
        val settings = Settings(this)
        bananalytics = BananalyticsImpl(
            filesDir = filesDir,
            config = BananalyticsConfig(
                baseUrl = BANANALYTICS_BASE_URL,
                apiKey = BANANALYTICS_API_KEY
            ),
            environmentProvider = AppEnvironmentProvider(this, settings),
            isDebug = BuildConfig.DEBUG
        )
        bananalytics.install()
    }

    private companion object {
        const val BANANALYTICS_BASE_URL = "https://bnn.citron.dev"
        const val BANANALYTICS_API_KEY = "bnn_Ufr2aD0NTR2yXmbKuDFGgSzPt-Bc-wNb"
    }
}

private class AppEnvironmentProvider(
    private val application: Application,
    private val settings: Settings
) : EnvironmentProvider {

    override fun environment(): Environment {
        val info = application.packageManager.getPackageInfo(application.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return Environment(
            packageName = application.packageName,
            appVersion = versionCode,
            appVersionName = info.versionName.orEmpty(),
            deviceId = settings.deviceId,
            osVersion = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            country = Locale.getDefault().country,
            language = Locale.getDefault().language
        )
    }
}

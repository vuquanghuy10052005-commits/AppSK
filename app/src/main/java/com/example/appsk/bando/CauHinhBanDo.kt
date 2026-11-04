package com.example.appsk.bando

import android.content.Context
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import java.io.File

object CauHinhBanDo {

    /** Phải gọi trước khi inflate layout có MapView. */
    fun khoiTao(context: Context) {
        val appContext = context.applicationContext
        val cauHinh = Configuration.getInstance()
        cauHinh.load(appContext, PreferenceManager.getDefaultSharedPreferences(appContext))
        cauHinh.userAgentValue = appContext.packageName
        cauHinh.osmdroidBasePath = File(appContext.cacheDir, "osmdroid")
        cauHinh.osmdroidTileCache = File(cauHinh.osmdroidBasePath, "tiles")
    }
}
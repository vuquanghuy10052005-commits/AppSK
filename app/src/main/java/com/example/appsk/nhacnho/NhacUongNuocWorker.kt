package com.example.appsk.nhacnho

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.appsk.MainActivity
import com.example.appsk.R

class NhacUongNuocWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {

    companion object {
        const val MA_KENH = "kenh_nhac_nho"
        private const val MA_THONG_BAO_NUOC = 2001
    }

    override fun doWork(): Result {
        taoKenhNeuChuaCo()
        hienThiThongBao()
        return Result.success()
    }

    private fun taoKenhNeuChuaCo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kenh = NotificationChannel(
                MA_KENH,
                applicationContext.getString(R.string.ten_kenh_nhac_nho),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.mo_ta_kenh_nhac_nho)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(kenh)
        }
    }

    private fun hienThiThongBao() {
        val intentMoApp = Intent(applicationContext, MainActivity::class.java)
        val pendingMoApp = PendingIntent.getActivity(
            applicationContext, 0, intentMoApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val thongBao = NotificationCompat.Builder(applicationContext, MA_KENH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.tieu_de_nhac_uong_nuoc))
            .setContentText(applicationContext.getString(R.string.noi_dung_nhac_uong_nuoc))
            .setContentIntent(pendingMoApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(MA_THONG_BAO_NUOC, thongBao)
    }
}
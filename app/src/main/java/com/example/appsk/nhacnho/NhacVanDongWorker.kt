package com.example.appsk.nhacnho

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.appsk.MainActivity
import com.example.appsk.R

class NhacVanDongWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {

    companion object {
        private const val MA_THONG_BAO_VAN_DONG = 2002
    }

    override fun doWork(): Result {
        hienThiThongBao()
        return Result.success()
    }

    private fun hienThiThongBao() {
        val intentMoApp = Intent(applicationContext, MainActivity::class.java)
        val pendingMoApp = PendingIntent.getActivity(
            applicationContext, 0, intentMoApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val thongBao = NotificationCompat.Builder(applicationContext, NhacUongNuocWorker.MA_KENH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.tieu_de_nhac_van_dong))
            .setContentText(applicationContext.getString(R.string.noi_dung_nhac_van_dong))
            .setContentIntent(pendingMoApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(MA_THONG_BAO_VAN_DONG, thongBao)
    }
}
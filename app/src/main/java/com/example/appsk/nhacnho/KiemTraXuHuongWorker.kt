package com.example.appsk.nhacnho

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appsk.R
import com.example.appsk.canhbao.BoPhanTichXuHuong
import com.example.appsk.canhbao.CanhBaoTamThoi
import com.example.appsk.canhbao.MucDoCanhBao
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.ui.canhbao.CanhBaoSucKhoeActivity
import kotlinx.coroutines.CancellationException

/**
 * Chạy mỗi ngày: phân tích xu hướng, lưu cảnh báo mới và chỉ thông báo
 * khi có cảnh báo mới ở mức "Cần chú ý" trở lên.
 */
class KiemTraXuHuongWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        private const val MA_THONG_BAO_XU_HUONG = 2003
    }

    override suspend fun doWork(): Result {
        return try {
            if (!QuanLyHoSo(applicationContext).daCoHoSo()) return Result.success()

            val boPhanTich = BoPhanTichXuHuong(applicationContext)
            val ketQua = boPhanTich.phanTich()
            val canhBaoMoi = boPhanTich.luuCanhBaoMoi(ketQua.canhBao)

            val canThongBao = canhBaoMoi.filter { it.mucDo >= MucDoCanhBao.VUA }
            if (canThongBao.isNotEmpty()) hienThiThongBao(canThongBao)

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun hienThiThongBao(danhSach: List<CanhBaoTamThoi>) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kenh = NotificationChannel(
                NhacUongNuocWorker.MA_KENH,
                applicationContext.getString(R.string.ten_kenh_nhac_nho),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.mo_ta_kenh_nhac_nho)
            }
            manager.createNotificationChannel(kenh)
        }

        val intent = Intent(applicationContext, CanhBaoSucKhoeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val thongBao = NotificationCompat.Builder(applicationContext, NhacUongNuocWorker.MA_KENH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.tieu_de_thong_bao_xu_huong))
            .setContentText(danhSach.first().tieuDe)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(danhSach.joinToString("\n") { "• ${it.tieuDe}" })
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        manager.notify(MA_THONG_BAO_XU_HUONG, thongBao)
    }
}
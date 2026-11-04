package com.example.appsk.nhacnho

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.appsk.dongbo.DongBo
import com.example.appsk.taikhoan.TaiKhoan
import java.util.concurrent.TimeUnit

class QuanLyNhacNho(context: Context) {

    private val appContext = context.applicationContext

    // Cài đặt được nhớ riêng cho từng tài khoản
    private val prefs get() = TaiKhoan.layPrefs(appContext, "cai_dat_nhac_nho")

    companion object {
        const val LOAI_NUOC = "nuoc"
        const val LOAI_VAN_DONG = "van_dong"
        const val LOAI_XU_HUONG = "xu_huong"
        val TAT_CA_LOAI = listOf(LOAI_NUOC, LOAI_VAN_DONG, LOAI_XU_HUONG)

        // Khóa của phiên bản cũ có một công tắc chung cho cả 3 loại
        private const val KHOA_CU_DA_BAT = "da_bat"

        private fun tenWork(loai: String): String = when (loai) {
            LOAI_NUOC -> "nhac_uong_nuoc"
            LOAI_VAN_DONG -> "nhac_van_dong"
            else -> "kiem_tra_xu_huong"
        }
    }

    fun dangBat(loai: String): Boolean {
        val p = prefs
        val khoa = "bat_$loai"
        return if (p.contains(khoa)) p.getBoolean(khoa, false)
        else p.getBoolean(KHOA_CU_DA_BAT, false)
    }

    /** Lưu cài đặt và bật hoặc hủy việc nền tương ứng. */
    fun datTrangThai(loai: String, bat: Boolean, yeuCauDongBo: Boolean = true) {
        prefs.edit().putBoolean("bat_$loai", bat).apply()
        if (bat) lenLich(loai) else huyLich(loai)
        if (yeuCauDongBo) DongBo.yeuCau(appContext)
    }

    /** Dùng khi mở app: đưa các việc nền về đúng theo cài đặt của tài khoản đang đăng nhập. */
    fun kichHoatTheoCaiDat() {
        TAT_CA_LOAI.forEach { loai ->
            if (dangBat(loai)) lenLich(loai) else huyLich(loai)
        }
    }

    /** Dùng khi đăng xuất: hủy các việc nền (kể cả đồng bộ) nhưng giữ nguyên cài đặt đã lưu. */
    fun huyTatCaViecNen() {
        TAT_CA_LOAI.forEach { huyLich(it) }
        DongBo.huyLich(appContext)
    }

    private fun lenLich(loai: String) {
        val workManager = WorkManager.getInstance(appContext)
        when (loai) {
            LOAI_NUOC -> workManager.enqueueUniquePeriodicWork(
                tenWork(loai),
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NhacUongNuocWorker>(2, TimeUnit.HOURS).build()
            )
            LOAI_VAN_DONG -> workManager.enqueueUniquePeriodicWork(
                tenWork(loai),
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NhacVanDongWorker>(3, TimeUnit.HOURS).build()
            )
            else -> workManager.enqueueUniquePeriodicWork(
                tenWork(loai),
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<KiemTraXuHuongWorker>(24, TimeUnit.HOURS).build()
            )
        }
    }

    private fun huyLich(loai: String) {
        WorkManager.getInstance(appContext).cancelUniqueWork(tenWork(loai))
    }
}
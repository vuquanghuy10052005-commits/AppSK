package com.example.appsk.dongbo

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.appsk.data.csdl.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object DongBo {

    private const val TEN_VIEC_NGAY = "dong_bo_ngay"
    private const val TEN_VIEC_DINH_KY = "dong_bo_dinh_ky"

    // Thay đổi ở các bảng này sẽ kích hoạt đồng bộ.
    // Không gồm số bước chân vì nó đổi liên tục khi đang đi, số bước được gửi theo lịch định kỳ.
    private val BANG_THEO_DOI = arrayOf(
        "lich_su_can_nang", "lich_su_chay_bo", "bua_an", "nuoc_uong", "tin_nhan_ai",
        "phan_tich_ai", "ke_hoach_ai", "phuong_an_du_phong", "canh_bao_suc_khoe",
        "phan_tich_anh"
    )

    private val rangBuocMang = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    @Volatile
    private var dbDangTheoDoi: AppDatabase? = null

    /**
     * Gọi mỗi khi dữ liệu thay đổi. Nhiều lần gọi liên tiếp được gộp thành một lần chạy,
     * chạy sau [doTreGiay] giây và chỉ khi có mạng. Chỉ có tác dụng khi người dùng đã bật đồng bộ.
     */
    fun yeuCau(context: Context, doTreGiay: Long = 15) {
        val appContext = context.applicationContext
        if (!CaiDatDongBo.dangBat(appContext)) return

        val yeuCau = OneTimeWorkRequestBuilder<DongBoWorker>()
            .setConstraints(rangBuocMang)
            .setInitialDelay(doTreGiay, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(TEN_VIEC_NGAY, ExistingWorkPolicy.REPLACE, yeuCau)
    }

    /** Gọi khi mở app: bật lịch và theo dõi nếu đã bật đồng bộ, ngược lại thì hủy hết. */
    fun kichHoat(context: Context) {
        val appContext = context.applicationContext
        if (!CaiDatDongBo.dangBat(appContext)) {
            huyLich(appContext)
            return
        }
        lenLichDinhKy(appContext)
        theoDoiThayDoi(appContext)
        yeuCau(appContext, 3)
    }

    fun huyLich(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(TEN_VIEC_NGAY)
        workManager.cancelUniqueWork(TEN_VIEC_DINH_KY)
    }

    private fun lenLichDinhKy(appContext: Context) {
        val yeuCau = PeriodicWorkRequestBuilder<DongBoWorker>(6, TimeUnit.HOURS)
            .setConstraints(rangBuocMang)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            TEN_VIEC_DINH_KY, ExistingPeriodicWorkPolicy.KEEP, yeuCau
        )
    }

    /** Theo dõi database của tài khoản đang đăng nhập; mỗi database chỉ đăng ký một lần. */
    private fun theoDoiThayDoi(appContext: Context) {
        val db = AppDatabase.layInstance(appContext)
        synchronized(this) {
            if (dbDangTheoDoi === db) return
            dbDangTheoDoi = db
        }

        // Đăng ký ở luồng nền để không chạm vào database trên luồng giao diện
        CoroutineScope(Dispatchers.IO).launch {
            db.invalidationTracker.addObserver(object : InvalidationTracker.Observer(BANG_THEO_DOI) {
                override fun onInvalidated(tables: Set<String>) {
                    yeuCau(appContext)
                }
            })
        }
    }
}
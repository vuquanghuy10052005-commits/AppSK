package com.example.appsk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.withTransaction
import com.example.appsk.data.DiemToaDo
import com.example.appsk.data.LoaiHoatDong
import com.example.appsk.data.TrangThaiChayBo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.ChiTietBuoiTap
import com.example.appsk.data.csdl.DiemTuyenDuong
import com.example.appsk.data.csdl.LichSuChayBo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Service chạy ngầm (foreground) để tiếp tục theo dõi dù màn hình tắt, chuyển app khác
 * hay thoát khỏi ChayBoActivity. Chỉ dừng hẳn khi người dùng bấm "Dừng lại".
 */
class TheoDoiChayBoService : Service() {

    companion object {
        const val ACTION_DUNG_LAI = "com.example.appsk.ACTION_DUNG_LAI_CHAY_BO"
        private const val MA_KENH_THONG_BAO = "kenh_chay_bo"
        private const val MA_THONG_BAO = 1001
        @Volatile
        var dangTheoDoi = false
    }

    inner class LocalBinder : Binder() {
        fun layService(): TheoDoiChayBoService = this@TheoDoiChayBoService
    }

    private val binder = LocalBinder()

    private val _trangThai = MutableLiveData(TrangThaiChayBo())
    val trangThai: LiveData<TrangThaiChayBo> get() = _trangThai

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var viTriTruoc: Location? = null
    private var tongQuangDuongMet = 0f
    private val duongDi = mutableListOf<DiemToaDo>()
    private var thoiGianBatDau = 0L
    private var loaiHoatDong = LoaiHoatDong.CHAY_BO

    private val handler = Handler(Looper.getMainLooper())
    private val taskCapNhatThoiGian = object : Runnable {
        override fun run() {
            if (_trangThai.value?.dangChay == true) {
                capNhatTrangThai()
                capNhatThongBao()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val viTriMoi = result.lastLocation ?: return
            duongDi.add(DiemToaDo(viTriMoi.latitude, viTriMoi.longitude))
            viTriTruoc?.let { tongQuangDuongMet += it.distanceTo(viTriMoi) }
            viTriTruoc = viTriMoi
            capNhatTrangThai()
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        taoKenhThongBao()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DUNG_LAI) {
            dungLai()
        }
        return START_STICKY
    }

    fun batDau(loai: String) {
        if (_trangThai.value?.dangChay == true) return

        loaiHoatDong = loai
        thoiGianBatDau = SystemClock.elapsedRealtime()
        tongQuangDuongMet = 0f
        viTriTruoc = null
        duongDi.clear()

        _trangThai.value = TrangThaiChayBo(dangChay = true, loaiHoatDong = loaiHoatDong)
        dangTheoDoi = true

        startForeground(MA_THONG_BAO, taoThongBao(0f, 0L))

        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val yeuCau = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateDistanceMeters(3f)
                .build()
            fusedLocationClient.requestLocationUpdates(yeuCau, locationCallback, Looper.getMainLooper())
        }

        handler.post(taskCapNhatThoiGian)
    }

    fun dungLai(): TrangThaiChayBo {
        val trangThaiHienTai = _trangThai.value ?: TrangThaiChayBo()

        if (!trangThaiHienTai.dangChay) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return trangThaiHienTai
        }
        dangTheoDoi = false

        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(taskCapNhatThoiGian)

        val thoiGianTroiQuaGiay = (SystemClock.elapsedRealtime() - thoiGianBatDau) / 1000
        val quangDuongKm = tongQuangDuongMet / 1000f
        val diemDaGhi = duongDi.toList()

        if (quangDuongKm > 0f) {
            luuBuoiTap(quangDuongKm, thoiGianTroiQuaGiay, loaiHoatDong, diemDaGhi)
        }

        val ketQuaCuoi = TrangThaiChayBo(
            dangChay = false,
            quangDuongKm = quangDuongKm,
            thoiGianGiay = thoiGianTroiQuaGiay,
            duongDi = diemDaGhi,
            loaiHoatDong = loaiHoatDong
        )

        _trangThai.value = TrangThaiChayBo(dangChay = false, loaiHoatDong = loaiHoatDong)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return ketQuaCuoi
    }

    private fun capNhatTrangThai() {
        val thoiGianTroiQuaGiay = (SystemClock.elapsedRealtime() - thoiGianBatDau) / 1000
        _trangThai.value = TrangThaiChayBo(
            dangChay = true,
            quangDuongKm = tongQuangDuongMet / 1000f,
            thoiGianGiay = thoiGianTroiQuaGiay,
            duongDi = duongDi.toList(),
            loaiHoatDong = loaiHoatDong
        )
    }

    private fun taoKenhThongBao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kenh = NotificationChannel(
                MA_KENH_THONG_BAO,
                getString(R.string.ten_kenh_thong_bao_chay_bo),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.mo_ta_kenh_thong_bao_chay_bo)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(kenh)
        }
    }

    private fun taoThongBao(quangDuongKm: Float, thoiGianGiay: Long): Notification {
        val noiDung = getString(
            R.string.noi_dung_thong_bao_dang_chay,
            String.format(Locale.getDefault(), "%.2f", quangDuongKm),
            dinhDangThoiGian(thoiGianGiay)
        )

        val intentDung = Intent(this, TheoDoiChayBoService::class.java).apply {
            action = ACTION_DUNG_LAI
        }
        val pendingDung = PendingIntent.getService(
            this, 0, intentDung,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intentMoApp = packageManager.getLaunchIntentForPackage(packageName)
        val pendingMoApp = PendingIntent.getActivity(
            this, 0, intentMoApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MA_KENH_THONG_BAO)
            .setContentTitle(getString(R.string.bt_dang_theo_doi, LoaiHoatDong.ten(loaiHoatDong)))
            .setContentText(noiDung)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingMoApp)
            .addAction(0, getString(R.string.hanh_dong_dung_lai), pendingDung)
            .setOngoing(true)
            .build()
    }

    private fun capNhatThongBao() {
        val trangThaiHienTai = _trangThai.value ?: return
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            MA_THONG_BAO,
            taoThongBao(trangThaiHienTai.quangDuongKm, trangThaiHienTai.thoiGianGiay)
        )
    }

    private fun dinhDangThoiGian(tongGiay: Long): String {
        val gio = tongGiay / 3600
        val phut = (tongGiay % 3600) / 60
        val giay = tongGiay % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", gio, phut, giay)
    }

    /**
     * Lưu buổi tập, loại hoạt động và toàn bộ điểm GPS trong một giao dịch:
     * hoặc lưu đủ cả ba, hoặc không lưu gì (tránh buổi tập thiếu tuyến đường).
     */
    private fun luuBuoiTap(
        quangDuongKm: Float,
        thoiGianGiay: Long,
        loai: String,
        diem: List<DiemToaDo>
    ) {
        val tocDoTrungBinh = if (thoiGianGiay > 0) quangDuongKm / (thoiGianGiay / 3600f) else 0f
        val thoiGianKetThuc = System.currentTimeMillis()
        val appContext = applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.layInstance(appContext)
            db.withTransaction {
                val phienId = db.lichSuChayBoDao().themBanGhi(
                    LichSuChayBo(
                        quangDuongKm = quangDuongKm,
                        thoiGianGiay = thoiGianGiay,
                        tocDoTrungBinh = tocDoTrungBinh,
                        thoiGianGhiNhan = thoiGianKetThuc
                    )
                )
                db.chiTietBuoiTapDao().luu(ChiTietBuoiTap(phienId, loai))
                db.diemTuyenDuongDao().themNhieuDiem(
                    diem.map { DiemTuyenDuong(phienId = phienId, vido = it.vido, kinhdo = it.kinhdo) }
                )
            }
        }
    }

    override fun onDestroy() {
        dangTheoDoi = false
        super.onDestroy()
        handler.removeCallbacks(taskCapNhatThoiGian)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
package com.example.appsk.buocchan

import android.content.Context
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.BuocChanNgay
import com.example.appsk.taikhoan.TaiKhoan
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cảm biến bước chân trả về số bước CỘNG DỒN từ lúc khởi động máy.
 * Mỗi lần đọc, ta lấy phần chênh so với lần đọc trước và cộng vào ngày hiện tại
 * của tài khoản đang đăng nhập.
 */
object GhiNhanBuocChan {

    private const val TEN_PREFS = "buoc_chan_cam_bien"
    private const val KEY_GIA_TRI_TRUOC = "gia_tri_truoc"

    private val khoa = Mutex()

    fun ngayChuoi(thoiGian: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(thoiGian))

    /** Ghi nhận một lần đọc cảm biến, trả về tổng số bước của hôm nay. */
    suspend fun ghiNhan(context: Context, giaTriCamBien: Long): Int = khoa.withLock {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(TEN_PREFS, Context.MODE_PRIVATE)

        val giaTriTruoc = prefs.getLong(KEY_GIA_TRI_TRUOC, -1L)
        val chenhLech = when {
            giaTriTruoc < 0 -> 0L                          // lần đọc đầu tiên: chỉ đặt mốc
            giaTriCamBien >= giaTriTruoc -> giaTriCamBien - giaTriTruoc
            else -> giaTriCamBien                           // máy vừa khởi động lại, cảm biến về 0
        }
        prefs.edit().putLong(KEY_GIA_TRI_TRUOC, giaTriCamBien).apply()

        // Chưa đăng nhập: chỉ cập nhật mốc để số bước lúc này không bị tính cho ai
        if (!TaiKhoan.daDangNhap()) return@withLock 0

        val dao = AppDatabase.layChoUid(appContext, TaiKhoan.uid()).buocChanNgayDao()
        val bayGio = System.currentTimeMillis()
        val homNay = ngayChuoi(bayGio)

        // Luôn đọc lại từ database: số bước hôm nay có thể vừa được cập nhật từ đồng bộ đám mây
        val daLuu = dao.laySoBuoc(homNay) ?: 0
        if (chenhLech > 0) {
            val tong = daLuu + chenhLech.toInt()
            dao.luuSoBuoc(BuocChanNgay(homNay, tong, bayGio))
            tong
        } else {
            daLuu
        }
    }

    /** Đọc tổng số bước hôm nay đã lưu (dùng để hiển thị ngay khi mở màn hình). */
    suspend fun docSoBuocHomNay(context: Context): Int = khoa.withLock {
        if (!TaiKhoan.daDangNhap()) return@withLock 0
        val dao = AppDatabase.layChoUid(context.applicationContext, TaiKhoan.uid()).buocChanNgayDao()
        dao.laySoBuoc(ngayChuoi(System.currentTimeMillis())) ?: 0
    }
}
package com.example.appsk.dongbo

import android.content.Context
import com.example.appsk.taikhoan.TaiKhoan

object CaiDatDongBo {

    private const val TEN = "dong_bo"

    private fun prefs(context: Context) = TaiKhoan.layPrefs(context, TEN)

    fun dangBat(context: Context): Boolean =
        TaiKhoan.daDangNhap() && prefs(context).getBoolean("bat", false)

    /** Lưu lựa chọn của người dùng và đánh dấu đã hỏi (để không hỏi lại). */
    fun datBat(context: Context, bat: Boolean) {
        prefs(context).edit().putBoolean("bat", bat).putBoolean("da_hoi", true).apply()
    }

    fun daHoi(context: Context): Boolean = prefs(context).getBoolean("da_hoi", false)

    // Mốc thời gian (theo đồng hồ máy chủ) của tài liệu mới nhất đã kéo về cho từng loại
    fun layMocKeo(context: Context, bang: String): Long = prefs(context).getLong("keo_$bang", 0L)

    fun datMocKeo(context: Context, bang: String, moc: Long) {
        prefs(context).edit().putLong("keo_$bang", moc).apply()
    }

    // Trạng thái đồng bộ phần cài đặt (hồ sơ, mục tiêu bước, nhắc nhở)
    fun layHashCaiDat(context: Context): String = prefs(context).getString("cai_dat_hash", "") ?: ""

    fun datHashCaiDat(context: Context, hash: String) {
        prefs(context).edit().putString("cai_dat_hash", hash).apply()
    }

    fun layMocCaiDatMay(context: Context): Long = prefs(context).getLong("cai_dat_may", 0L)

    fun datMocCaiDatMay(context: Context, moc: Long) {
        prefs(context).edit().putLong("cai_dat_may", moc).apply()
    }

    fun luuKetQua(context: Context, thanhCong: Boolean, thongBao: String) {
        val editor = prefs(context).edit().putString("thong_bao", thongBao)
        if (thanhCong) editor.putLong("lan_cuoi_thanh_cong", System.currentTimeMillis())
        editor.apply()
    }

    fun layThoiGianCuoi(context: Context): Long = prefs(context).getLong("lan_cuoi_thanh_cong", 0L)

    fun layThongBao(context: Context): String = prefs(context).getString("thong_bao", "") ?: ""

    /** Dùng sau khi xóa dữ liệu đám mây: quên các mốc để lần bật lại sẽ đồng bộ từ đầu. */
    fun xoaMocDongBo(context: Context) {
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys
            .filter { it.startsWith("keo_") || it == "cai_dat_hash" || it == "cai_dat_may" }
            .forEach { editor.remove(it) }
        editor.putString("thong_bao", "")
        editor.putLong("lan_cuoi_thanh_cong", 0L)
        editor.apply()
    }
}
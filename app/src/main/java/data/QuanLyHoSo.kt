package com.example.appsk.data

import android.content.Context
import com.example.appsk.dongbo.DongBo
import com.example.appsk.taikhoan.TaiKhoan

class QuanLyHoSo(context: Context) {

    private val appContext = context.applicationContext

    // Lấy theo tài khoản đang đăng nhập ở mỗi lần dùng
    private val prefs get() = TaiKhoan.layPrefs(appContext, "ho_so_suc_khoe")

    companion object {
        private const val KEY_TUOI = "tuoi"
        private const val KEY_GIOI_TINH = "gioi_tinh"
        private const val KEY_CHIEU_CAO = "chieu_cao"
        private const val KEY_CAN_NANG = "can_nang"
        private const val KEY_MUC_DO_HOAT_DONG = "muc_do_hoat_dong"
        private const val KEY_MUC_TIEU = "muc_tieu"
        private const val KEY_HE_SO_HOAT_DONG = "he_so_hoat_dong"

        val HE_SO_THEO_MUC_DO = mapOf(
            "Ít vận động (ít hoặc không tập)" to 1.2,
            "Vận động nhẹ (1-3 buổi/tuần)" to 1.375,
            "Vận động vừa (3-5 buổi/tuần)" to 1.55,
            "Vận động nhiều (6-7 buổi/tuần)" to 1.725,
            "Vận động rất nhiều (cường độ cao, lao động chân tay)" to 1.9
        )
    }

    /** yeuCauDongBo = false dùng khi chính bộ máy đồng bộ ghi dữ liệu về, để không gọi lại chính nó. */
    fun luuHoSo(hoSo: HoSoSucKhoe, yeuCauDongBo: Boolean = true) {
        prefs.edit()
            .putInt(KEY_TUOI, hoSo.tuoi)
            .putString(KEY_GIOI_TINH, hoSo.gioiTinh)
            .putFloat(KEY_CHIEU_CAO, hoSo.chieuCaoCm)
            .putFloat(KEY_CAN_NANG, hoSo.canNangKg)
            .putString(KEY_MUC_DO_HOAT_DONG, hoSo.mucDoHoatDong)
            .putString(KEY_MUC_TIEU, hoSo.mucTieu)
            .putFloat(KEY_HE_SO_HOAT_DONG, hoSo.heSoHoatDong.toFloat())
            .apply()
        if (yeuCauDongBo) DongBo.yeuCau(appContext)
    }

    fun docHoSo(): HoSoSucKhoe {
        val p = prefs
        return HoSoSucKhoe(
            tuoi = p.getInt(KEY_TUOI, 0),
            gioiTinh = p.getString(KEY_GIOI_TINH, "Nam") ?: "Nam",
            chieuCaoCm = p.getFloat(KEY_CHIEU_CAO, 0f),
            canNangKg = p.getFloat(KEY_CAN_NANG, 0f),
            mucDoHoatDong = p.getString(KEY_MUC_DO_HOAT_DONG, "") ?: "",
            mucTieu = p.getString(KEY_MUC_TIEU, "") ?: "",
            heSoHoatDong = p.getFloat(KEY_HE_SO_HOAT_DONG, 1.2f).toDouble()
        )
    }

    fun daCoHoSo(): Boolean {
        return prefs.contains(KEY_CHIEU_CAO)
    }
}
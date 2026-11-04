package com.example.appsk.data

import android.content.Context
import com.example.appsk.dongbo.DongBo
import com.example.appsk.taikhoan.TaiKhoan

class QuanLyBuocChan(context: Context) {

    private val appContext = context.applicationContext

    private val prefs get() = TaiKhoan.layPrefs(appContext, "buoc_chan")

    companion object {
        private const val KEY_MUC_TIEU_BUOC = "muc_tieu_buoc"
        const val MUC_TIEU_MAC_DINH = 8000
    }

    fun luuMucTieuBuoc(mucTieu: Int, yeuCauDongBo: Boolean = true) {
        prefs.edit().putInt(KEY_MUC_TIEU_BUOC, mucTieu).apply()
        if (yeuCauDongBo) DongBo.yeuCau(appContext)
    }

    fun docMucTieuBuoc(): Int {
        return prefs.getInt(KEY_MUC_TIEU_BUOC, MUC_TIEU_MAC_DINH)
    }
}
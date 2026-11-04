package com.example.appsk.taikhoan

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

object TaiKhoan {

    const val KHACH = "khach"

    /** Mã tài khoản đang đăng nhập, hoặc "khach" nếu chưa đăng nhập. */
    fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: KHACH

    fun daDangNhap(): Boolean = FirebaseAuth.getInstance().currentUser != null

    /**
     * SharedPreferences riêng cho từng tài khoản.
     * Dữ liệu cũ (từ trước khi tách theo tài khoản) được giao cho tài khoản đầu tiên dùng đến nó.
     */
    fun layPrefs(context: Context, tenGoc: String): SharedPreferences {
        val appContext = context.applicationContext
        val uid = uid()
        val prefs = appContext.getSharedPreferences("${tenGoc}_$uid", Context.MODE_PRIVATE)
        if (uid != KHACH) nhanPrefsCu(appContext, tenGoc, prefs)
        return prefs
    }

    private fun nhanPrefsCu(appContext: Context, tenGoc: String, prefsMoi: SharedPreferences) {
        val chung = appContext.getSharedPreferences("tai_khoan_chung", Context.MODE_PRIVATE)
        val khoaDaNhan = "da_nhan_prefs_$tenGoc"
        if (chung.getBoolean(khoaDaNhan, false)) return

        val duLieuCu = appContext.getSharedPreferences(tenGoc, Context.MODE_PRIVATE).all
        if (duLieuCu.isNotEmpty() && prefsMoi.all.isEmpty()) {
            val editor = prefsMoi.edit()
            for ((khoa, giaTri) in duLieuCu) {
                when (giaTri) {
                    is Int -> editor.putInt(khoa, giaTri)
                    is Long -> editor.putLong(khoa, giaTri)
                    is Float -> editor.putFloat(khoa, giaTri)
                    is Boolean -> editor.putBoolean(khoa, giaTri)
                    is String -> editor.putString(khoa, giaTri)
                    is Set<*> -> editor.putStringSet(khoa, giaTri.filterIsInstance<String>().toSet())
                }
            }
            editor.apply()
        }
        chung.edit().putBoolean(khoaDaNhan, true).apply()
    }
}
package com.example.appsk.data

object LoaiHoatDong {
    const val DI_BO = "DI_BO"
    const val CHAY_BO = "CHAY_BO"
    const val DAP_XE = "DAP_XE"

    /** loai = null là các buổi tập cũ, được ghi trước khi app có chức năng chọn loại. */
    fun ten(loai: String?): String = when (loai) {
        DI_BO -> "Đi bộ"
        CHAY_BO -> "Chạy bộ"
        DAP_XE -> "Đạp xe"
        else -> "Chạy bộ / đi bộ"
    }

    fun bieuTuong(loai: String?): String = when (loai) {
        DI_BO -> "🚶"
        CHAY_BO -> "🏃"
        DAP_XE -> "🚴"
        else -> "👟"
    }
}
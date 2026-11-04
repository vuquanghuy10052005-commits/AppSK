package com.example.appsk.data

import java.util.Locale
import kotlin.math.roundToInt

object DinhDang {

    fun thoiLuong(tongGiay: Long): String {
        val gio = tongGiay / 3600
        val phut = (tongGiay % 3600) / 60
        val giay = tongGiay % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", gio, phut, giay)
    }

    /** Nhịp độ (pace): số phút cho mỗi km, ví dụ 6:05 /km. */
    fun nhipDo(phutMoiKm: Double): String {
        if (phutMoiKm <= 0.0 || phutMoiKm.isNaN() || phutMoiKm.isInfinite()) return "--"
        var phut = phutMoiKm.toInt()
        var giay = ((phutMoiKm - phut) * 60).roundToInt()
        if (giay == 60) {
            phut += 1
            giay = 0
        }
        return String.format(Locale.getDefault(), "%d:%02d /km", phut, giay)
    }
}
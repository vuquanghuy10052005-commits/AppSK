package com.example.appsk.data

data class HoSoSucKhoe(
    val tuoi: Int = 0,
    val gioiTinh: String = "Nam",
    val chieuCaoCm: Float = 0f,
    val canNangKg: Float = 0f,
    val mucDoHoatDong: String = "",
    val mucTieu: String = "",
    val heSoHoatDong: Double = 1.2
) {
    fun tinhBMI(): Double {
        if (chieuCaoCm <= 0f) return 0.0
        val chieuCaoM = chieuCaoCm / 100.0
        return canNangKg / (chieuCaoM * chieuCaoM)
    }

    fun tinhBMR(): Double {
        val base = 10 * canNangKg + 6.25 * chieuCaoCm - 5 * tuoi
        return if (gioiTinh == "Nam") base + 5 else base - 161
    }

    fun tinhTDEE(): Double {
        return tinhBMR() * heSoHoatDong
    }

    fun phanLoaiBMI(): String {
        val bmi = tinhBMI()
        return when {
            bmi <= 0.0 -> ""
            bmi < 18.5 -> "Thiếu cân"
            bmi < 25.0 -> "Bình thường"
            bmi < 30.0 -> "Thừa cân"
            else -> "Béo phì"
        }
    }
}
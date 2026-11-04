package com.example.appsk.thoitiet

import kotlin.math.roundToInt

data class ThoiTiet(
    val nhietDo: Double,
    val camGiac: Double,
    val doAm: Int,
    val luongMua: Double,
    val maThoiTiet: Int,
    val tocDoGio: Double,
    val nhietDoCaoNhat: Double,
    val nhietDoThapNhat: Double,
    val xacSuatMua: Int
) {

    fun moTa(): String = when (maThoiTiet) {
        0 -> "Trời quang"
        1, 2 -> "Ít mây"
        3 -> "Nhiều mây"
        45, 48 -> "Sương mù"
        in 51..57 -> "Mưa phùn"
        in 61..67 -> "Mưa"
        in 71..77 -> "Tuyết"
        in 80..82 -> "Mưa rào"
        in 85..86 -> "Mưa tuyết"
        in 95..99 -> "Dông"
        else -> "Không rõ"
    }

    fun bieuTuong(): String = when (maThoiTiet) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌧️"
        in 85..86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }

    fun tomTat(): String = "${moTa()}, ${nhietDo.roundToInt()}°C"

    /**
     * Quy tắc cố định do ứng dụng tự đánh giá, không dùng AI.
     * Danh sách rỗng nghĩa là thời tiết phù hợp để tập ngoài trời.
     */
    fun lyDoKhongThuanLoi(): List<String> {
        val lyDo = mutableListOf<String>()
        if (maThoiTiet >= 51 || luongMua > 0.1) {
            lyDo.add("đang có mưa hoặc dông")
        } else if (xacSuatMua >= 60) {
            lyDo.add("khả năng mưa trong ngày cao ($xacSuatMua%)")
        }
        if (camGiac >= 37) lyDo.add("trời rất nóng (cảm giác ${camGiac.roundToInt()}°C)")
        if (nhietDo <= 10) lyDo.add("trời lạnh (${nhietDo.roundToInt()}°C)")
        if (tocDoGio >= 40) lyDo.add("gió mạnh (${tocDoGio.roundToInt()} km/h)")
        return lyDo
    }
}
package com.example.appsk.data

data class TrangThaiChayBo(
    val dangChay: Boolean = false,
    val quangDuongKm: Float = 0f,
    val thoiGianGiay: Long = 0L,
    val duongDi: List<DiemToaDo> = emptyList(),
    val loaiHoatDong: String = LoaiHoatDong.CHAY_BO
)
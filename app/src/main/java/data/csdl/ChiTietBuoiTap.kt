package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chi_tiet_buoi_tap")
data class ChiTietBuoiTap(
    @PrimaryKey
    val phienId: Long,
    val loaiHoatDong: String
)
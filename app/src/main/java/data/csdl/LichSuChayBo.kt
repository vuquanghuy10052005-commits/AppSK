package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lich_su_chay_bo")
data class LichSuChayBo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quangDuongKm: Float,
    val thoiGianGiay: Long,
    val tocDoTrungBinh: Float,
    val thoiGianGhiNhan: Long
)
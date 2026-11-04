package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lich_su_can_nang")
data class LichSuCanNang(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val canNangKg: Float,
    val thoiGianGhiNhan: Long
)
package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phuong_an_du_phong")
data class PhuongAnDuPhong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val thoiTiet: String,
    val noiDung: String,
    val thoiGianTao: Long
)
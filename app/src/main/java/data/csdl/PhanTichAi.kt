package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phan_tich_ai")
data class PhanTichAi(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noiDungPhanTich: String,
    val thoiGianPhanTich: Long
)
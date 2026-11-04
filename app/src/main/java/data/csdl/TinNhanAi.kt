package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tin_nhan_ai")
data class TinNhanAi(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noiDung: String,
    val laCuaNguoiDung: Boolean,
    val thoiGianGuiTin: Long
)
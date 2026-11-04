package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bua_an")
data class BuaAn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tenMon: String,
    val loaiBua: String, // "Sáng", "Trưa", "Tối", "Phụ"
    val calo: Int,
    val thoiGianGhiNhan: Long
)
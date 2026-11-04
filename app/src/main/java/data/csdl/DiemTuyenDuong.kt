package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diem_tuyen_duong")
data class DiemTuyenDuong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phienId: Long,
    val vido: Double,
    val kinhdo: Double
)
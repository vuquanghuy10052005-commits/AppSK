package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nuoc_uong")
data class NuocUong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val soMl: Int,
    val thoiGianGhiNhan: Long
)
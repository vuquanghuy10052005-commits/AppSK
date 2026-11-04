package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ke_hoach_ai")
data class KeHoachAi(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noiDung: String,
    val thoiGianTao: Long
)
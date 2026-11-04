package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mon_ke_hoach")
data class MonTrongKeHoach(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keHoachId: Long,
    val thu: Int, // theo Calendar.DAY_OF_WEEK: Chủ nhật = 1, Thứ 2 = 2, ... Thứ 7 = 7
    val loaiBua: String,
    val tenMon: String,
    val calo: Int
)
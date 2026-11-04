package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buoc_chan_ngay")
data class BuocChanNgay(
    @PrimaryKey
    val ngay: String, // dạng yyyy-MM-dd
    val soBuoc: Int,
    val thoiGianCapNhat: Long
)
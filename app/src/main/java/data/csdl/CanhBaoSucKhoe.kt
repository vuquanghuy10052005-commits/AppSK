package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canh_bao_suc_khoe")
data class CanhBaoSucKhoe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val khoa: String,
    val loai: String,
    val mucDo: Int,
    val tieuDe: String,
    val noiDung: String,
    val thoiGianTao: Long
)
package com.example.appsk.data.csdl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phan_tich_anh")
data class PhanTichAnh(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val thoiGian: Long,
    val loaiBua: String,
    val tongCalo: Int,
    val phanLoai: String,   // PHU_HOP, CAN_NHAC, HAN_CHE hoặc CHUA_DANH_GIA
    val monJson: String,    // các món đã được người dùng xác nhận (JSON)
    val nhanXet: String     // lý do đánh giá và gợi ý thay thế
)
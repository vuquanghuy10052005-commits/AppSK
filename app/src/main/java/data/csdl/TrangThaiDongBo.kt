package com.example.appsk.data.csdl

import androidx.room.Entity

/**
 * Ghi nhớ dòng dữ liệu nào đã được đồng bộ: mã dòng trên máy, mã tài liệu trên đám mây,
 * và dấu vân (hash) nội dung tại lần đồng bộ cuối để biết dòng nào đã bị sửa.
 */
@Entity(tableName = "trang_thai_dong_bo", primaryKeys = ["bang", "idCucBo"])
data class TrangThaiDongBo(
    val bang: String,
    val idCucBo: Long,
    val syncId: String,
    val dauVan: String
)
package com.example.appsk.dongbo

import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.PhanTichAnh

object LoaiPhanTichAnh : LoaiDongBo("phan_tich_anh") {

    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.phanTichAnhDao().layTatCa().map {
            BanGhiCucBo(
                it.id,
                mapOf(
                    "thoiGian" to it.thoiGian,
                    "loaiBua" to it.loaiBua,
                    "tongCalo" to it.tongCalo,
                    "phanLoai" to it.phanLoai,
                    "monJson" to it.monJson,
                    "nhanXet" to it.nhanXet
                )
            )
        }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.phanTichAnhDao().them(
            PhanTichAnh(
                thoiGian = (truong["thoiGian"] as? Number)?.toLong() ?: 0L,
                loaiBua = truong["loaiBua"] as? String ?: "",
                tongCalo = (truong["tongCalo"] as? Number)?.toInt() ?: 0,
                phanLoai = truong["phanLoai"] as? String ?: "",
                monJson = truong["monJson"] as? String ?: "[]",
                nhanXet = truong["nhanXet"] as? String ?: ""
            )
        )

    override suspend fun xoa(db: AppDatabase, id: Long) = db.phanTichAnhDao().xoa(id)
}
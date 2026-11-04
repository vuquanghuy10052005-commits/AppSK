package com.example.appsk.dongbo

import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.BuaAn
import com.example.appsk.data.csdl.CanhBaoSucKhoe
import com.example.appsk.data.csdl.ChiTietBuoiTap
import com.example.appsk.data.csdl.KeHoachAi
import com.example.appsk.data.csdl.LichSuCanNang
import com.example.appsk.data.csdl.LichSuChayBo
import com.example.appsk.data.csdl.MonTrongKeHoach
import com.example.appsk.data.csdl.NuocUong
import com.example.appsk.data.csdl.PhanTichAi
import com.example.appsk.data.csdl.PhuongAnDuPhong
import com.example.appsk.data.csdl.TinNhanAi

private fun Map<String, Any?>.so(khoa: String): Number = this[khoa] as? Number ?: 0
private fun Map<String, Any?>.chuoi(khoa: String): String = this[khoa] as? String ?: ""

object CacLoaiDongBo {
    val TAT_CA: List<LoaiDongBo> = listOf(
        LoaiCanNang, LoaiChayBo, LoaiBuaAn, LoaiNuocUong, LoaiTinNhan,
        LoaiPhanTich, LoaiKeHoach, LoaiPhuongAn, LoaiCanhBao, LoaiPhanTichAnh
    )
}

object LoaiCanNang : LoaiDongBo("can_nang") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.dongBoDao().tatCaCanNang().map {
            BanGhiCucBo(
                it.id,
                mapOf("canNangKg" to it.canNangKg.toDouble(), "thoiGianGhiNhan" to it.thoiGianGhiNhan)
            )
        }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.dongBoDao().themCanNang(
            LichSuCanNang(
                canNangKg = truong.so("canNangKg").toFloat(),
                thoiGianGhiNhan = truong.so("thoiGianGhiNhan").toLong()
            )
        )

    override suspend fun xoa(db: AppDatabase, id: Long) = db.dongBoDao().xoaCanNang(id)
}

/** Buổi tập: kèm loại hoạt động. Tuyến đường GPS không được đồng bộ. */
object LoaiChayBo : LoaiDongBo("chay_bo") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> {
        val loaiTheoPhien = db.chiTietBuoiTapDao().layTatCa().associate { it.phienId to it.loaiHoatDong }
        return db.dongBoDao().tatCaChayBo().map { r ->
            val truong = mutableMapOf<String, Any?>(
                "quangDuongKm" to r.quangDuongKm.toDouble(),
                "thoiGianGiay" to r.thoiGianGiay,
                "tocDoTrungBinh" to r.tocDoTrungBinh.toDouble(),
                "thoiGianGhiNhan" to r.thoiGianGhiNhan
            )
            loaiTheoPhien[r.id]?.let { truong["loai"] = it }
            BanGhiCucBo(r.id, truong)
        }
    }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long {
        val id = db.dongBoDao().themChayBo(
            LichSuChayBo(
                quangDuongKm = truong.so("quangDuongKm").toFloat(),
                thoiGianGiay = truong.so("thoiGianGiay").toLong(),
                tocDoTrungBinh = truong.so("tocDoTrungBinh").toFloat(),
                thoiGianGhiNhan = truong.so("thoiGianGhiNhan").toLong()
            )
        )
        (truong["loai"] as? String)?.let { db.chiTietBuoiTapDao().luu(ChiTietBuoiTap(id, it)) }
        return id
    }

    override suspend fun xoa(db: AppDatabase, id: Long) {
        db.diemTuyenDuongDao().xoaTheoPhien(id)
        db.chiTietBuoiTapDao().xoa(id)
        db.dongBoDao().xoaChayBo(id)
    }
}

/** Bữa ăn là loại duy nhất có thể bị sửa (sửa tay, thay món) nên cho phép cập nhật. */
object LoaiBuaAn : LoaiDongBo("bua_an", choPhepCapNhat = true) {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.dongBoDao().tatCaBuaAn().map {
            BanGhiCucBo(
                it.id,
                mapOf(
                    "tenMon" to it.tenMon,
                    "loaiBua" to it.loaiBua,
                    "calo" to it.calo,
                    "thoiGianGhiNhan" to it.thoiGianGhiNhan
                )
            )
        }

    private fun tuTruong(id: Long, truong: Map<String, Any?>) = BuaAn(
        id = id,
        tenMon = truong.chuoi("tenMon"),
        loaiBua = truong.chuoi("loaiBua"),
        calo = truong.so("calo").toInt(),
        thoiGianGhiNhan = truong.so("thoiGianGhiNhan").toLong()
    )

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.dongBoDao().themBuaAn(tuTruong(0, truong))

    override suspend fun capNhat(db: AppDatabase, id: Long, truong: Map<String, Any?>) =
        db.dongBoDao().capNhatBuaAn(tuTruong(id, truong))

    override suspend fun xoa(db: AppDatabase, id: Long) = db.dongBoDao().xoaBuaAn(id)
}

object LoaiNuocUong : LoaiDongBo("nuoc_uong") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.dongBoDao().tatCaNuocUong().map {
            BanGhiCucBo(it.id, mapOf("soMl" to it.soMl, "thoiGianGhiNhan" to it.thoiGianGhiNhan))
        }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.dongBoDao().themNuocUong(
            NuocUong(
                soMl = truong.so("soMl").toInt(),
                thoiGianGhiNhan = truong.so("thoiGianGhiNhan").toLong()
            )
        )

    override suspend fun xoa(db: AppDatabase, id: Long) = db.dongBoDao().xoaNuocUong(id)
}

object LoaiTinNhan : LoaiDongBo("tin_nhan_ai") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.dongBoDao().tatCaTinNhan().map {
            BanGhiCucBo(
                it.id,
                mapOf(
                    "noiDung" to it.noiDung,
                    "laCuaNguoiDung" to it.laCuaNguoiDung,
                    "thoiGianGuiTin" to it.thoiGianGuiTin
                )
            )
        }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.dongBoDao().themTinNhan(
            TinNhanAi(
                noiDung = truong.chuoi("noiDung"),
                laCuaNguoiDung = truong["laCuaNguoiDung"] as? Boolean ?: false,
                thoiGianGuiTin = truong.so("thoiGianGuiTin").toLong()
            )
        )

    override suspend fun xoa(db: AppDatabase, id: Long) = db.dongBoDao().xoaTinNhan(id)
}

object LoaiPhanTich : LoaiDongBo("phan_tich_ai") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.dongBoDao().tatCaPhanTich().map {
            BanGhiCucBo(
                it.id,
                mapOf(
                    "noiDungPhanTich" to it.noiDungPhanTich,
                    "thoiGianPhanTich" to it.thoiGianPhanTich
                )
            )
        }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.dongBoDao().themPhanTich(
            PhanTichAi(
                noiDungPhanTich = truong.chuoi("noiDungPhanTich"),
                thoiGianPhanTich = truong.so("thoiGianPhanTich").toLong()
            )
        )

    override suspend fun xoa(db: AppDatabase, id: Long) = db.dongBoDao().xoaPhanTich(id)
}

/** Kế hoạch: các món ăn trong kế hoạch được gói chung vào cùng một tài liệu. */
object LoaiKeHoach : LoaiDongBo("ke_hoach_ai") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> {
        val monTheoKeHoach = db.dongBoDao().tatCaMonKeHoach().groupBy { it.keHoachId }
        return db.dongBoDao().tatCaKeHoach().map { keHoach ->
            val mon = (monTheoKeHoach[keHoach.id] ?: emptyList())
                .sortedBy { it.id }
                .map {
                    mapOf(
                        "thu" to it.thu,
                        "loaiBua" to it.loaiBua,
                        "tenMon" to it.tenMon,
                        "calo" to it.calo
                    )
                }
            BanGhiCucBo(
                keHoach.id,
                mapOf(
                    "noiDung" to keHoach.noiDung,
                    "thoiGianTao" to keHoach.thoiGianTao,
                    "mon" to mon
                )
            )
        }
    }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long {
        val id = db.dongBoDao().themKeHoach(
            KeHoachAi(
                noiDung = truong.chuoi("noiDung"),
                thoiGianTao = truong.so("thoiGianTao").toLong()
            )
        )
        val mon = (truong["mon"] as? List<*>).orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .map { m ->
                MonTrongKeHoach(
                    keHoachId = id,
                    thu = (m["thu"] as? Number)?.toInt() ?: 0,
                    loaiBua = m["loaiBua"] as? String ?: "",
                    tenMon = m["tenMon"] as? String ?: "",
                    calo = (m["calo"] as? Number)?.toInt() ?: 0
                )
            }
        if (mon.isNotEmpty()) db.monTrongKeHoachDao().themNhieu(mon)
        return id
    }

    override suspend fun xoa(db: AppDatabase, id: Long) {
        db.dongBoDao().xoaMonTheoKeHoach(id)
        db.dongBoDao().xoaKeHoach(id)
    }
}

object LoaiPhuongAn : LoaiDongBo("phuong_an") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.dongBoDao().tatCaPhuongAn().map {
            BanGhiCucBo(
                it.id,
                mapOf(
                    "thoiTiet" to it.thoiTiet,
                    "noiDung" to it.noiDung,
                    "thoiGianTao" to it.thoiGianTao
                )
            )
        }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.dongBoDao().themPhuongAn(
            PhuongAnDuPhong(
                thoiTiet = truong.chuoi("thoiTiet"),
                noiDung = truong.chuoi("noiDung"),
                thoiGianTao = truong.so("thoiGianTao").toLong()
            )
        )

    override suspend fun xoa(db: AppDatabase, id: Long) = db.dongBoDao().xoaPhuongAn(id)
}

object LoaiCanhBao : LoaiDongBo("canh_bao") {
    override suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo> =
        db.dongBoDao().tatCaCanhBao().map {
            BanGhiCucBo(
                it.id,
                mapOf(
                    "khoa" to it.khoa,
                    "loai" to it.loai,
                    "mucDo" to it.mucDo,
                    "tieuDe" to it.tieuDe,
                    "noiDung" to it.noiDung,
                    "thoiGianTao" to it.thoiGianTao
                )
            )
        }

    override suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long =
        db.dongBoDao().themCanhBao(
            CanhBaoSucKhoe(
                khoa = truong.chuoi("khoa"),
                loai = truong.chuoi("loai"),
                mucDo = truong.so("mucDo").toInt(),
                tieuDe = truong.chuoi("tieuDe"),
                noiDung = truong.chuoi("noiDung"),
                thoiGianTao = truong.so("thoiGianTao").toLong()
            )
        )

    override suspend fun xoa(db: AppDatabase, id: Long) = db.dongBoDao().xoaCanhBao(id)
}
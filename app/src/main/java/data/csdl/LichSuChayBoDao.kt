package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LichSuChayBoDao {

    @Insert
    suspend fun themBanGhi(banGhi: LichSuChayBo): Long

    @Query("SELECT * FROM lich_su_chay_bo ORDER BY thoiGianGhiNhan DESC")
    suspend fun layTatCa(): List<LichSuChayBo>

    @Query("SELECT * FROM lich_su_chay_bo WHERE id = :id")
    suspend fun layTheoId(id: Long): LichSuChayBo?

    @Query("DELETE FROM lich_su_chay_bo WHERE id = :id")
    suspend fun xoaTheoId(id: Long)

    @Query("SELECT * FROM lich_su_chay_bo WHERE thoiGianGhiNhan >= :tuThoiGian ORDER BY thoiGianGhiNhan ASC")
    suspend fun layTheoKhoangThoiGian(tuThoiGian: Long): List<LichSuChayBo>

    @Query("SELECT SUM(quangDuongKm) FROM lich_su_chay_bo WHERE thoiGianGhiNhan >= :tuThoiGian")
    suspend fun tinhTongQuangDuongTu(tuThoiGian: Long): Float?

    @Query("SELECT COUNT(*) FROM lich_su_chay_bo WHERE thoiGianGhiNhan >= :tuThoiGian")
    suspend fun demSoBuoiTu(tuThoiGian: Long): Int
}
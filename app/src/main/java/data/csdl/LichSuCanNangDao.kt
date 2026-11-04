package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LichSuCanNangDao {

    @Insert
    suspend fun themBanGhi(banGhi: LichSuCanNang)

    @Query("SELECT * FROM lich_su_can_nang ORDER BY thoiGianGhiNhan ASC")
    suspend fun layTatCa(): List<LichSuCanNang>

    @Query("SELECT * FROM lich_su_can_nang WHERE thoiGianGhiNhan >= :tuThoiGian ORDER BY thoiGianGhiNhan ASC")
    suspend fun layTheoKhoangThoiGian(tuThoiGian: Long): List<LichSuCanNang>

    @Query("SELECT * FROM lich_su_can_nang ORDER BY thoiGianGhiNhan DESC LIMIT 1")
    suspend fun layBanGhiMoiNhat(): LichSuCanNang?
}
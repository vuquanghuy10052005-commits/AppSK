package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NuocUongDao {

    @Insert
    suspend fun themNuocUong(nuocUong: NuocUong)

    @Query("SELECT * FROM nuoc_uong WHERE thoiGianGhiNhan >= :tuThoiGian AND thoiGianGhiNhan < :denThoiGian ORDER BY thoiGianGhiNhan DESC, id DESC")
    suspend fun layTrongNgay(tuThoiGian: Long, denThoiGian: Long): List<NuocUong>

    @Query("SELECT SUM(soMl) FROM nuoc_uong WHERE thoiGianGhiNhan >= :tuThoiGian AND thoiGianGhiNhan < :denThoiGian")
    suspend fun tinhTongNuocTrongNgay(tuThoiGian: Long, denThoiGian: Long): Int?

    @Query("DELETE FROM nuoc_uong WHERE id = :id")
    suspend fun xoaNuocUong(id: Long)
}
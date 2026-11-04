package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BuaAnDao {

    @Insert
    suspend fun themBuaAn(buaAn: BuaAn)

    @Update
    suspend fun capNhatBuaAn(buaAn: BuaAn)

    @Query("SELECT * FROM bua_an WHERE thoiGianGhiNhan >= :tuThoiGian AND thoiGianGhiNhan < :denThoiGian ORDER BY thoiGianGhiNhan DESC")
    suspend fun layBuaAnTrongNgay(tuThoiGian: Long, denThoiGian: Long): List<BuaAn>

    @Query("SELECT SUM(calo) FROM bua_an WHERE thoiGianGhiNhan >= :tuThoiGian AND thoiGianGhiNhan < :denThoiGian")
    suspend fun tinhTongCaloTrongNgay(tuThoiGian: Long, denThoiGian: Long): Int?

    @Query("DELETE FROM bua_an WHERE id = :id")
    suspend fun xoaBuaAn(id: Long)
}
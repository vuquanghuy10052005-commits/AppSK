package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChiTietBuoiTapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luu(chiTiet: ChiTietBuoiTap)

    @Query("SELECT * FROM chi_tiet_buoi_tap")
    suspend fun layTatCa(): List<ChiTietBuoiTap>

    @Query("SELECT loaiHoatDong FROM chi_tiet_buoi_tap WHERE phienId = :phienId")
    suspend fun layLoai(phienId: Long): String?

    @Query("DELETE FROM chi_tiet_buoi_tap WHERE phienId = :phienId")
    suspend fun xoa(phienId: Long)
}
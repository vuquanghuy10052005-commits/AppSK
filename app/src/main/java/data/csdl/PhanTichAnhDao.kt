package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PhanTichAnhDao {

    @Insert
    suspend fun them(x: PhanTichAnh): Long

    @Query("SELECT * FROM phan_tich_anh ORDER BY thoiGian DESC")
    suspend fun layTatCa(): List<PhanTichAnh>

    @Query("DELETE FROM phan_tich_anh WHERE id = :id")
    suspend fun xoa(id: Long)
}
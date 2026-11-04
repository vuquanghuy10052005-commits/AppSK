package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PhanTichAiDao {

    @Insert
    suspend fun themPhanTich(phanTich: PhanTichAi)

    @Query("SELECT * FROM phan_tich_ai ORDER BY thoiGianPhanTich DESC")
    suspend fun layTatCa(): List<PhanTichAi>

    @Query("SELECT * FROM phan_tich_ai ORDER BY thoiGianPhanTich DESC LIMIT 1")
    suspend fun layMoiNhat(): PhanTichAi?
}
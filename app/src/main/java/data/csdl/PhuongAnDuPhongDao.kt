package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PhuongAnDuPhongDao {

    @Insert
    suspend fun themPhuongAn(phuongAn: PhuongAnDuPhong)

    @Query("SELECT * FROM phuong_an_du_phong ORDER BY thoiGianTao DESC")
    suspend fun layTatCa(): List<PhuongAnDuPhong>
}
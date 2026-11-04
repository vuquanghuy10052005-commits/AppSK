package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TinNhanAiDao {

    @Insert
    suspend fun themTinNhan(tinNhan: TinNhanAi)

    @Query("SELECT * FROM tin_nhan_ai ORDER BY thoiGianGuiTin ASC")
    suspend fun layTatCaTinNhan(): List<TinNhanAi>

    @Query("DELETE FROM tin_nhan_ai")
    suspend fun xoaToanBoLichSu()
}
package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface KeHoachAiDao {

    @Insert
    suspend fun themKeHoach(keHoach: KeHoachAi): Long

    @Query("SELECT * FROM ke_hoach_ai ORDER BY thoiGianTao DESC")
    suspend fun layTatCa(): List<KeHoachAi>
}
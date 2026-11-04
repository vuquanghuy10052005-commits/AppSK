package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CanhBaoSucKhoeDao {

    @Insert
    suspend fun themCanhBao(canhBao: CanhBaoSucKhoe)

    @Query("SELECT * FROM canh_bao_suc_khoe ORDER BY thoiGianTao DESC, mucDo DESC")
    suspend fun layTatCa(): List<CanhBaoSucKhoe>

    @Query(
        "SELECT COUNT(*) FROM canh_bao_suc_khoe " +
                "WHERE khoa = :khoa AND mucDo = :mucDo AND thoiGianTao >= :tuThoiGian"
    )
    suspend fun demGanDay(khoa: String, mucDo: Int, tuThoiGian: Long): Int
}
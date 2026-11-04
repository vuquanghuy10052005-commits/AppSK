package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DiemTuyenDuongDao {

    @Insert
    suspend fun themNhieuDiem(danhSach: List<DiemTuyenDuong>)

    @Query("SELECT * FROM diem_tuyen_duong WHERE phienId = :phienId ORDER BY id ASC")
    suspend fun layTheoPhien(phienId: Long): List<DiemTuyenDuong>

    @Query("DELETE FROM diem_tuyen_duong WHERE phienId = :phienId")
    suspend fun xoaTheoPhien(phienId: Long)
}
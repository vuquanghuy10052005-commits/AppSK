package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BuocChanNgayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuSoBuoc(buocChan: BuocChanNgay)

    @Query("SELECT soBuoc FROM buoc_chan_ngay WHERE ngay = :ngay")
    suspend fun laySoBuoc(ngay: String): Int?

    @Query("SELECT * FROM buoc_chan_ngay WHERE ngay >= :tuNgay ORDER BY ngay ASC")
    suspend fun layTuNgay(tuNgay: String): List<BuocChanNgay>
}
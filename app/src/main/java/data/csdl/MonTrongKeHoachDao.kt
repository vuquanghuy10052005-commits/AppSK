package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MonTrongKeHoachDao {

    @Insert
    suspend fun themNhieu(danhSach: List<MonTrongKeHoach>)

    /** Các món của kế hoạch MỚI NHẤT, theo thứ trong tuần. */
    @Query(
        "SELECT * FROM mon_ke_hoach " +
                "WHERE keHoachId = (SELECT id FROM ke_hoach_ai ORDER BY thoiGianTao DESC, id DESC LIMIT 1) " +
                "AND thu = :thu ORDER BY id ASC"
    )
    suspend fun layCuaKeHoachMoiNhat(thu: Int): List<MonTrongKeHoach>
}
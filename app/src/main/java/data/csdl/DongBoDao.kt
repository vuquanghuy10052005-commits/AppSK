package com.example.appsk.data.csdl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DongBoDao {

    // ----- Bảng trạng thái đồng bộ -----
    @Query("SELECT * FROM trang_thai_dong_bo WHERE bang = :bang")
    suspend fun layTrangThai(bang: String): List<TrangThaiDongBo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuTrangThai(trangThai: TrangThaiDongBo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuNhieuTrangThai(danhSach: List<TrangThaiDongBo>)

    @Query("DELETE FROM trang_thai_dong_bo WHERE bang = :bang AND idCucBo = :idCucBo")
    suspend fun xoaTrangThai(bang: String, idCucBo: Long)

    @Query("DELETE FROM trang_thai_dong_bo")
    suspend fun xoaTatCaTrangThai()

    // ----- Cân nặng -----
    @Query("SELECT * FROM lich_su_can_nang")
    suspend fun tatCaCanNang(): List<LichSuCanNang>

    @Insert
    suspend fun themCanNang(x: LichSuCanNang): Long

    @Query("DELETE FROM lich_su_can_nang WHERE id = :id")
    suspend fun xoaCanNang(id: Long)

    // ----- Buổi tập -----
    @Query("SELECT * FROM lich_su_chay_bo")
    suspend fun tatCaChayBo(): List<LichSuChayBo>

    @Insert
    suspend fun themChayBo(x: LichSuChayBo): Long

    @Query("DELETE FROM lich_su_chay_bo WHERE id = :id")
    suspend fun xoaChayBo(id: Long)

    // ----- Bữa ăn -----
    @Query("SELECT * FROM bua_an")
    suspend fun tatCaBuaAn(): List<BuaAn>

    @Insert
    suspend fun themBuaAn(x: BuaAn): Long

    @Update
    suspend fun capNhatBuaAn(x: BuaAn)

    @Query("DELETE FROM bua_an WHERE id = :id")
    suspend fun xoaBuaAn(id: Long)

    // ----- Nước uống -----
    @Query("SELECT * FROM nuoc_uong")
    suspend fun tatCaNuocUong(): List<NuocUong>

    @Insert
    suspend fun themNuocUong(x: NuocUong): Long

    @Query("DELETE FROM nuoc_uong WHERE id = :id")
    suspend fun xoaNuocUong(id: Long)

    // ----- Chat AI -----
    @Query("SELECT * FROM tin_nhan_ai")
    suspend fun tatCaTinNhan(): List<TinNhanAi>

    @Insert
    suspend fun themTinNhan(x: TinNhanAi): Long

    @Query("DELETE FROM tin_nhan_ai WHERE id = :id")
    suspend fun xoaTinNhan(id: Long)

    // ----- Phân tích AI -----
    @Query("SELECT * FROM phan_tich_ai")
    suspend fun tatCaPhanTich(): List<PhanTichAi>

    @Insert
    suspend fun themPhanTich(x: PhanTichAi): Long

    @Query("DELETE FROM phan_tich_ai WHERE id = :id")
    suspend fun xoaPhanTich(id: Long)

    // ----- Kế hoạch AI và các món trong kế hoạch -----
    @Query("SELECT * FROM ke_hoach_ai")
    suspend fun tatCaKeHoach(): List<KeHoachAi>

    @Insert
    suspend fun themKeHoach(x: KeHoachAi): Long

    @Query("DELETE FROM ke_hoach_ai WHERE id = :id")
    suspend fun xoaKeHoach(id: Long)

    @Query("SELECT * FROM mon_ke_hoach")
    suspend fun tatCaMonKeHoach(): List<MonTrongKeHoach>

    @Query("DELETE FROM mon_ke_hoach WHERE keHoachId = :keHoachId")
    suspend fun xoaMonTheoKeHoach(keHoachId: Long)

    // ----- Phương án dự phòng -----
    @Query("SELECT * FROM phuong_an_du_phong")
    suspend fun tatCaPhuongAn(): List<PhuongAnDuPhong>

    @Insert
    suspend fun themPhuongAn(x: PhuongAnDuPhong): Long

    @Query("DELETE FROM phuong_an_du_phong WHERE id = :id")
    suspend fun xoaPhuongAn(id: Long)

    // ----- Cảnh báo -----
    @Query("SELECT * FROM canh_bao_suc_khoe")
    suspend fun tatCaCanhBao(): List<CanhBaoSucKhoe>

    @Insert
    suspend fun themCanhBao(x: CanhBaoSucKhoe): Long

    @Query("DELETE FROM canh_bao_suc_khoe WHERE id = :id")
    suspend fun xoaCanhBao(id: Long)
}
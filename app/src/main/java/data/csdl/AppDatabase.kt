package com.example.appsk.data.csdl

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appsk.taikhoan.TaiKhoan
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Database(
    entities = [
        LichSuCanNang::class,
        LichSuChayBo::class,
        BuaAn::class,
        NuocUong::class,
        TinNhanAi::class,
        PhanTichAi::class,
        KeHoachAi::class,
        PhuongAnDuPhong::class,
        CanhBaoSucKhoe::class,
        BuocChanNgay::class,
        ChiTietBuoiTap::class,
        DiemTuyenDuong::class,
        MonTrongKeHoach::class,
        TrangThaiDongBo::class,
        PhanTichAnh::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lichSuCanNangDao(): LichSuCanNangDao
    abstract fun lichSuChayBoDao(): LichSuChayBoDao
    abstract fun buaAnDao(): BuaAnDao
    abstract fun nuocUongDao(): NuocUongDao
    abstract fun tinNhanAiDao(): TinNhanAiDao
    abstract fun phanTichAiDao(): PhanTichAiDao
    abstract fun keHoachAiDao(): KeHoachAiDao
    abstract fun phuongAnDuPhongDao(): PhuongAnDuPhongDao
    abstract fun canhBaoSucKhoeDao(): CanhBaoSucKhoeDao
    abstract fun buocChanNgayDao(): BuocChanNgayDao
    abstract fun chiTietBuoiTapDao(): ChiTietBuoiTapDao
    abstract fun diemTuyenDuongDao(): DiemTuyenDuongDao
    abstract fun monTrongKeHoachDao(): MonTrongKeHoachDao
    abstract fun dongBoDao(): DongBoDao
    abstract fun phanTichAnhDao(): PhanTichAnhDao

    companion object {
        private const val TEN_DB_CU = "suckhoeai_database"

        // Mỗi tài khoản một database riêng
        private val cacDb = ConcurrentHashMap<String, AppDatabase>()

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ke_hoach_ai` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`noiDung` TEXT NOT NULL, " +
                            "`thoiGianTao` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `phuong_an_du_phong` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`thoiTiet` TEXT NOT NULL, " +
                            "`noiDung` TEXT NOT NULL, " +
                            "`thoiGianTao` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `canh_bao_suc_khoe` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`khoa` TEXT NOT NULL, " +
                            "`loai` TEXT NOT NULL, " +
                            "`mucDo` INTEGER NOT NULL, " +
                            "`tieuDe` TEXT NOT NULL, " +
                            "`noiDung` TEXT NOT NULL, " +
                            "`thoiGianTao` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `buoc_chan_ngay` (" +
                            "`ngay` TEXT NOT NULL, " +
                            "`soBuoc` INTEGER NOT NULL, " +
                            "`thoiGianCapNhat` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`ngay`))"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chi_tiet_buoi_tap` (" +
                            "`phienId` INTEGER NOT NULL, " +
                            "`loaiHoatDong` TEXT NOT NULL, " +
                            "PRIMARY KEY(`phienId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `diem_tuyen_duong` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`phienId` INTEGER NOT NULL, " +
                            "`vido` REAL NOT NULL, " +
                            "`kinhdo` REAL NOT NULL)"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mon_ke_hoach` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`keHoachId` INTEGER NOT NULL, " +
                            "`thu` INTEGER NOT NULL, " +
                            "`loaiBua` TEXT NOT NULL, " +
                            "`tenMon` TEXT NOT NULL, " +
                            "`calo` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trang_thai_dong_bo` (" +
                            "`bang` TEXT NOT NULL, " +
                            "`idCucBo` INTEGER NOT NULL, " +
                            "`syncId` TEXT NOT NULL, " +
                            "`dauVan` TEXT NOT NULL, " +
                            "PRIMARY KEY(`bang`, `idCucBo`))"
                )
            }
        }

        // Chỉ thêm bảng mới, dữ liệu cũ nguyên vẹn
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `phan_tich_anh` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`thoiGian` INTEGER NOT NULL, " +
                            "`loaiBua` TEXT NOT NULL, " +
                            "`tongCalo` INTEGER NOT NULL, " +
                            "`phanLoai` TEXT NOT NULL, " +
                            "`monJson` TEXT NOT NULL, " +
                            "`nhanXet` TEXT NOT NULL)"
                )
            }
        }

        /** Database của tài khoản đang đăng nhập. Các màn hình cứ gọi hàm này như trước. */
        fun layInstance(context: Context): AppDatabase = layChoUid(context, TaiKhoan.uid())

        fun layChoUid(context: Context, uid: String): AppDatabase {
            cacDb[uid]?.let { return it }
            return synchronized(this) {
                cacDb[uid] ?: taoDb(context.applicationContext, uid).also { cacDb[uid] = it }
            }
        }

        private fun tenFileDb(uid: String): String = "suckhoeai_$uid"

        private fun taoDb(appContext: Context, uid: String): AppDatabase {
            val tenFile = tenFileDb(uid)
            nhanDuLieuCu(appContext, uid, tenFile)

            return Room.databaseBuilder(appContext, AppDatabase::class.java, tenFile)
                .addMigrations(
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
                    MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12
                )
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Database cũ (trước khi tách theo tài khoản) được chuyển cho tài khoản đầu tiên
         * mở database sau khi cập nhật. Chỉ chuyển một lần.
         */
        private fun nhanDuLieuCu(appContext: Context, uid: String, tenFileMoi: String) {
            if (uid == TaiKhoan.KHACH) return

            val chung = appContext.getSharedPreferences("tai_khoan_chung", Context.MODE_PRIVATE)
            if (chung.getBoolean("da_nhan_db_cu", false)) return

            val fileCu = appContext.getDatabasePath(TEN_DB_CU)
            val fileMoi = appContext.getDatabasePath(tenFileMoi)

            var thanhCong = true
            if (fileCu.exists() && !fileMoi.exists()) {
                for (hauTo in listOf("", "-wal", "-shm", "-journal")) {
                    val nguon = File(fileCu.path + hauTo)
                    if (nguon.exists() && !nguon.renameTo(File(fileMoi.path + hauTo))) {
                        thanhCong = false
                    }
                }
            }
            if (thanhCong) chung.edit().putBoolean("da_nhan_db_cu", true).apply()
        }
    }
}
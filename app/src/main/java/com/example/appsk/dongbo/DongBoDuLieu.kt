package com.example.appsk.dongbo

import android.content.Context
import androidx.room.withTransaction
import com.example.appsk.buocchan.GhiNhanBuocChan
import com.example.appsk.data.HoSoSucKhoe
import com.example.appsk.data.QuanLyBuocChan
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.BuocChanNgay
import com.example.appsk.data.csdl.TrangThaiDongBo
import com.example.appsk.nhacnho.QuanLyNhacNho
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.UUID

/**
 * Đồng bộ hai chiều giữa database trên máy (Room) và Firestore, theo từng tài khoản:
 *
 *  - Mỗi dòng dữ liệu có một tài liệu trên đám mây với mã ngẫu nhiên (syncId).
 *  - Bảng trang_thai_dong_bo nhớ "dòng nào đã đồng bộ, nội dung lúc đó ra sao"
 *    nên biết được dòng nào mới, đã sửa hay đã xóa mà không cần sửa các bảng cũ.
 *  - Xóa được gửi lên dưới dạng tài liệu đánh dấu daXoa = true để máy khác xóa theo.
 *  - Kéo về chỉ lấy tài liệu mới hơn lần kéo trước (theo đồng hồ máy chủ).
 */
object DongBoDuLieu {

    private const val GOC = "nguoi_dung"
    private const val BANG_BUOC_CHAN = "buoc_chan"
    private const val LE_AN_TOAN_MS = 2 * 60 * 1000L
    private const val KICH_THUOC_LO = 400
    private const val THOI_GIAN_TOI_DA_MS = 120_000L

    class KetQua(val thanhCong: Boolean, val thongBao: String, val canThuLai: Boolean = false)

    private class DemSo {
        var dayLen = 0
        var keoVe = 0
    }

    private class ViecGhi(
        val idCucBo: Long,
        val syncId: String,
        val truong: Map<String, Any?>,
        val dauVan: String,
        val laXoa: Boolean,
        val laMoi: Boolean
    )

    // Không cho hai lần đồng bộ chạy chồng lên nhau
    private val khoa = Mutex()

    suspend fun dongBo(context: Context): KetQua = khoa.withLock {
        val appContext = context.applicationContext
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withLock KetQua(false, "Chưa đăng nhập")

        val ketQua = withTimeoutOrNull(THOI_GIAN_TOI_DA_MS) { thucHien(appContext, uid) }
            ?: KetQua(false, "Quá thời gian chờ, ứng dụng sẽ tự thử lại", canThuLai = true)

        if (FirebaseAuth.getInstance().currentUser?.uid == uid) {
            CaiDatDongBo.luuKetQua(appContext, ketQua.thanhCong, ketQua.thongBao)
        }
        ketQua
    }

    private suspend fun thucHien(ctx: Context, uid: String): KetQua {
        val fs = FirebaseFirestore.getInstance()
        val goc = fs.collection(GOC).document(uid)
        val db = AppDatabase.layChoUid(ctx, uid)
        val dem = DemSo()

        return try {
            // Bước đầu tiên đọc thẳng từ máy chủ nên cũng là phép thử kết nối và quyền truy cập
            dongBoCaiDat(ctx, goc, dem)

            for (loai in CacLoaiDongBo.TAT_CA) {
                kiemTraConDangNhap(uid)
                dongBoLoai(ctx, db, fs, goc, loai, dem)
            }

            kiemTraConDangNhap(uid)
            dongBoBuocChan(ctx, db, fs, goc, dem)

            val thongBao = if (dem.dayLen == 0 && dem.keoVe == 0) {
                "Đã đồng bộ, không có thay đổi mới"
            } else {
                "Đã đồng bộ (gửi ${dem.dayLen}, nhận ${dem.keoVe})"
            }
            KetQua(true, thongBao)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            moTaLoiFirestore(e)
        } catch (e: IllegalStateException) {
            KetQua(false, e.message ?: "Lỗi trạng thái")
        } catch (e: Exception) {
            KetQua(false, "Lỗi: ${e.message}", canThuLai = true)
        }
    }

    private fun kiemTraConDangNhap(uid: String) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid) {
            "Đã đổi tài khoản trong lúc đồng bộ"
        }
    }

    private fun moTaLoiFirestore(e: FirebaseFirestoreException): KetQua = when (e.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> KetQua(
            false,
            "Máy chủ từ chối truy cập (chưa tạo Firestore Database hoặc luật bảo mật chưa đúng)"
        )
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> KetQua(
            false, "Không kết nối được máy chủ, ứng dụng sẽ tự thử lại", canThuLai = true
        )
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> KetQua(
            false, "Phiên đăng nhập đã hết hạn, hãy đăng nhập lại"
        )
        else -> KetQua(false, "Lỗi máy chủ (${e.code})", canThuLai = true)
    }

    private fun mocTruyVan(moc: Long): Timestamp =
        Timestamp(Date(maxOf(0L, moc - LE_AN_TOAN_MS)))

    private fun layTruong(tai: DocumentSnapshot): Map<String, Any?> =
        (tai.data ?: emptyMap()).filterKeys { it != "capNhatLuc" && it != "daXoa" }

    // ================================================================ Các loại dữ liệu

    private suspend fun dongBoLoai(
        ctx: Context,
        db: AppDatabase,
        fs: FirebaseFirestore,
        goc: DocumentReference,
        loai: LoaiDongBo,
        dem: DemSo
    ) {
        val kho = goc.collection(loai.ten)
        val dao = db.dongBoDao()

        // ---------------- Kéo về ----------------
        val trangThaiTruoc = dao.layTrangThai(loai.ten)
        val idTheoSync = trangThaiTruoc.associate { it.syncId to it.idCucBo }.toMutableMap()
        val trangThaiTheoId = trangThaiTruoc.associateBy { it.idCucBo }.toMutableMap()
        val cucBoTheoId = loai.layTatCa(db).associateBy { it.id }.toMutableMap()

        val mocCu = CaiDatDongBo.layMocKeo(ctx, loai.ten)
        // Luôn đọc từ máy chủ: đọc từ bộ nhớ đệm có thể trống và bị hiểu nhầm là "đã bị xóa"
        val truyVan = kho.whereGreaterThan("capNhatLuc", mocTruyVan(mocCu))
            .get(Source.SERVER).await()
        var mocMoi = mocCu

        for (tai in truyVan.documents) {
            tai.getTimestamp("capNhatLuc")?.let { mocMoi = maxOf(mocMoi, it.toDate().time) }
            try {
                val syncId = tai.id
                val idCucBo = idTheoSync[syncId]

                if (tai.getBoolean("daXoa") == true) {
                    // Máy khác đã xóa dòng này
                    if (idCucBo != null) {
                        db.withTransaction {
                            loai.xoa(db, idCucBo)
                            dao.xoaTrangThai(loai.ten, idCucBo)
                        }
                        idTheoSync.remove(syncId)
                        trangThaiTheoId.remove(idCucBo)
                        cucBoTheoId.remove(idCucBo)
                        dem.keoVe++
                    }
                    continue
                }

                val truong = layTruong(tai)
                if (idCucBo == null) {
                    // Dòng mới từ máy khác: thêm vào và ghi trạng thái trong cùng một giao dịch
                    val dauVan = DauVan.tinh(truong)
                    val idMoi = db.withTransaction {
                        val id = loai.them(db, truong)
                        dao.luuTrangThai(TrangThaiDongBo(loai.ten, id, syncId, dauVan))
                        id
                    }
                    idTheoSync[syncId] = idMoi
                    trangThaiTheoId[idMoi] = TrangThaiDongBo(loai.ten, idMoi, syncId, dauVan)
                    cucBoTheoId[idMoi] = BanGhiCucBo(idMoi, truong)
                    dem.keoVe++
                } else if (loai.choPhepCapNhat) {
                    // Chỉ áp dụng bản sửa từ máy khác nếu dòng này chưa bị sửa trên máy này
                    val cucBo = cucBoTheoId[idCucBo]
                    val trangThai = trangThaiTheoId[idCucBo]
                    if (cucBo != null && trangThai != null) {
                        val dauVanMay = DauVan.tinh(truong)
                        val chuaSuaTrenMay = DauVan.tinh(cucBo.truong) == trangThai.dauVan
                        if (chuaSuaTrenMay && dauVanMay != trangThai.dauVan) {
                            db.withTransaction {
                                loai.capNhat(db, idCucBo, truong)
                                dao.luuTrangThai(TrangThaiDongBo(loai.ten, idCucBo, syncId, dauVanMay))
                            }
                            dem.keoVe++
                        }
                    }
                }
                // Dòng đã có trên máy nhưng bị xóa tại chỗ thì bỏ qua: lệnh xóa sẽ được gửi ở bước đẩy lên
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                throw e
            } catch (e: Exception) {
                // Tài liệu hỏng thì bỏ qua, không làm hỏng cả lần đồng bộ
            }
        }
        CaiDatDongBo.datMocKeo(ctx, loai.ten, mocMoi)

        // ---------------- Đẩy lên ----------------
        val cucBoMoi = loai.layTatCa(db)
        val trangThaiMoi = dao.layTrangThai(loai.ten).associateBy { it.idCucBo }
        val idConTon = cucBoMoi.map { it.id }.toSet()

        val viecGhi = ArrayList<ViecGhi>()
        for (r in cucBoMoi) {
            val dauVan = DauVan.tinh(r.truong)
            val trangThai = trangThaiMoi[r.id]
            if (trangThai == null || trangThai.dauVan != dauVan) {
                viecGhi.add(
                    ViecGhi(
                        idCucBo = r.id,
                        syncId = trangThai?.syncId ?: UUID.randomUUID().toString(),
                        truong = r.truong,
                        dauVan = dauVan,
                        laXoa = false,
                        laMoi = trangThai == null
                    )
                )
            }
        }
        for (trangThai in trangThaiMoi.values) {
            if (trangThai.idCucBo !in idConTon) {
                viecGhi.add(
                    ViecGhi(trangThai.idCucBo, trangThai.syncId, emptyMap(), "", laXoa = true, laMoi = false)
                )
            }
        }

        for (nhom in viecGhi.chunked(KICH_THUOC_LO)) {
            // Lưu trước mã đám mây của dòng mới: nếu bị ngắt giữa chừng, lần sau dùng lại đúng mã đó
            // nên không tạo bản sao trùng lặp trên đám mây
            val chuaCo = nhom.filter { it.laMoi }
                .map { TrangThaiDongBo(loai.ten, it.idCucBo, it.syncId, "") }
            if (chuaCo.isNotEmpty()) dao.luuNhieuTrangThai(chuaCo)

            val lo = fs.batch()
            for (viec in nhom) {
                val tham = kho.document(viec.syncId)
                if (viec.laXoa) {
                    lo.set(tham, mapOf("daXoa" to true, "capNhatLuc" to FieldValue.serverTimestamp()))
                } else {
                    val duLieu = HashMap<String, Any?>(viec.truong)
                    duLieu["daXoa"] = false
                    duLieu["capNhatLuc"] = FieldValue.serverTimestamp()
                    lo.set(tham, duLieu)
                }
            }
            lo.commit().await()

            db.withTransaction {
                for (viec in nhom) {
                    if (viec.laXoa) {
                        dao.xoaTrangThai(loai.ten, viec.idCucBo)
                    } else {
                        dao.luuTrangThai(
                            TrangThaiDongBo(loai.ten, viec.idCucBo, viec.syncId, viec.dauVan)
                        )
                    }
                }
            }
            dem.dayLen += nhom.size
        }
    }

    // ================================================================ Số bước chân

    /**
     * Mỗi ngày một tài liệu (mã là ngày yyyy-MM-dd). Hai máy cùng ngày thì lấy số lớn hơn,
     * vì cảm biến đếm riêng từng máy và thường chỉ mang theo một máy.
     */
    private suspend fun dongBoBuocChan(
        ctx: Context,
        db: AppDatabase,
        fs: FirebaseFirestore,
        goc: DocumentReference,
        dem: DemSo
    ) {
        val kho = goc.collection(BANG_BUOC_CHAN)
        val dao = db.buocChanNgayDao()
        val dongBoDao = db.dongBoDao()
        val bayGio = System.currentTimeMillis()

        // Kéo về
        val mocCu = CaiDatDongBo.layMocKeo(ctx, BANG_BUOC_CHAN)
        val truyVan = kho.whereGreaterThan("capNhatLuc", mocTruyVan(mocCu))
            .get(Source.SERVER).await()
        var mocMoi = mocCu
        for (tai in truyVan.documents) {
            tai.getTimestamp("capNhatLuc")?.let { mocMoi = maxOf(mocMoi, it.toDate().time) }
            val soBuocMay = (tai.getLong("soBuoc") ?: 0L).toInt()
            val soBuocCucBo = dao.laySoBuoc(tai.id) ?: 0
            if (soBuocMay > soBuocCucBo) {
                dao.luuSoBuoc(BuocChanNgay(tai.id, soBuocMay, bayGio))
                dem.keoVe++
            }
        }
        CaiDatDongBo.datMocKeo(ctx, BANG_BUOC_CHAN, mocMoi)

        // Đẩy lên (60 ngày gần nhất)
        val tuNgay = GhiNhanBuocChan.ngayChuoi(bayGio - 60L * 24 * 60 * 60 * 1000)
        val trangThai = dongBoDao.layTrangThai(BANG_BUOC_CHAN).associateBy { it.syncId }
        for (r in dao.layTuNgay(tuNgay)) {
            val daGui = trangThai[r.ngay]?.dauVan?.toIntOrNull()
            if (r.soBuoc <= 0 || daGui == r.soBuoc) continue

            val tham = kho.document(r.ngay)
            val soBuocSauCung = fs.runTransaction(Transaction.Function<Long> { giaoDich ->
                val daCo = giaoDich.get(tham).getLong("soBuoc") ?: 0L
                if (r.soBuoc > daCo) {
                    giaoDich.set(
                        tham,
                        mapOf(
                            "ngay" to r.ngay,
                            "soBuoc" to r.soBuoc,
                            "capNhatLuc" to FieldValue.serverTimestamp()
                        )
                    )
                    r.soBuoc.toLong()
                } else {
                    daCo
                }
            }).await().toInt()

            if (soBuocSauCung > r.soBuoc) {
                dao.luuSoBuoc(BuocChanNgay(r.ngay, soBuocSauCung, bayGio))
            }
            dongBoDao.luuTrangThai(
                TrangThaiDongBo(BANG_BUOC_CHAN, ngaySangSo(r.ngay), r.ngay, soBuocSauCung.toString())
            )
            dem.dayLen++
        }
    }

    private fun ngaySangSo(ngay: String): Long = ngay.replace("-", "").toLongOrNull() ?: 0L

    // ================================================================ Cài đặt

    private fun taoCaiDatCucBo(ctx: Context): Map<String, Any?> {
        val quanLyHoSo = QuanLyHoSo(ctx)
        val quanLyNhacNho = QuanLyNhacNho(ctx)

        val ketQua = HashMap<String, Any?>()
        ketQua["mucTieuBuoc"] = QuanLyBuocChan(ctx).docMucTieuBuoc()
        ketQua["nhacNho"] = QuanLyNhacNho.TAT_CA_LOAI.associateWith { quanLyNhacNho.dangBat(it) }
        if (quanLyHoSo.daCoHoSo()) {
            val h = quanLyHoSo.docHoSo()
            ketQua["hoSo"] = mapOf(
                "tuoi" to h.tuoi,
                "gioiTinh" to h.gioiTinh,
                "chieuCaoCm" to h.chieuCaoCm.toDouble(),
                "canNangKg" to h.canNangKg.toDouble(),
                "mucDoHoatDong" to h.mucDoHoatDong,
                "mucTieu" to h.mucTieu,
                "heSoHoatDong" to h.heSoHoatDong
            )
        }
        return ketQua
    }

    private fun apDungCaiDatMay(ctx: Context, duLieu: Map<String, Any?>) {
        (duLieu["hoSo"] as? Map<*, *>)?.let { h ->
            val hoSo = HoSoSucKhoe(
                tuoi = (h["tuoi"] as? Number)?.toInt() ?: 0,
                gioiTinh = h["gioiTinh"] as? String ?: "Nam",
                chieuCaoCm = (h["chieuCaoCm"] as? Number)?.toFloat() ?: 0f,
                canNangKg = (h["canNangKg"] as? Number)?.toFloat() ?: 0f,
                mucDoHoatDong = h["mucDoHoatDong"] as? String ?: "",
                mucTieu = h["mucTieu"] as? String ?: "",
                heSoHoatDong = (h["heSoHoatDong"] as? Number)?.toDouble() ?: 1.2
            )
            QuanLyHoSo(ctx).luuHoSo(hoSo, yeuCauDongBo = false)
        }

        (duLieu["mucTieuBuoc"] as? Number)?.toInt()?.let {
            QuanLyBuocChan(ctx).luuMucTieuBuoc(it, yeuCauDongBo = false)
        }

        (duLieu["nhacNho"] as? Map<*, *>)?.let { nhacNho ->
            val quanLyNhacNho = QuanLyNhacNho(ctx)
            QuanLyNhacNho.TAT_CA_LOAI.forEach { loai ->
                (nhacNho[loai] as? Boolean)?.let {
                    quanLyNhacNho.datTrangThai(loai, it, yeuCauDongBo = false)
                }
            }
        }
    }

    /**
     * Cài đặt gồm hồ sơ, mục tiêu bước chân, công tắc nhắc nhở, lưu trong một tài liệu.
     * Máy có thay đổi chưa gửi thì thắng; nếu không thì nhận bản mới hơn từ đám mây.
     */
    private suspend fun dongBoCaiDat(ctx: Context, goc: DocumentReference, dem: DemSo) {
        val tham = goc.collection("cai_dat").document("chung")
        val snap = tham.get(Source.SERVER).await()
        val damMayTonTai = snap.exists()
        val thoiGianMay = snap.getTimestamp("capNhatLuc")?.toDate()?.time ?: 0L

        val coHoSo = QuanLyHoSo(ctx).daCoHoSo()
        val cucBo = taoCaiDatCucBo(ctx)
        val dauVanCucBo = DauVan.tinh(cucBo)
        val dauVanDaDongBo = CaiDatDongBo.layHashCaiDat(ctx)
        val mocMayDaThay = CaiDatDongBo.layMocCaiDatMay(ctx)

        val nenKeo: Boolean
        val nenDay: Boolean
        if (dauVanDaDongBo.isEmpty()) {
            // Lần đầu trên thiết bị này: máy chưa có hồ sơ thì nhận từ đám mây, có hồ sơ thì gửi lên
            nenKeo = damMayTonTai && !coHoSo
            nenDay = !nenKeo && coHoSo
        } else {
            nenDay = dauVanCucBo != dauVanDaDongBo
            nenKeo = !nenDay && damMayTonTai && thoiGianMay > mocMayDaThay
        }

        if (nenKeo) {
            apDungCaiDatMay(ctx, snap.data ?: emptyMap())
            CaiDatDongBo.datHashCaiDat(ctx, DauVan.tinh(taoCaiDatCucBo(ctx)))
            CaiDatDongBo.datMocCaiDatMay(ctx, thoiGianMay)
            dem.keoVe++
        } else if (nenDay) {
            val duLieu = HashMap<String, Any?>(cucBo)
            duLieu["capNhatLuc"] = FieldValue.serverTimestamp()
            tham.set(duLieu).await()

            // Đọc lại để biết thời điểm máy chủ ghi, tránh lần sau tự kéo lại chính bản vừa gửi
            val moi = tham.get(Source.SERVER).await()
            CaiDatDongBo.datHashCaiDat(ctx, dauVanCucBo)
            CaiDatDongBo.datMocCaiDatMay(ctx, moi.getTimestamp("capNhatLuc")?.toDate()?.time ?: 0L)
            dem.dayLen++
        }
    }

    // ================================================================ Xóa dữ liệu đám mây

    /** Xóa toàn bộ bản sao của tài khoản này trên đám mây. Dữ liệu trên máy không bị đụng tới. */
    suspend fun xoaDuLieuDamMay(context: Context): KetQua = khoa.withLock {
        val appContext = context.applicationContext
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withLock KetQua(false, "Chưa đăng nhập")

        val ketQua = withTimeoutOrNull(THOI_GIAN_TOI_DA_MS) {
            try {
                val fs = FirebaseFirestore.getInstance()
                val goc = fs.collection(GOC).document(uid)

                val cacBang = CacLoaiDongBo.TAT_CA.map { it.ten } + BANG_BUOC_CHAN + "cai_dat"
                for (bang in cacBang) {
                    while (true) {
                        val dot = goc.collection(bang).limit(400).get(Source.SERVER).await()
                        if (dot.isEmpty) break
                        val lo = fs.batch()
                        dot.documents.forEach { lo.delete(it.reference) }
                        lo.commit().await()
                    }
                }

                // Quên trạng thái đã đồng bộ để lần bật lại sẽ gửi lên từ đầu
                AppDatabase.layChoUid(appContext, uid).dongBoDao().xoaTatCaTrangThai()
                CaiDatDongBo.xoaMocDongBo(appContext)
                KetQua(true, "Đã xóa dữ liệu trên đám mây")
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                moTaLoiFirestore(e)
            } catch (e: Exception) {
                KetQua(false, "Lỗi: ${e.message}", canThuLai = true)
            }
        } ?: KetQua(false, "Quá thời gian chờ, hãy thử lại", canThuLai = true)

        ketQua
    }
}
package com.example.appsk.canhbao

import android.content.Context
import com.example.appsk.data.HoSoSucKhoe
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.CanhBaoSucKhoe
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class CanhBaoTamThoi(
    val khoa: String,
    val loai: String,
    val mucDo: Int,
    val tieuDe: String,
    val noiDung: String
)

data class KetQuaPhanTich(
    val canhBao: List<CanhBaoTamThoi>,
    val ghiChuThieuDuLieu: List<String>
)

/**
 * Calo mục tiêu mỗi ngày: giảm cân = TDEE - 500, tăng cân = TDEE + 300,
 * duy trì = TDEE, và không bao giờ thấp hơn BMR.
 */
fun HoSoSucKhoe.caloMucTieu(): Double {
    val tdee = tinhTDEE()
    return when {
        mucTieu.startsWith("Giảm") -> tdee - 500
        mucTieu.startsWith("Tăng") -> tdee + 300
        else -> tdee
    }.coerceAtLeast(tinhBMR())
}

/**
 * Toàn bộ việc so sánh và đánh giá mức cảnh báo do ứng dụng tự tính bằng quy tắc cố định.
 * Không dùng AI và không kết luận bệnh.
 */
class BoPhanTichXuHuong(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.layInstance(appContext)

    companion object {
        private const val MOT_NGAY = 24L * 60 * 60 * 1000
        private const val KHOANG_TRANH_TRUNG = 3 * MOT_NGAY

        // Từ khóa đã bỏ dấu, so khớp theo từ nguyên vẹn
        private val TU_KHOA_NHIEU_DUONG = listOf(
            "tra sua", "nuoc ngot", "coca", "pepsi", "banh ngot", "kem", "che",
            "keo", "socola", "chocolate", "donut", "snack", "khoai tay chien",
            "ga ran", "do chien", "banh ran", "tra dao"
        )
        private val REGEX_NHIEU_DUONG =
            Regex("\\b(" + TU_KHOA_NHIEU_DUONG.joinToString("|") + ")\\b")
    }

    suspend fun phanTich(): KetQuaPhanTich {
        val hoSo = QuanLyHoSo(appContext).docHoSo()
        val bayGio = System.currentTimeMillis()
        val dauHomNay = dauNgay(bayGio)

        val canhBao = mutableListOf<CanhBaoTamThoi>()
        val thieuDuLieu = mutableListOf<String>()


        kiemTraHoatDong(dauHomNay, bayGio, canhBao, thieuDuLieu)
        KiemTraBuocChan.kiemTra(appContext, dauHomNay, canhBao, thieuDuLieu)
        kiemTraAnUong(hoSo, dauHomNay, bayGio, canhBao, thieuDuLieu)
        kiemTraNuoc(hoSo, dauHomNay, canhBao, thieuDuLieu)
        kiemTraCanNang(hoSo, bayGio, canhBao, thieuDuLieu)
        kiemTraMucTieu(hoSo, canhBao)

        return KetQuaPhanTich(canhBao.sortedByDescending { it.mucDo }, thieuDuLieu)
    }

    /**
     * Chỉ lưu cảnh báo chưa được lưu (cùng loại, cùng mức) trong 3 ngày gần đây.
     * Trả về danh sách cảnh báo vừa được lưu mới.
     */
    suspend fun luuCanhBaoMoi(danhSach: List<CanhBaoTamThoi>): List<CanhBaoTamThoi> {
        val bayGio = System.currentTimeMillis()
        val moi = mutableListOf<CanhBaoTamThoi>()
        for (canhBao in danhSach) {
            val daCo = db.canhBaoSucKhoeDao()
                .demGanDay(canhBao.khoa, canhBao.mucDo, bayGio - KHOANG_TRANH_TRUNG)
            if (daCo == 0) {
                db.canhBaoSucKhoeDao().themCanhBao(
                    CanhBaoSucKhoe(
                        khoa = canhBao.khoa,
                        loai = canhBao.loai,
                        mucDo = canhBao.mucDo,
                        tieuDe = canhBao.tieuDe,
                        noiDung = canhBao.noiDung,
                        thoiGianTao = bayGio
                    )
                )
                moi.add(canhBao)
            }
        }
        return moi
    }

    // ------------------------------------------------------------- Vận động

    private suspend fun kiemTraHoatDong(
        dauHomNay: Long,
        bayGio: Long,
        canhBao: MutableList<CanhBaoTamThoi>,
        thieuDuLieu: MutableList<String>
    ) {
        val mocGanDay = dauHomNay - 6 * MOT_NGAY      // 7 ngày gần nhất, tính cả hôm nay
        val mocNenTang = dauHomNay - 34 * MOT_NGAY    // 28 ngày trước đó = mức thông thường

        val tatCa = db.lichSuChayBoDao().layTheoKhoangThoiGian(mocNenTang)
        val nenTang = tatCa.filter { it.thoiGianGhiNhan < mocGanDay }
        val ganDay = tatCa.filter { it.thoiGianGhiNhan >= mocGanDay }

        if (nenTang.size < 4) {
            thieuDuLieu.add("Vận động: cần ít nhất 4 buổi tập có theo dõi GPS trong 4 tuần trước đó để biết mức thông thường của bạn")
            return
        }

        val kmTuanThuong = nenTang.sumOf { it.quangDuongKm.toDouble() } / 4.0
        val buoiTuanThuong = nenTang.size / 4.0
        if (kmTuanThuong < 0.5) return

        if (ganDay.isEmpty()) {
            val ngayTuLanCuoi = ((bayGio - nenTang.last().thoiGianGhiNhan) / MOT_NGAY).toInt()
            val mucDo = if (ngayTuLanCuoi >= 14) MucDoCanhBao.CAO else MucDoCanhBao.VUA
            canhBao.add(
                CanhBaoTamThoi(
                    khoa = "HOAT_DONG",
                    loai = "Vận động",
                    mucDo = mucDo,
                    tieuDe = "Vận động giảm rõ rệt",
                    noiDung = "Bạn chưa có buổi tập GPS nào trong 7 ngày qua (buổi gần nhất cách đây " +
                            "$ngayTuLanCuoi ngày), trong khi 4 tuần trước đó bạn thường tập khoảng " +
                            "${so(buoiTuanThuong)} buổi/tuần (≈ ${so(kmTuanThuong)} km/tuần). " +
                            "Nếu bạn vẫn vận động theo cách khác (phòng gym, bơi...) thì có thể bỏ qua cảnh báo này."
                )
            )
            return
        }

        val kmGanDay = ganDay.sumOf { it.quangDuongKm.toDouble() }
        val tyLe = kmGanDay / kmTuanThuong
        val mucDo = when {
            tyLe < 0.3 -> MucDoCanhBao.VUA
            tyLe < 0.5 -> MucDoCanhBao.NHE
            else -> 0
        }
        if (mucDo > 0) {
            canhBao.add(
                CanhBaoTamThoi(
                    khoa = "HOAT_DONG",
                    loai = "Vận động",
                    mucDo = mucDo,
                    tieuDe = "Vận động giảm so với thường ngày",
                    noiDung = "7 ngày qua bạn tập ${so(kmGanDay)} km, chỉ bằng " +
                            "${(tyLe * 100).roundToInt()}% mức thông thường của bạn " +
                            "(≈ ${so(kmTuanThuong)} km/tuần trong 4 tuần trước đó)."
                )
            )
        }
    }

    // ------------------------------------------------------------- Ăn uống

    private suspend fun kiemTraAnUong(
        hoSo: HoSoSucKhoe,
        dauHomNay: Long,
        bayGio: Long,
        canhBao: MutableList<CanhBaoTamThoi>,
        thieuDuLieu: MutableList<String>
    ) {
        val tu7Ngay = dauHomNay - 6 * MOT_NGAY
        val buaAn7Ngay = db.buaAnDao().layBuaAnTrongNgay(tu7Ngay, bayGio + 1)

        // 1) Calo trung bình: chỉ tính các ngày đã trọn vẹn, không tính hôm nay vì chưa ăn xong
        val caloTheoNgay = buaAn7Ngay
            .filter { it.thoiGianGhiNhan < dauHomNay }
            .groupBy { dauNgay(it.thoiGianGhiNhan) }
            .mapValues { (_, danhSach) -> danhSach.sumOf { it.calo } }

        if (caloTheoNgay.size < 3) {
            thieuDuLieu.add("Ăn uống: cần ghi bữa ăn ít nhất 3 ngày (không tính hôm nay) để so với mục tiêu calo")
        } else {
            val trungBinh = caloTheoNgay.values.average()
            val bmr = hoSo.tinhBMR()
            val mucTieuCalo = hoSo.caloMucTieu()
            val chenhLech = trungBinh - mucTieuCalo
            val soNgay = caloTheoNgay.size

            if (trungBinh < bmr * 0.7) {
                canhBao.add(
                    CanhBaoTamThoi(
                        khoa = "AN_THIEU",
                        loai = "Ăn uống",
                        mucDo = MucDoCanhBao.VUA,
                        tieuDe = "Năng lượng ăn vào khá thấp",
                        noiDung = "Trung bình ${trungBinh.roundToInt()} kcal/ngày trong $soNgay ngày bạn đã ghi, " +
                                "thấp hơn nhiều so với năng lượng cơ bản cơ thể cần (BMR ≈ ${bmr.roundToInt()} kcal). " +
                                "Có thể bạn chưa ghi đủ bữa. Nếu bạn đang chủ ý ăn rất ít, hãy trao đổi với " +
                                "bác sĩ hoặc chuyên gia dinh dưỡng."
                    )
                )
            } else {
                val mucDo = when {
                    hoSo.mucTieu.startsWith("Giảm") -> when {
                        chenhLech > 500 -> MucDoCanhBao.VUA
                        chenhLech > 200 -> MucDoCanhBao.NHE
                        else -> 0
                    }
                    hoSo.mucTieu.startsWith("Tăng") -> when {
                        chenhLech < -700 -> MucDoCanhBao.VUA
                        chenhLech < -400 -> MucDoCanhBao.NHE
                        else -> 0
                    }
                    else -> when {
                        abs(chenhLech) > 700 -> MucDoCanhBao.VUA
                        abs(chenhLech) > 400 -> MucDoCanhBao.NHE
                        else -> 0
                    }
                }
                if (mucDo > 0) {
                    val huong = if (chenhLech > 0) "cao hơn" else "thấp hơn"
                    canhBao.add(
                        CanhBaoTamThoi(
                            khoa = "CALO_LECH_MUC_TIEU",
                            loai = "Ăn uống",
                            mucDo = mucDo,
                            tieuDe = "Lượng calo chưa phù hợp mục tiêu",
                            noiDung = "Trung bình ${trungBinh.roundToInt()} kcal/ngày trong $soNgay ngày đã ghi, " +
                                    "$huong khoảng ${abs(chenhLech).roundToInt()} kcal so với mục tiêu " +
                                    "${mucTieuCalo.roundToInt()} kcal/ngày (mục tiêu của bạn: ${hoSo.mucTieu})."
                        )
                    )
                }
            }
        }

        // 2) Nhiều món ngọt / chiên rán trong 7 ngày (tính cả hôm nay)
        val monNhieuDuong = buaAn7Ngay.filter { REGEX_NHIEU_DUONG.containsMatchIn(boDau(it.tenMon)) }
        val soMon = monNhieuDuong.size
        val mucDoDuong = when {
            soMon >= 7 -> MucDoCanhBao.VUA
            soMon >= 4 -> MucDoCanhBao.NHE
            else -> 0
        }
        if (mucDoDuong > 0) {
            val viDu = monNhieuDuong.map { it.tenMon.trim() }.distinct().take(3).joinToString(", ")
            canhBao.add(
                CanhBaoTamThoi(
                    khoa = "NHIEU_DUONG",
                    loai = "Ăn uống",
                    mucDo = mucDoDuong,
                    tieuDe = "Nhiều món ngọt hoặc chiên rán",
                    noiDung = "Trong 7 ngày qua bạn ghi $soMon món/bữa thuộc nhóm nhiều đường hoặc chiên rán " +
                            "(ví dụ: $viDu). Nhận định này dựa trên tên món bạn nhập nên chỉ mang tính tham khảo; " +
                            "hãy cân nhắc giảm tần suất nếu nó không phù hợp với mục tiêu của bạn."
                )
            )
        }
    }

    // ------------------------------------------------------------- Nước uống

    private suspend fun kiemTraNuoc(
        hoSo: HoSoSucKhoe,
        dauHomNay: Long,
        canhBao: MutableList<CanhBaoTamThoi>,
        thieuDuLieu: MutableList<String>
    ) {
        val tongTheoNgay = (1..6).map { i ->
            val tu = dauHomNay - i * MOT_NGAY
            val den = dauHomNay - (i - 1) * MOT_NGAY
            db.nuocUongDao().tinhTongNuocTrongNgay(tu, den) ?: 0
        }.filter { it > 0 }

        if (tongTheoNgay.size < 3) {
            thieuDuLieu.add("Nước uống: cần ghi lượng nước ít nhất 3 ngày (không tính hôm nay)")
            return
        }

        val trungBinh = tongTheoNgay.average()
        val thamKhao = if (hoSo.canNangKg > 0) hoSo.canNangKg * 30.0 else 2000.0
        if (trungBinh < thamKhao * 0.5) {
            canhBao.add(
                CanhBaoTamThoi(
                    khoa = "NUOC_IT",
                    loai = "Nước uống",
                    mucDo = MucDoCanhBao.NHE,
                    tieuDe = "Uống ít nước hơn mức tham khảo",
                    noiDung = "Trung bình ${trungBinh.roundToInt()} ml/ngày trong ${tongTheoNgay.size} ngày đã ghi, " +
                            "thấp hơn nhiều so với mức tham khảo khoảng ${thamKhao.roundToInt()} ml/ngày " +
                            "(≈ 30 ml cho mỗi kg cân nặng). Có thể bạn chưa ghi đủ lượng nước đã uống."
                )
            )
        }
    }

    // ------------------------------------------------------------- Cân nặng

    private suspend fun kiemTraCanNang(
        hoSo: HoSoSucKhoe,
        bayGio: Long,
        canhBao: MutableList<CanhBaoTamThoi>,
        thieuDuLieu: MutableList<String>
    ) {
        val danhSach = db.lichSuCanNangDao().layTheoKhoangThoiGian(bayGio - 30 * MOT_NGAY)
        if (danhSach.size < 2) {
            thieuDuLieu.add("Cân nặng: cần ít nhất 2 lần ghi cách nhau từ 7 ngày để xem xu hướng")
            return
        }

        val dau = danhSach.first()
        val cuoi = danhSach.last()
        val soNgay = (cuoi.thoiGianGhiNhan - dau.thoiGianGhiNhan) / MOT_NGAY.toDouble()
        if (soNgay < 7) {
            thieuDuLieu.add("Cân nặng: cần ít nhất 2 lần ghi cách nhau từ 7 ngày để xem xu hướng")
            return
        }

        val moiTuan = (cuoi.canNangKg - dau.canNangKg) / (soNgay / 7.0)
        val huong = if (moiTuan > 0) "tăng" else "giảm"

        when {
            abs(moiTuan) > 1.0 -> canhBao.add(
                CanhBaoTamThoi(
                    khoa = "CAN_NANG_THAY_DOI",
                    loai = "Cân nặng",
                    mucDo = MucDoCanhBao.VUA,
                    tieuDe = "Cân nặng thay đổi nhanh",
                    noiDung = "Cân nặng $huong khoảng ${so(abs(moiTuan))} kg/tuần " +
                            "(từ ${so(dau.canNangKg.toDouble())} kg đến ${so(cuoi.canNangKg.toDouble())} kg " +
                            "trong ${soNgay.roundToInt()} ngày). Mức thay đổi nhanh hơn khoảng 1 kg/tuần " +
                            "thường nên có chuyên gia theo dõi. Lưu ý cân nặng dao động trong ngày nên " +
                            "số liệu có thể chưa phản ánh đúng."
                )
            )
            hoSo.mucTieu.startsWith("Giảm") && moiTuan > 0.5 -> canhBao.add(
                CanhBaoTamThoi(
                    khoa = "CAN_NANG_NGUOC_MUC_TIEU",
                    loai = "Cân nặng",
                    mucDo = MucDoCanhBao.NHE,
                    tieuDe = "Cân nặng đang đi ngược mục tiêu",
                    noiDung = "Mục tiêu của bạn là giảm cân nhưng cân nặng đang tăng khoảng " +
                            "${so(moiTuan)} kg/tuần trong ${soNgay.roundToInt()} ngày qua."
                )
            )
            hoSo.mucTieu.startsWith("Tăng") && moiTuan < -0.5 -> canhBao.add(
                CanhBaoTamThoi(
                    khoa = "CAN_NANG_NGUOC_MUC_TIEU",
                    loai = "Cân nặng",
                    mucDo = MucDoCanhBao.NHE,
                    tieuDe = "Cân nặng đang đi ngược mục tiêu",
                    noiDung = "Mục tiêu của bạn là tăng cân/tăng cơ nhưng cân nặng đang giảm khoảng " +
                            "${so(abs(moiTuan))} kg/tuần trong ${soNgay.roundToInt()} ngày qua."
                )
            )
        }
    }

    // ------------------------------------------------------------- Mục tiêu so với BMI

    private fun kiemTraMucTieu(hoSo: HoSoSucKhoe, canhBao: MutableList<CanhBaoTamThoi>) {
        if (hoSo.chieuCaoCm <= 0f || hoSo.canNangKg <= 0f) return
        val bmi = hoSo.tinhBMI()

        if (bmi < 18.5 && hoSo.mucTieu.startsWith("Giảm")) {
            canhBao.add(
                CanhBaoTamThoi(
                    khoa = "MUC_TIEU_BMI",
                    loai = "Mục tiêu",
                    mucDo = MucDoCanhBao.VUA,
                    tieuDe = "Mục tiêu giảm cân có thể chưa phù hợp",
                    noiDung = "Chỉ số BMI hiện tại ${so(bmi)} thuộc nhóm thiếu cân nhưng mục tiêu của bạn " +
                            "đang là giảm cân. BMI chỉ mang tính tham khảo; bạn nên cân nhắc lại mục tiêu " +
                            "hoặc hỏi ý kiến bác sĩ/chuyên gia dinh dưỡng."
                )
            )
        } else if (bmi >= 30 && hoSo.mucTieu.startsWith("Tăng")) {
            canhBao.add(
                CanhBaoTamThoi(
                    khoa = "MUC_TIEU_BMI",
                    loai = "Mục tiêu",
                    mucDo = MucDoCanhBao.NHE,
                    tieuDe = "Mục tiêu tăng cân có thể cần xem lại",
                    noiDung = "Chỉ số BMI hiện tại ${so(bmi)} ở mức béo phì nhưng mục tiêu của bạn đang là " +
                            "tăng cân/tăng cơ. Nếu bạn muốn tăng cơ, hãy cân nhắc tham khảo huấn luyện viên " +
                            "hoặc chuyên gia. BMI chỉ mang tính tham khảo."
                )
            )
        }
    }

    // ------------------------------------------------------------- Tiện ích

    private fun so(giaTri: Double, soLe: Int = 1): String =
        String.format(Locale.getDefault(), "%.${soLe}f", giaTri)

    private fun dauNgay(thoiGian: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = thoiGian
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun boDau(chuoi: String): String {
        val daTach = Normalizer.normalize(chuoi.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        return daTach
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd')
    }
}
package com.example.appsk.thucdon

import android.graphics.Color
import com.example.appsk.canhbao.caloMucTieu
import com.example.appsk.data.HoSoSucKhoe
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Phân loại bữa ăn bằng quy tắc cố định do ứng dụng tự tính, không dùng AI.
 * Các ngưỡng chỉ mang tính tham khảo, không phải chỉ định y tế.
 */
object DanhGiaBuaAn {

    const val PHU_HOP = "PHU_HOP"
    const val CAN_NHAC = "CAN_NHAC"
    const val HAN_CHE = "HAN_CHE"
    const val CHUA_DANH_GIA = "CHUA_DANH_GIA"

    class KetQua(
        val phanLoai: String,
        val lyDo: List<String>,
        val caloMucBua: Int,
        val caloDaAn: Int,
        val mucTieuNgay: Int
    )

    // Từ khóa đã bỏ dấu, so khớp theo từ nguyên vẹn
    private val TU_KHOA_NHIEU_DUONG = listOf(
        "tra sua", "nuoc ngot", "coca", "pepsi", "banh ngot", "kem", "che",
        "keo", "socola", "chocolate", "donut", "snack", "khoai tay chien",
        "ga ran", "do chien", "banh ran", "tra dao"
    )
    private val REGEX_NHIEU_DUONG =
        Regex("\\b(" + TU_KHOA_NHIEU_DUONG.joinToString("|") + ")\\b")

    fun laMonNhieuDuong(ten: String): Boolean =
        REGEX_NHIEU_DUONG.containsMatchIn(KeHoachMonAn.boDau(ten))

    fun ten(phanLoai: String): String = when (phanLoai) {
        PHU_HOP -> "Phù hợp"
        CAN_NHAC -> "Cần cân nhắc"
        HAN_CHE -> "Nên hạn chế"
        else -> "Chưa đánh giá"
    }

    fun mau(phanLoai: String): Int = Color.parseColor(
        when (phanLoai) {
            PHU_HOP -> "#2E7D32"
            CAN_NHAC -> "#EF6C00"
            HAN_CHE -> "#C62828"
            else -> "#757575"
        }
    )

    /** Tỷ lệ calo cả ngày dự kiến cho từng loại bữa. */
    private fun tyLeBua(loaiBua: String): Double = when (loaiBua) {
        "Sáng" -> 0.25
        "Trưa" -> 0.35
        "Tối" -> 0.30
        else -> 0.10
    }

    fun danhGia(
        cacMon: List<MonNhanDien>,
        loaiBua: String,
        hoSo: HoSoSucKhoe,
        caloDaAnHomNay: Int,
        soMonNgotChien7Ngay: Int
    ): KetQua {
        val tong = cacMon.sumOf { it.calo }
        val mucTieuNgay = hoSo.caloMucTieu().roundToInt()
        val mucBua = (mucTieuNgay * tyLeBua(loaiBua)).roundToInt()
        val tongDuKien = caloDaAnHomNay + tong
        val tangCan = hoSo.mucTieu.startsWith("Tăng")

        val lyDo = mutableListOf<String>()
        var diem = 0

        // 1) Tổng calo cả ngày so với mục tiêu
        val nguongNhe = if (tangCan) 1.2 else 1.1
        val nguongNang = if (tangCan) 1.4 else 1.25
        when {
            tongDuKien > mucTieuNgay * nguongNang -> {
                diem += 2
                lyDo.add("Cộng với bữa này, tổng hôm nay khoảng $tongDuKien kcal, vượt mục tiêu $mucTieuNgay kcal khá nhiều.")
            }
            tongDuKien > mucTieuNgay * nguongNhe -> {
                diem += 1
                lyDo.add("Cộng với bữa này, tổng hôm nay khoảng $tongDuKien kcal, vượt nhẹ mục tiêu $mucTieuNgay kcal.")
            }
        }

        // 2) Bữa quá lớn so với phần dự kiến
        if (mucBua > 0 && tong > mucBua * 1.5) {
            diem += 1
            lyDo.add("Bữa này khoảng $tong kcal, cao hơn nhiều so với phần dự kiến khoảng $mucBua kcal cho bữa $loaiBua.")
        }

        // 3) Nhiều món ngọt hoặc chiên rán
        val monNgot = cacMon.filter { laMonNhieuDuong(it.ten) }
        val caloNgot = monNgot.sumOf { it.calo }
        if (tong > 0 && caloNgot >= tong * 0.4) {
            diem += 1
            lyDo.add("Có nhiều món ngọt hoặc chiên rán (${monNgot.joinToString(", ") { it.ten }}).")
        }
        if (caloNgot > 0 && soMonNgotChien7Ngay >= 4) {
            diem += 1
            lyDo.add("Trong 7 ngày qua bạn đã ghi $soMonNgotChien7Ngay món ngọt hoặc chiên rán, bữa này thêm tiếp.")
        }

        // 4) Mục tiêu tăng cân hoặc tăng cơ
        if (tangCan && loaiBua != "Phụ" && mucBua > 0 && tong < mucBua * 0.5) {
            diem += 1
            lyDo.add("Mục tiêu của bạn là tăng cân/tăng cơ nhưng bữa này khá ít (khoảng $tong kcal so với khoảng $mucBua kcal dự kiến).")
        }
        val damTong = cacMon.sumOf { it.dam.toDouble() }
        if (tangCan && damTong > 0 && tong > 0 && damTong * 4 / tong < 0.15) {
            diem += 1
            lyDo.add("Lượng đạm khá thấp (khoảng ${damTong.roundToInt()} g) so với mục tiêu tăng cơ.")
        }

        if (diem == 0) {
            val gan = abs(tong - mucBua) <= mucBua * 0.5
            lyDo.add(
                if (gan) "Calo của bữa ăn (khoảng $tong kcal) gần với phần dự kiến cho bữa $loaiBua (khoảng $mucBua kcal)."
                else "Bữa ăn nhẹ hơn dự kiến (khoảng $tong kcal so với khoảng $mucBua kcal) nhưng vẫn phù hợp."
            )
            lyDo.add("Sau bữa này, hôm nay còn khoảng ${max(0, mucTieuNgay - tongDuKien)} kcal để đạt mục tiêu.")
        }

        val phanLoai = when {
            diem == 0 -> PHU_HOP
            diem == 1 -> CAN_NHAC
            else -> HAN_CHE
        }
        return KetQua(phanLoai, lyDo, mucBua, caloDaAnHomNay, mucTieuNgay)
    }

    // ------------------------------------------------------------ Lưu và đọc danh sách món

    fun monSangJson(cacMon: List<MonNhanDien>): String {
        val mang = JSONArray()
        cacMon.forEach {
            mang.put(
                JSONObject()
                    .put("ten", it.ten)
                    .put("khau_phan", it.khauPhan)
                    .put("calo", it.calo)
                    .put("dam_g", it.dam.toDouble())
                    .put("bot_duong_g", it.botDuong.toDouble())
                    .put("beo_g", it.beo.toDouble())
            )
        }
        return mang.toString()
    }

    fun monJsonSangDong(monJson: String): String = try {
        val mang = JSONArray(monJson)
        (0 until mang.length()).joinToString("\n") { i ->
            val o = mang.getJSONObject(i)
            val khau = o.optString("khau_phan", "")
            val phanKhau = if (khau.isNotBlank()) " ($khau)" else ""
            "- ${o.optString("ten")}$phanKhau: ${o.optInt("calo")} kcal"
        }
    } catch (e: JSONException) {
        ""
    }
}
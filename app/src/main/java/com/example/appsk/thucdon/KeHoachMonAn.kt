package com.example.appsk.thucdon

import com.example.appsk.data.csdl.MonTrongKeHoach
import org.json.JSONObject
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

object KeHoachMonAn {

    data class KetQua(
        val noiDungHienThi: String,
        val cacMon: List<MonTrongKeHoach>
    )

    private class NgayTho(
        val baiTap: String,
        val nuocMl: Int,
        val mon: List<MonTrongKeHoach>
    )

    private val THU_TRONG_TUAN = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    private val THU_TU_LOAI = listOf("Sáng", "Trưa", "Tối", "Phụ")

    /** Ném lỗi nếu dữ liệu AI trả về không đọc được hoặc quá thiếu (dưới 5 ngày hợp lệ). */
    fun phanTich(raw: String): KetQua {
        val batDau = raw.indexOf('{')
        val ketThuc = raw.lastIndexOf('}')
        if (batDau < 0 || ketThuc <= batDau) {
            throw IllegalArgumentException("Không tìm thấy dữ liệu JSON")
        }

        val goc = JSONObject(raw.substring(batDau, ketThuc + 1))
        val mangNgay = goc.getJSONArray("ngay")

        val cacNgay = LinkedHashMap<Int, NgayTho>()
        for (i in 0 until mangNgay.length()) {
            val ngay = mangNgay.optJSONObject(i) ?: continue
            val thu = thuSangThuTu(ngay.optString("thu", ""))
                ?: THU_TRONG_TUAN[i.coerceAtMost(6)]
            if (cacNgay.containsKey(thu)) continue

            val monTrongNgay = mutableListOf<MonTrongKeHoach>()
            val mangBua = ngay.optJSONArray("bua_an")
            if (mangBua != null) {
                for (j in 0 until mangBua.length()) {
                    val bua = mangBua.optJSONObject(j) ?: continue
                    val loai = chuanHoaLoai(bua.optString("loai", "")) ?: continue
                    val ten = bua.optString("ten", "").trim()
                    val calo = bua.optInt("calo", -1)
                    if (ten.isEmpty() || calo !in 20..3000) continue
                    monTrongNgay.add(
                        MonTrongKeHoach(
                            keHoachId = 0,
                            thu = thu,
                            loaiBua = loai,
                            tenMon = ten,
                            calo = calo
                        )
                    )
                }
            }
            if (monTrongNgay.isEmpty()) continue

            cacNgay[thu] = NgayTho(
                baiTap = ngay.optString("bai_tap", "").trim(),
                nuocMl = ngay.optInt("nuoc_ml", 0),
                mon = monTrongNgay.sortedBy { THU_TU_LOAI.indexOf(it.loaiBua) }
            )
        }

        if (cacNgay.size < 5) {
            throw IllegalArgumentException("AI chỉ trả về ${cacNgay.size} ngày hợp lệ")
        }

        val sb = StringBuilder()
        val cacMon = mutableListOf<MonTrongKeHoach>()
        for (thu in THU_TRONG_TUAN) {
            val ngay = cacNgay[thu] ?: continue
            sb.append(tenThu(thu)).append('\n')
            if (ngay.baiTap.isNotEmpty()) sb.append("- Bài tập: ").append(ngay.baiTap).append('\n')
            ngay.mon.forEach { sb.append("- ${it.loaiBua}: ${it.tenMon} (${it.calo} kcal)\n") }
            if (ngay.nuocMl > 0) sb.append("- Nước nên uống: ${ngay.nuocMl} ml\n")
            sb.append("- Tổng calo ăn: khoảng ${ngay.mon.sumOf { it.calo }} kcal\n\n")
            cacMon.addAll(ngay.mon)
        }

        val giaiThich = goc.optString("giai_thich", "").trim()
        if (giaiThich.isNotEmpty()) sb.append("Giải thích:\n").append(giaiThich)

        return KetQua(sb.toString().trim(), cacMon)
    }

    fun tenThu(thu: Int): String = when (thu) {
        Calendar.MONDAY -> "Thứ 2"
        Calendar.TUESDAY -> "Thứ 3"
        Calendar.WEDNESDAY -> "Thứ 4"
        Calendar.THURSDAY -> "Thứ 5"
        Calendar.FRIDAY -> "Thứ 6"
        Calendar.SATURDAY -> "Thứ 7"
        else -> "Chủ nhật"
    }

    fun boDau(chuoi: String): String {
        val daTach = Normalizer.normalize(chuoi.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        return daTach
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd')
            .trim()
    }

    private fun thuSangThuTu(thu: String): Int? {
        val s = boDau(thu)
        return when {
            s.contains("chu nhat") || s == "cn" -> Calendar.SUNDAY
            s.contains("2") -> Calendar.MONDAY
            s.contains("3") -> Calendar.TUESDAY
            s.contains("4") -> Calendar.WEDNESDAY
            s.contains("5") -> Calendar.THURSDAY
            s.contains("6") -> Calendar.FRIDAY
            s.contains("7") -> Calendar.SATURDAY
            else -> null
        }
    }

    private fun chuanHoaLoai(loai: String): String? {
        val s = boDau(loai)
        return when {
            s.contains("sang") -> "Sáng"
            s.contains("trua") -> "Trưa"
            s.contains("toi") -> "Tối"
            s.contains("phu") || s.contains("nhe") || s.contains("snack") -> "Phụ"
            else -> null
        }
    }
}
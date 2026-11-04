package com.example.appsk.thucdon

import android.graphics.Bitmap
import com.example.appsk.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject

/** Một món do AI nhận diện (hoặc do người dùng tự thêm). Mọi giá trị đều là ước tính. */
data class MonNhanDien(
    val ten: String,
    val khauPhan: String,
    val calo: Int,
    val dam: Float = 0f,
    val botDuong: Float = 0f,
    val beo: Float = 0f,
    val tuThem: Boolean = false
)

class KetQuaNhanDien(
    val laMonAn: Boolean,
    val loaiBuaGoiY: String?,
    val cacMon: List<MonNhanDien>,
    val ghiChu: String
)

private val DANH_SACH_MODEL = listOf("gemini-flash-latest", "gemini-flash-lite-latest")

/** Thử model chính 2 lần, nếu vẫn lỗi thì chuyển sang model dự phòng nhẹ hơn. */
private suspend fun <T> thuLaiVoiModel(thucHien: suspend (GenerativeModel) -> T): T {
    var loiCuoi: Exception? = null
    for (tenModel in DANH_SACH_MODEL) {
        val model = GenerativeModel(modelName = tenModel, apiKey = BuildConfig.GEMINI_API_KEY)
        for (lan in 1..2) {
            try {
                return thucHien(model)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loiCuoi = e
                if (lan == 1) delay(2000)
            }
        }
    }
    throw loiCuoi ?: IllegalStateException("Không gọi được AI")
}

object NhanDienAnhMonAn {

    private const val PROMPT =
        "Bạn là chuyên gia dinh dưỡng Việt Nam. Hãy quan sát ảnh và liệt kê các món ăn, đồ uống " +
                "hoặc thành phần chính nhìn thấy trong bữa ăn.\n" +
                "Với mỗi món, ước tính khẩu phần như trong ảnh (so với bát, đĩa, thìa nếu có) và giá trị " +
                "dinh dưỡng của khẩu phần đó.\n" +
                "Quy tắc:\n" +
                "- Chỉ nêu những gì thật sự nhìn thấy, tối đa 8 món. Nếu không chắc, ghi tên kèm \"có thể là\".\n" +
                "- calo là số nguyên (kcal); dam_g, bot_duong_g, beo_g là số gram của khẩu phần đó.\n" +
                "- Nếu ảnh không phải đồ ăn hoặc thức uống, đặt la_mon_an=false và mon=[].\n" +
                "- loai_bua chỉ nhận một trong: Sáng, Trưa, Tối, Phụ (đoán theo kiểu món ăn).\n" +
                "- ghi_chu: tối đa 2 câu, nêu điều không chắc chắn (ví dụ khẩu phần khó đoán). " +
                "Không chẩn đoán bệnh.\n" +
                "Chỉ trả về MỘT đối tượng JSON hợp lệ, không thêm chữ nào khác, không dùng markdown hay khối code:\n" +
                "{\"la_mon_an\":true,\"loai_bua\":\"Trưa\",\"mon\":[{\"ten\":\"Cơm trắng\"," +
                "\"khau_phan\":\"1 chén (khoảng 150g)\",\"calo\":200,\"dam_g\":4,\"bot_duong_g\":44," +
                "\"beo_g\":0.5}],\"ghi_chu\":\"...\"}"

    suspend fun nhanDien(anh: Bitmap): KetQuaNhanDien = thuLaiVoiModel { model ->
        val vanBan = model.generateContent(
            content {
                image(anh)
                text(PROMPT)
            }
        ).text ?: throw IllegalStateException("AI không trả về nội dung")
        phanTich(vanBan)
    }

    /** Ném lỗi nếu dữ liệu AI trả về không đọc được, để lớp gọi thử lại. */
    fun phanTich(raw: String): KetQuaNhanDien {
        val batDau = raw.indexOf('{')
        val ketThuc = raw.lastIndexOf('}')
        require(batDau >= 0 && ketThuc > batDau) { "Không tìm thấy dữ liệu JSON" }

        val goc = JSONObject(raw.substring(batDau, ketThuc + 1))
        val ghiChu = goc.optString("ghi_chu", "").trim()
        val loaiBua = chuanHoaLoai(goc.optString("loai_bua", ""))

        if (!goc.optBoolean("la_mon_an", true)) {
            return KetQuaNhanDien(false, null, emptyList(), ghiChu)
        }

        val cacMon = mutableListOf<MonNhanDien>()
        val mang = goc.optJSONArray("mon")
        if (mang != null) {
            for (i in 0 until minOf(mang.length(), 10)) {
                val m = mang.optJSONObject(i) ?: continue
                val ten = m.optString("ten", "").trim()
                val calo = m.optInt("calo", -1)
                if (ten.isEmpty() || calo !in 1..3000) continue
                cacMon.add(
                    MonNhanDien(
                        ten = ten,
                        khauPhan = m.optString("khau_phan", "").trim(),
                        calo = calo,
                        dam = m.soThuc("dam_g"),
                        botDuong = m.soThuc("bot_duong_g"),
                        beo = m.soThuc("beo_g")
                    )
                )
            }
        }
        require(cacMon.isNotEmpty()) { "AI không nhận diện được món nào" }
        return KetQuaNhanDien(true, loaiBua, cacMon, ghiChu)
    }

    private fun JSONObject.soThuc(khoa: String): Float {
        val d = optDouble(khoa, 0.0)
        return if (d.isNaN() || d < 0) 0f else d.toFloat().coerceAtMost(500f)
    }

    private fun chuanHoaLoai(loai: String): String? {
        val s = KeHoachMonAn.boDau(loai)
        return when {
            s.contains("sang") -> "Sáng"
            s.contains("trua") -> "Trưa"
            s.contains("toi") -> "Tối"
            s.contains("phu") -> "Phụ"
            else -> null
        }
    }
}

object GoiYThayTheAi {
    suspend fun hoi(prompt: String): String = thuLaiVoiModel { model ->
        val vanBan = model.generateContent(prompt).text
        if (vanBan.isNullOrBlank()) throw IllegalStateException("AI không trả về nội dung")
        vanBan.trim()
    }
}
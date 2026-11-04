package com.example.appsk.thucdon

import com.example.appsk.BuildConfig
import com.example.appsk.canhbao.caloMucTieu
import com.example.appsk.data.HoSoSucKhoe
import com.example.appsk.data.csdl.BuaAn
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

data class MonGoiY(val ten: String, val calo: Int)

object ThayMonAn {

    /** Món mới được phép lệch so với món cũ tối đa chừng này kcal. */
    const val DUNG_SAI_CALO = 100

    private const val THOI_GIAN_CHO_AI_MS = 15_000L

    /**
     * Ưu tiên nhờ AI gợi ý và tự kiểm tra calo. Nếu AI bận, chậm quá hoặc trả sai
     * thì dùng danh sách món có sẵn, nên luôn có kết quả.
     */
    suspend fun timMonThay(
        monHienTai: BuaAn,
        cacMonKhacTrongNgay: List<BuaAn>,
        hoSo: HoSoSucKhoe?
    ): MonGoiY {
        val tuAi = withTimeoutOrNull(THOI_GIAN_CHO_AI_MS) {
            hoiAi(monHienTai, cacMonKhacTrongNgay, hoSo)
        }
        if (tuAi != null) return tuAi

        return KhoMonAn.goiYThay(
            loaiBua = monHienTai.loaiBua,
            caloMuc = monHienTai.calo,
            tenHienTai = monHienTai.tenMon,
            tenDaCo = cacMonKhacTrongNgay.map { it.tenMon }
        )
    }

    private suspend fun hoiAi(
        monHienTai: BuaAn,
        cacMonKhac: List<BuaAn>,
        hoSo: HoSoSucKhoe?
    ): MonGoiY? {
        val model = GenerativeModel(
            modelName = "gemini-flash-latest",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
        val prompt = xayDungPrompt(monHienTai, cacMonKhac, hoSo)

        repeat(2) { lan ->
            try {
                val vanBan = model.generateContent(prompt).text
                val goiY = vanBan?.let { docGoiY(it) }
                if (goiY != null && hopLe(goiY, monHienTai)) return goiY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // thử lại một lần, nếu vẫn lỗi thì dùng danh sách có sẵn
            }
            if (lan == 0) delay(1000)
        }
        return null
    }

    private fun xayDungPrompt(
        monHienTai: BuaAn,
        cacMonKhac: List<BuaAn>,
        hoSo: HoSoSucKhoe?
    ): String {
        val thap = maxOf(monHienTai.calo - DUNG_SAI_CALO, 30)
        val cao = monHienTai.calo + DUNG_SAI_CALO

        val sb = StringBuilder()
        sb.append("Bạn là chuyên gia dinh dưỡng. Hãy gợi ý MỘT món ăn thay thế cho bữa ${monHienTai.loaiBua}.\n")
        sb.append("Món hiện tại: ${monHienTai.tenMon} (${monHienTai.calo} kcal).\n")
        if (cacMonKhac.isNotEmpty()) {
            sb.append("Các món khác trong ngày (không được trùng): ")
            sb.append(cacMonKhac.joinToString(", ") { it.tenMon }).append(".\n")
        }
        if (hoSo != null) {
            sb.append("Mục tiêu của người dùng: ${hoSo.mucTieu}, calo cả ngày khoảng ")
            sb.append("${hoSo.caloMucTieu().roundToInt()} kcal.\n")
        }
        sb.append("Yêu cầu bắt buộc:\n")
        sb.append("- Lượng calo của món mới nằm trong khoảng $thap đến $cao kcal.\n")
        sb.append("- Khác món hiện tại, là món phổ biến ở Việt Nam, dễ mua hoặc dễ nấu, ")
        sb.append("phù hợp bữa ${monHienTai.loaiBua} và phù hợp mục tiêu trên.\n")
        sb.append("Chỉ trả về JSON hợp lệ, không giải thích, không dùng markdown: ")
        sb.append("{\"ten\":\"tên món kèm khẩu phần\",\"calo\":số nguyên}")
        return sb.toString()
    }

    private fun docGoiY(raw: String): MonGoiY? {
        val batDau = raw.indexOf('{')
        val ketThuc = raw.lastIndexOf('}')
        if (batDau < 0 || ketThuc <= batDau) return null
        return try {
            val json = JSONObject(raw.substring(batDau, ketThuc + 1))
            val ten = json.optString("ten", "").trim()
            val calo = json.optInt("calo", -1)
            if (ten.isEmpty() || calo <= 0) null else MonGoiY(ten, calo)
        } catch (e: JSONException) {
            null
        }
    }

    private fun hopLe(goiY: MonGoiY, monHienTai: BuaAn): Boolean {
        return goiY.ten.length <= 80 &&
                abs(goiY.calo - monHienTai.calo) <= DUNG_SAI_CALO &&
                KeHoachMonAn.boDau(goiY.ten) != KeHoachMonAn.boDau(monHienTai.tenMon)
    }
}
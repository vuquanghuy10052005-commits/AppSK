package com.example.appsk.ui.phantich

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appsk.BuildConfig
import com.example.appsk.R
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.PhanTichAi
import com.example.appsk.databinding.ActivityPhanTichAiBinding
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PhanTichAiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhanTichAiBinding

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-flash-latest",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhanTichAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnPhanTichLai.setOnClickListener {
            thucHienPhanTich()
        }

        napPhanTichDaLuuHoacPhanTichMoi()
    }

    private fun napPhanTichDaLuuHoacPhanTichMoi() {
        lifecycleScope.launch {
            val db = AppDatabase.layInstance(this@PhanTichAiActivity)
            val phanTichCu = db.phanTichAiDao().layMoiNhat()
            if (phanTichCu != null) {
                binding.tvNoiDungPhanTich.text = phanTichCu.noiDungPhanTich
            } else {
                thucHienPhanTich()
            }
        }
    }

    private fun thoiGian7NgayTruoc(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        return cal.timeInMillis
    }

    private fun thucHienPhanTich() {
        hienThiDangTai(true)

        lifecycleScope.launch {
            val db = AppDatabase.layInstance(this@PhanTichAiActivity)
            val quanLyHoSo = QuanLyHoSo(this@PhanTichAiActivity)
            val tuThoiGian = thoiGian7NgayTruoc()

            val lichSuCanNang = db.lichSuCanNangDao().layTheoKhoangThoiGian(tuThoiGian)
            val lichSuChayBo = db.lichSuChayBoDao().layTheoKhoangThoiGian(tuThoiGian)
            val tongQuangDuong = db.lichSuChayBoDao().tinhTongQuangDuongTu(tuThoiGian) ?: 0f
            val soBuoiChay = db.lichSuChayBoDao().demSoBuoiTu(tuThoiGian)

            if (lichSuCanNang.isEmpty() && lichSuChayBo.isEmpty()) {
                binding.tvNoiDungPhanTich.text = getString(R.string.khong_du_du_lieu_phan_tich)
                hienThiDangTai(false)
                return@launch
            }

            val moTaDuLieu = xayDungMoTaDuLieu(
                quanLyHoSo, lichSuCanNang, lichSuChayBo, tongQuangDuong, soBuoiChay
            )

            try {
                val prompt = "Bạn là chuyên gia tư vấn sức khỏe. Dựa trên dữ liệu 7 ngày gần nhất " +
                        "của người dùng dưới đây, hãy đưa ra nhận xét ngắn gọn (khoảng 150-200 từ) về " +
                        "xu hướng cân nặng, mức độ vận động, và gợi ý cải thiện cụ thể. " +
                        "Trả lời bằng tiếng Việt, giọng văn thân thiện, dễ hiểu.\n\n$moTaDuLieu"

                val phanHoi = model.generateContent(prompt)
                val noiDungPhanTich = phanHoi.text ?: getString(R.string.loi_goi_ai)

                binding.tvNoiDungPhanTich.text = noiDungPhanTich

                db.phanTichAiDao().themPhanTich(
                    PhanTichAi(
                        noiDungPhanTich = noiDungPhanTich,
                        thoiGianPhanTich = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                binding.tvNoiDungPhanTich.text = "Lỗi: ${e.message}"
            } finally {
                hienThiDangTai(false)
            }
        }
    }

    private fun xayDungMoTaDuLieu(
        quanLyHoSo: QuanLyHoSo,
        lichSuCanNang: List<com.example.appsk.data.csdl.LichSuCanNang>,
        lichSuChayBo: List<com.example.appsk.data.csdl.LichSuChayBo>,
        tongQuangDuong: Float,
        soBuoiChay: Int
    ): String {
        val dinhDangNgay = SimpleDateFormat("dd/MM", Locale.getDefault())
        val sb = StringBuilder()

        if (quanLyHoSo.daCoHoSo()) {
            val hoSo = quanLyHoSo.docHoSo()
            sb.append("Hồ sơ: ${hoSo.tuoi} tuổi, ${hoSo.gioiTinh}, cao ${hoSo.chieuCaoCm}cm, ")
            sb.append("mục tiêu: ${hoSo.mucTieu}, mức độ hoạt động: ${hoSo.mucDoHoatDong}.\n\n")
        }

        if (lichSuCanNang.isNotEmpty()) {
            sb.append("Lịch sử cân nặng 7 ngày qua:\n")
            lichSuCanNang.forEach {
                sb.append("- ${dinhDangNgay.format(it.thoiGianGhiNhan)}: ${it.canNangKg}kg\n")
            }
            sb.append("\n")
        } else {
            sb.append("Không có dữ liệu cân nặng mới trong 7 ngày qua.\n\n")
        }

        sb.append("Vận động 7 ngày qua: $soBuoiChay buổi chạy bộ, ")
        sb.append("tổng quãng đường ${String.format("%.2f", tongQuangDuong)}km.\n")

        if (lichSuChayBo.isNotEmpty()) {
            lichSuChayBo.forEach {
                sb.append("- ${dinhDangNgay.format(it.thoiGianGhiNhan)}: ${String.format("%.2f", it.quangDuongKm)}km, ")
                sb.append("tốc độ TB ${String.format("%.1f", it.tocDoTrungBinh)}km/h\n")
            }
        }

        return sb.toString()
    }

    private fun hienThiDangTai(dangTai: Boolean) {
        binding.progressBar.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.tvDangPhanTich.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnPhanTichLai.isEnabled = !dangTai
        if (dangTai) binding.tvNoiDungPhanTich.text = ""
    }
}
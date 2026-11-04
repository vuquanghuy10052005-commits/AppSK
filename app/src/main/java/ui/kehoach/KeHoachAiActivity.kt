package com.example.appsk.ui.kehoach

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.example.appsk.BuildConfig
import com.example.appsk.R
import com.example.appsk.canhbao.caloMucTieu
import com.example.appsk.data.HoSoSucKhoe
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.KeHoachAi
import com.example.appsk.databinding.ActivityKeHoachAiBinding
import com.example.appsk.thucdon.KeHoachMonAn
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class KeHoachAiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeHoachAiBinding
    private lateinit var db: AppDatabase
    private var danhSachKeHoach: List<KeHoachAi> = emptyList()

    private val dinhDangNgayGio = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dinhDangNgay = SimpleDateFormat("dd/MM", Locale.getDefault())

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-flash-latest",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeHoachAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.layInstance(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnTaoKeHoachMoi.setOnClickListener { taoKeHoachMoi() }
        binding.btnKeHoachCu.setOnClickListener { hienThiDanhSachCu() }

        napKeHoachDaLuu()
    }

    private fun napKeHoachDaLuu() {
        lifecycleScope.launch {
            danhSachKeHoach = db.keHoachAiDao().layTatCa()
            val moiNhat = danhSachKeHoach.firstOrNull()
            if (moiNhat != null) {
                hienThiKeHoach(moiNhat)
            } else {
                binding.tvThoiGianTao.visibility = View.GONE
                binding.tvNoiDungKeHoach.text = getString(R.string.chua_co_ke_hoach)
            }
            binding.btnKeHoachCu.isEnabled = danhSachKeHoach.isNotEmpty()
        }
    }

    private fun hienThiKeHoach(keHoach: KeHoachAi) {
        binding.tvThoiGianTao.visibility = View.VISIBLE
        binding.tvThoiGianTao.text = getString(
            R.string.tao_luc, dinhDangNgayGio.format(Date(keHoach.thoiGianTao))
        )
        binding.tvNoiDungKeHoach.text = keHoach.noiDung
    }

    private fun hienThiDanhSachCu() {
        if (danhSachKeHoach.isEmpty()) return
        val nhan = danhSachKeHoach
            .map { dinhDangNgayGio.format(Date(it.thoiGianTao)) }
            .toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.tieu_de_chon_ke_hoach_cu))
            .setItems(nhan) { _, viTri -> hienThiKeHoach(danhSachKeHoach[viTri]) }
            .show()
    }

    private fun taoKeHoachMoi() {
        val quanLyHoSo = QuanLyHoSo(this)
        if (!quanLyHoSo.daCoHoSo()) {
            Toast.makeText(this, getString(R.string.can_ho_so_truoc), Toast.LENGTH_LONG).show()
            return
        }
        val hoSo = quanLyHoSo.docHoSo()

        hienThiDangTai(true)

        lifecycleScope.launch {
            try {
                val prompt = xayDungPrompt(hoSo)
                val ketQua = goiAiVaPhanTich(prompt)

                // Lưu kế hoạch và các món theo từng ngày/bữa trong một giao dịch
                db.withTransaction {
                    val keHoachId = db.keHoachAiDao().themKeHoach(
                        KeHoachAi(
                            noiDung = ketQua.noiDungHienThi,
                            thoiGianTao = System.currentTimeMillis()
                        )
                    )
                    db.monTrongKeHoachDao().themNhieu(
                        ketQua.cacMon.map { it.copy(keHoachId = keHoachId) }
                    )
                }

                danhSachKeHoach = db.keHoachAiDao().layTatCa()
                hienThiKeHoach(danhSachKeHoach.first())
                Toast.makeText(
                    this@KeHoachAiActivity,
                    getString(R.string.td_ke_hoach_da_tao),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val thongBao = when {
                    e is JSONException || e is IllegalArgumentException ->
                        "AI trả về dữ liệu chưa đúng định dạng, hãy bấm tạo lại."
                    e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true ->
                        "Máy chủ AI đang quá tải, vui lòng thử lại sau ít phút."
                    else -> "Lỗi: ${e.message}"
                }
                Toast.makeText(this@KeHoachAiActivity, thongBao, Toast.LENGTH_LONG).show()
            } finally {
                hienThiDangTai(false)
            }
        }
    }

    /**
     * Gọi AI tối đa 3 lần. Thử lại khi máy chủ bận (chờ 2 giây)
     * hoặc khi dữ liệu trả về không đọc được.
     */
    private suspend fun goiAiVaPhanTich(prompt: String): KeHoachMonAn.KetQua {
        var loiCuoi: Exception? = null
        repeat(3) { lanThu ->
            try {
                val vanBan = model.generateContent(prompt).text
                    ?: throw IllegalStateException("AI không trả về nội dung")
                return KeHoachMonAn.phanTich(vanBan)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loiCuoi = e
                if (lanThu < 2) delay(2000)
            }
        }
        throw loiCuoi ?: IllegalStateException("Không tạo được kế hoạch")
    }

    private fun mocThoiGianTruoc(soNgay: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -soNgay)
        return cal.timeInMillis
    }

    /**
     * Các con số (BMI, BMR, TDEE, calo mục tiêu, tổng quãng đường...) do ứng dụng tự tính.
     * AI chỉ nhận số liệu có sẵn để lập kế hoạch và giải thích.
     */
    private suspend fun xayDungPrompt(hoSo: HoSoSucKhoe): String {
        val bayGio = System.currentTimeMillis()
        val tu14Ngay = mocThoiGianTruoc(14)
        val tu7Ngay = mocThoiGianTruoc(7)

        val lichSuCanNang = db.lichSuCanNangDao().layTheoKhoangThoiGian(tu14Ngay)
        val lichSuChayBo = db.lichSuChayBoDao().layTheoKhoangThoiGian(tu14Ngay)
        val tongKm = lichSuChayBo.sumOf { it.quangDuongKm.toDouble() }
        val tongCalo7Ngay = db.buaAnDao().tinhTongCaloTrongNgay(tu7Ngay, bayGio + 1) ?: 0
        val tongNuoc7Ngay = db.nuocUongDao().tinhTongNuocTrongNgay(tu7Ngay, bayGio + 1) ?: 0

        val bmr = hoSo.tinhBMR()
        val tdee = hoSo.tinhTDEE()
        val caloMucTieu = hoSo.caloMucTieu()

        val sb = StringBuilder()
        sb.append("THÔNG TIN NGƯỜI DÙNG (các chỉ số đã được ứng dụng tính sẵn, hãy dùng đúng, không tính lại):\n")
        sb.append("- ${hoSo.tuoi} tuổi, ${hoSo.gioiTinh}, cao ${hoSo.chieuCaoCm.roundToInt()} cm, nặng ${hoSo.canNangKg} kg\n")
        sb.append("- BMI: ${String.format(Locale.US, "%.1f", hoSo.tinhBMI())} (${hoSo.phanLoaiBMI()})\n")
        sb.append("- BMR: ${bmr.roundToInt()} kcal/ngày, TDEE: ${tdee.roundToInt()} kcal/ngày\n")
        sb.append("- Mục tiêu: ${hoSo.mucTieu}\n")
        sb.append("- Mức độ hoạt động hiện tại: ${hoSo.mucDoHoatDong}\n")
        sb.append("- Calo mục tiêu mỗi ngày: ${caloMucTieu.roundToInt()} kcal\n\n")

        sb.append("LỊCH SỬ 14 NGÀY GẦN NHẤT:\n")
        if (lichSuCanNang.size >= 2) {
            val dau = lichSuCanNang.first()
            val cuoi = lichSuCanNang.last()
            sb.append("- Cân nặng: từ ${dau.canNangKg} kg (${dinhDangNgay.format(Date(dau.thoiGianGhiNhan))}) ")
            sb.append("đến ${cuoi.canNangKg} kg (${dinhDangNgay.format(Date(cuoi.thoiGianGhiNhan))})\n")
        } else {
            sb.append("- Cân nặng: chưa đủ dữ liệu để xem xu hướng\n")
        }
        sb.append("- Chạy/đi bộ có theo dõi GPS: ${lichSuChayBo.size} buổi, tổng ${String.format(Locale.US, "%.1f", tongKm)} km\n")
        sb.append("- 7 ngày qua ghi nhận tổng ${tongCalo7Ngay} kcal ăn vào và ${tongNuoc7Ngay} ml nước ")
        sb.append("(người dùng có thể chưa ghi chép đầy đủ nên số liệu có thể thấp hơn thực tế)\n\n")

        val viDuJson = """
            {"ngay":[{"thu":"Thứ 2","bai_tap":"Đi bộ nhanh 30 phút, cường độ vừa","bua_an":[{"loai":"Sáng","ten":"Bánh mì trứng ốp la","calo":350},{"loai":"Trưa","ten":"Cơm gà luộc, rau luộc","calo":600},{"loai":"Tối","ten":"Cá hấp, canh bí đỏ","calo":450},{"loai":"Phụ","ten":"1 hũ sữa chua","calo":110}],"nuoc_ml":2000}],"giai_thich":"2-3 câu giải thích"}
        """.trimIndent()

        sb.append("YÊU CẦU:\n")
        sb.append("Lập kế hoạch 7 ngày (Thứ 2 đến Chủ nhật). Mỗi ngày gồm: bài tập (loại, thời lượng, mức độ), ")
        sb.append("các bữa ăn và lượng nước nên uống. Tổng calo các bữa ăn mỗi ngày phải gần ")
        sb.append("${caloMucTieu.roundToInt()} kcal. Có ít nhất 1-2 ngày nghỉ hoặc vận động nhẹ. ")
        sb.append("Món ăn là món phổ biến ở Việt Nam, dễ mua hoặc dễ nấu.\n\n")
        sb.append("ĐỊNH DẠNG TRẢ LỜI (bắt buộc): chỉ trả về MỘT đối tượng JSON hợp lệ, không thêm chữ nào khác, ")
        sb.append("không dùng markdown hay khối code. Trường loai chỉ nhận một trong: Sáng, Trưa, Tối, Phụ. ")
        sb.append("Mỗi ngày có ít nhất 3 bữa Sáng, Trưa, Tối (bữa Phụ không bắt buộc). calo và nuoc_ml là số nguyên. ")
        sb.append("Phải có đủ 7 ngày. Ví dụ cấu trúc (chỉ minh họa 1 ngày, bạn phải trả đủ 7 ngày):\n")
        sb.append(viDuJson)
        sb.append("\nTrong giai_thich viết 2-3 câu tiếng Việt vì sao kế hoạch phù hợp mục tiêu, nhắc đây chỉ là ")
        sb.append("tham khảo và nên hỏi bác sĩ nếu có vấn đề sức khỏe. Không chẩn đoán bệnh.")

        return sb.toString()
    }

    private fun hienThiDangTai(dangTai: Boolean) {
        binding.progressBar.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.tvDangTao.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnTaoKeHoachMoi.isEnabled = !dangTai
        binding.btnKeHoachCu.isEnabled = !dangTai && danhSachKeHoach.isNotEmpty()
    }
}
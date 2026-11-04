package com.example.appsk.ui.thoitiet

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appsk.BuildConfig
import com.example.appsk.R
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.PhuongAnDuPhong
import com.example.appsk.databinding.ActivityThoiTietBinding
import com.example.appsk.thoitiet.OpenMeteoClient
import com.example.appsk.thoitiet.ThoiTiet
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ThoiTietActivity : AppCompatActivity() {

    companion object {
        // Hà Nội, dùng khi không lấy được vị trí thật
        private const val VI_DO_MAC_DINH = 21.0278
        private const val KINH_DO_MAC_DINH = 105.8342
    }

    private lateinit var binding: ActivityThoiTietBinding
    private lateinit var db: AppDatabase
    private var thoiTietHienTai: ThoiTiet? = null
    private var danhSachPhuongAn: List<PhuongAnDuPhong> = emptyList()

    private val dinhDangNgayGio = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dinhDangNgay = SimpleDateFormat("dd/MM", Locale.getDefault())

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-flash-latest",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    private val xinQuyenViTri = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { duocCap ->
        if (duocCap) {
            layViTriRoiTaiThoiTiet()
        } else {
            taiThoiTiet(VI_DO_MAC_DINH, KINH_DO_MAC_DINH, dungViTriMacDinh = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThoiTietBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.layInstance(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnLamMoi.setOnClickListener { batDauTaiThoiTiet() }
        binding.btnPhuongAn.setOnClickListener { taoPhuongAn() }
        binding.btnPhuongAnCu.setOnClickListener { hienThiDanhSachCu() }

        napPhuongAnDaLuu()
        batDauTaiThoiTiet()
    }

    // ---------------------------------------------------------------- Thời tiết

    private fun coQuyenViTri(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun batDauTaiThoiTiet() {
        hienThiDangTaiThoiTiet(true)
        if (coQuyenViTri()) {
            layViTriRoiTaiThoiTiet()
        } else {
            xinQuyenViTri.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun layViTriRoiTaiThoiTiet() {
        val client = LocationServices.getFusedLocationProviderClient(this)

        client.lastLocation
            .addOnSuccessListener { viTri ->
                if (viTri != null) {
                    taiThoiTiet(viTri.latitude, viTri.longitude, dungViTriMacDinh = false)
                } else {
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token
                    )
                        .addOnSuccessListener { viTriMoi ->
                            if (viTriMoi != null) {
                                taiThoiTiet(viTriMoi.latitude, viTriMoi.longitude, false)
                            } else {
                                taiThoiTiet(VI_DO_MAC_DINH, KINH_DO_MAC_DINH, true)
                            }
                        }
                        .addOnFailureListener {
                            taiThoiTiet(VI_DO_MAC_DINH, KINH_DO_MAC_DINH, true)
                        }
                }
            }
            .addOnFailureListener {
                taiThoiTiet(VI_DO_MAC_DINH, KINH_DO_MAC_DINH, true)
            }
    }

    private fun taiThoiTiet(viDo: Double, kinhDo: Double, dungViTriMacDinh: Boolean) {
        lifecycleScope.launch {
            try {
                val thoiTiet = OpenMeteoClient.layThoiTiet(viDo, kinhDo)
                thoiTietHienTai = thoiTiet
                hienThiThoiTiet(thoiTiet, dungViTriMacDinh)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                thoiTietHienTai = null
                binding.khungThoiTiet.visibility = View.GONE
                binding.tvLoiThoiTiet.visibility = View.VISIBLE
                binding.btnPhuongAn.isEnabled = false
            } finally {
                hienThiDangTaiThoiTiet(false, giuNutPhuongAn = true)
            }
        }
    }

    private fun hienThiThoiTiet(thoiTiet: ThoiTiet, dungViTriMacDinh: Boolean) {
        binding.tvLoiThoiTiet.visibility = View.GONE
        binding.khungThoiTiet.visibility = View.VISIBLE

        binding.tvBieuTuong.text = thoiTiet.bieuTuong()
        binding.tvNhietDo.text = "${thoiTiet.nhietDo.roundToInt()}°C"
        binding.tvMoTaThoiTiet.text = thoiTiet.moTa()
        binding.tvChiTiet.text = getString(
            R.string.chi_tiet_thoi_tiet,
            thoiTiet.camGiac.roundToInt(),
            thoiTiet.doAm,
            thoiTiet.tocDoGio.roundToInt(),
            thoiTiet.xacSuatMua,
            thoiTiet.nhietDoCaoNhat.roundToInt(),
            thoiTiet.nhietDoThapNhat.roundToInt()
        )

        val lyDo = thoiTiet.lyDoKhongThuanLoi()
        if (lyDo.isEmpty()) {
            binding.tvCanhBaoNgoaiTroi.text = getString(R.string.thoi_tiet_thuan_loi)
            binding.tvCanhBaoNgoaiTroi.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvCanhBaoNgoaiTroi.text =
                getString(R.string.canh_bao_ngoai_troi, lyDo.joinToString(", "))
            binding.tvCanhBaoNgoaiTroi.setTextColor(Color.parseColor("#E65100"))
        }

        binding.tvGhiChuViTri.visibility = if (dungViTriMacDinh) View.VISIBLE else View.GONE
        binding.btnPhuongAn.isEnabled = true
    }

    private fun hienThiDangTaiThoiTiet(dangTai: Boolean, giuNutPhuongAn: Boolean = false) {
        binding.progressBarThoiTiet.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnLamMoi.isEnabled = !dangTai
        if (dangTai) {
            binding.khungThoiTiet.visibility = View.GONE
            binding.tvLoiThoiTiet.visibility = View.GONE
            binding.btnPhuongAn.isEnabled = false
        } else if (!giuNutPhuongAn) {
            binding.btnPhuongAn.isEnabled = thoiTietHienTai != null
        }
    }

    // ------------------------------------------------------ Phương án dự phòng

    private fun napPhuongAnDaLuu() {
        lifecycleScope.launch {
            danhSachPhuongAn = db.phuongAnDuPhongDao().layTatCa()
            val moiNhat = danhSachPhuongAn.firstOrNull()
            if (moiNhat != null) {
                hienThiPhuongAn(moiNhat)
            } else {
                binding.tvThoiGianPhuongAn.visibility = View.GONE
                binding.tvNoiDungPhuongAn.text = getString(R.string.chua_co_phuong_an)
            }
            binding.btnPhuongAnCu.isEnabled = danhSachPhuongAn.isNotEmpty()
        }
    }

    private fun hienThiPhuongAn(phuongAn: PhuongAnDuPhong) {
        binding.tvThoiGianPhuongAn.visibility = View.VISIBLE
        binding.tvThoiGianPhuongAn.text = getString(
            R.string.tao_luc, dinhDangNgayGio.format(Date(phuongAn.thoiGianTao))
        ) + " • " + phuongAn.thoiTiet
        binding.tvNoiDungPhuongAn.text = phuongAn.noiDung
    }

    private fun hienThiDanhSachCu() {
        if (danhSachPhuongAn.isEmpty()) return
        val nhan = danhSachPhuongAn
            .map { "${dinhDangNgayGio.format(Date(it.thoiGianTao))} • ${it.thoiTiet}" }
            .toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.tieu_de_chon_phuong_an_cu))
            .setItems(nhan) { _, viTri -> hienThiPhuongAn(danhSachPhuongAn[viTri]) }
            .show()
    }

    private fun taoPhuongAn() {
        val thoiTiet = thoiTietHienTai
        if (thoiTiet == null) {
            Toast.makeText(this, getString(R.string.can_thoi_tiet_truoc), Toast.LENGTH_SHORT).show()
            return
        }

        hienThiDangTaiAi(true)

        lifecycleScope.launch {
            try {
                val prompt = xayDungPrompt(thoiTiet)
                val noiDung = model.generateContent(prompt).text
                if (noiDung.isNullOrBlank()) throw IllegalStateException("AI không trả về nội dung")

                db.phuongAnDuPhongDao().themPhuongAn(
                    PhuongAnDuPhong(
                        thoiTiet = thoiTiet.tomTat(),
                        noiDung = noiDung,
                        thoiGianTao = System.currentTimeMillis()
                    )
                )
                danhSachPhuongAn = db.phuongAnDuPhongDao().layTatCa()
                hienThiPhuongAn(danhSachPhuongAn.first())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val thongBao = if (e.message?.contains("503") == true ||
                    e.message?.contains("UNAVAILABLE") == true
                ) {
                    "Máy chủ AI đang bận, vui lòng thử lại sau ít phút."
                } else {
                    "Lỗi: ${e.message}"
                }
                Toast.makeText(this@ThoiTietActivity, thongBao, Toast.LENGTH_LONG).show()
            } finally {
                hienThiDangTaiAi(false)
            }
        }
    }

    private fun tenThuHomNay(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Thứ 2"
            Calendar.TUESDAY -> "Thứ 3"
            Calendar.WEDNESDAY -> "Thứ 4"
            Calendar.THURSDAY -> "Thứ 5"
            Calendar.FRIDAY -> "Thứ 6"
            Calendar.SATURDAY -> "Thứ 7"
            else -> "Chủ nhật"
        }
    }

    /**
     * Kết hợp: thời tiết hôm nay + kế hoạch tuần AI đã lập trước đó + hồ sơ và mục tiêu.
     * Việc đánh giá thời tiết có thuận lợi hay không do ứng dụng tự tính sẵn.
     */
    private suspend fun xayDungPrompt(thoiTiet: ThoiTiet): String {
        val quanLyHoSo = QuanLyHoSo(this)
        val keHoachMoiNhat = db.keHoachAiDao().layTatCa().firstOrNull()
        val lyDo = thoiTiet.lyDoKhongThuanLoi()

        val sb = StringBuilder()
        sb.append("Hôm nay là ${tenThuHomNay()}, ngày ${dinhDangNgay.format(Date())}.\n\n")

        sb.append("THỜI TIẾT HÔM NAY (dữ liệu thật):\n")
        sb.append("- ${thoiTiet.moTa()}, ${thoiTiet.nhietDo.roundToInt()}°C ")
        sb.append("(cảm giác như ${thoiTiet.camGiac.roundToInt()}°C), độ ẩm ${thoiTiet.doAm}%\n")
        sb.append("- Gió ${thoiTiet.tocDoGio.roundToInt()} km/h, khả năng mưa ${thoiTiet.xacSuatMua}%\n")
        sb.append("- Cao nhất ${thoiTiet.nhietDoCaoNhat.roundToInt()}°C, thấp nhất ${thoiTiet.nhietDoThapNhat.roundToInt()}°C\n")
        if (lyDo.isEmpty()) {
            sb.append("- Ứng dụng đánh giá: thời tiết phù hợp để tập ngoài trời\n\n")
        } else {
            sb.append("- Ứng dụng đánh giá: KHÔNG thuận lợi để tập ngoài trời vì ${lyDo.joinToString(", ")}\n\n")
        }

        if (quanLyHoSo.daCoHoSo()) {
            val hoSo = quanLyHoSo.docHoSo()
            sb.append("NGƯỜI DÙNG: ${hoSo.tuoi} tuổi, ${hoSo.gioiTinh}, cao ${hoSo.chieuCaoCm.roundToInt()} cm, ")
            sb.append("nặng ${hoSo.canNangKg} kg, mục tiêu: ${hoSo.mucTieu}, ")
            sb.append("mức độ hoạt động: ${hoSo.mucDoHoatDong}\n\n")
        }

        if (keHoachMoiNhat != null) {
            sb.append("KẾ HOẠCH TUẦN ĐANG ÁP DỤNG (lập theo thứ trong tuần):\n")
            sb.append(keHoachMoiNhat.noiDung.take(3000))
            sb.append("\n\n")
        } else {
            sb.append("Người dùng chưa có kế hoạch tuần nào, hãy dựa vào mục tiêu để gợi ý.\n\n")
        }

        sb.append("YÊU CẦU:\n")
        sb.append("1. Xác định bài tập của hôm nay trong kế hoạch (nếu có kế hoạch).\n")
        sb.append("2. Nếu thời tiết không thuận lợi cho bài đó, đề xuất 2 phương án thay thế tập trong nhà ")
        sb.append("(không cần thiết bị đắt tiền), có thời lượng và cường độ tương đương để vẫn giữ mục tiêu. ")
        sb.append("Mỗi phương án ghi: tên bài, thời lượng, các động tác chính, lưu ý an toàn.\n")
        sb.append("3. Nếu thời tiết ổn và bài hôm nay phù hợp, nói rõ có thể giữ nguyên và đưa 2-3 lưu ý ")
        sb.append("(giờ tập hợp lý trong ngày, uống nước, trang phục).\n")
        sb.append("Quy tắc trình bày: tiếng Việt, thân thiện, KHÔNG dùng ký hiệu markdown như ** hay #, ")
        sb.append("dùng dấu gạch đầu dòng (-) và xuống dòng rõ ràng. Không chẩn đoán bệnh. ")
        sb.append("Kết thúc bằng một câu nhắc đây chỉ là gợi ý tham khảo.")

        return sb.toString()
    }

    private fun hienThiDangTaiAi(dangTai: Boolean) {
        binding.progressBarAi.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.tvDangTaoPhuongAn.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnPhuongAn.isEnabled = !dangTai && thoiTietHienTai != null
        binding.btnPhuongAnCu.isEnabled = !dangTai && danhSachPhuongAn.isNotEmpty()
    }
}
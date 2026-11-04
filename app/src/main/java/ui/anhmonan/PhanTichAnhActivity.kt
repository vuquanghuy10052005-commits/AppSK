package com.example.appsk.ui.anhmonan

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.withTransaction
import com.example.appsk.R
import com.example.appsk.canhbao.caloMucTieu
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.BuaAn
import com.example.appsk.data.csdl.PhanTichAnh
import com.example.appsk.databinding.ActivityPhanTichAnhBinding
import com.example.appsk.databinding.DialogSuaMonNhanDienBinding
import com.example.appsk.thucdon.DanhGiaBuaAn
import com.example.appsk.thucdon.GoiYThayTheAi
import com.example.appsk.thucdon.KhoMonAn
import com.example.appsk.thucdon.MonNhanDien
import com.example.appsk.thucdon.NhanDienAnhMonAn
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class PhanTichAnhActivity : AppCompatActivity() {

    companion object {
        private const val THU_MUC_ANH = "anh_mon_an"
        private const val KEY_TEP_CHUP = "tep_anh_chup"
        private const val CANH_TOI_DA_GUI_AI = 1024
        private const val CANH_TOI_DA_DOC = 2048
        private const val THOI_GIAN_CHO_ANH_MS = 75_000L
        private const val THOI_GIAN_CHO_GOI_Y_MS = 45_000L
        private const val MOT_NGAY = 24L * 60 * 60 * 1000
    }

    private lateinit var binding: ActivityPhanTichAnhBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: MonNhanDienAdapter

    private var anhHienTai: Bitmap? = null
    private var tepAnhChup: File? = null

    // Trạng thái của lần phân tích hiện tại
    private val cacMon = mutableListOf<MonNhanDien>()
    private var loaiBua = "Trưa"
    private var coHoSo = false
    private var danhGia: DanhGiaBuaAn.KetQua? = null
    private var goiYThayThe = ""

    private var jobPhanTich: Job? = null
    private var jobDanhGia: Job? = null
    private var dangLuu = false

    private val dinhDangNgayGio = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    private val chupAnh = registerForActivityResult(ActivityResultContracts.TakePicture()) { thanhCong ->
        val tep = tepAnhChup
        if (thanhCong && tep != null && tep.exists()) xuLyAnh(Uri.fromFile(tep))
    }

    private val chonAnh = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) xuLyAnh(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhanTichAnhBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.layInstance(this)

        savedInstanceState?.getString(KEY_TEP_CHUP)?.let { tepAnhChup = File(it) }
        // Dọn ảnh tạm của lần trước (chỉ khi mở mới, không phải khi xoay màn hình giữa lúc chụp)
        if (savedInstanceState == null) xoaAnhTam()

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MonNhanDienAdapter { viTri -> moDialogMon(viTri) }
        binding.rvMon.layoutManager = LinearLayoutManager(this)
        binding.rvMon.adapter = adapter

        binding.spinnerLoaiBua.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val moi = resources.getStringArray(R.array.mang_loai_bua)[position]
                if (moi != loaiBua) {
                    loaiBua = moi
                    xoaGoiY()
                    danhGiaLai()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnChup.setOnClickListener { moMayAnh() }
        binding.btnChon.setOnClickListener {
            chonAnh.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnPhanTichLai.setOnClickListener {
            anhHienTai?.let { anh ->
                jobPhanTich?.cancel()
                jobPhanTich = lifecycleScope.launch { thucHienPhanTich(anh) }
            }
        }
        binding.btnThemMon.setOnClickListener { moDialogMon(null) }
        binding.btnGoiY.setOnClickListener { goiYThayTheMon() }
        binding.btnLuu.setOnClickListener { luu() }
        binding.btnLichSu.setOnClickListener { moLichSu() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tepAnhChup?.let { outState.putString(KEY_TEP_CHUP, it.path) }
    }

    // ================================================================ Lấy ảnh

    private fun moMayAnh() {
        try {
            val thuMuc = File(cacheDir, THU_MUC_ANH).apply { mkdirs() }
            val tep = File(thuMuc, "chup_${System.currentTimeMillis()}.jpg")
            tepAnhChup = tep
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", tep)
            chupAnh.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.anh_loi_mo_may_anh), Toast.LENGTH_SHORT).show()
        }
    }

    private fun xoaAnhTam() {
        File(cacheDir, THU_MUC_ANH).listFiles()?.forEach { it.delete() }
    }

    private fun xuLyAnh(uri: Uri) {
        jobPhanTich?.cancel()
        jobPhanTich = lifecycleScope.launch {
            hienThiDangPhanTich(true)
            val anh = docAnh(uri)
            xoaAnhTam()

            if (anh == null) {
                hienThiDangPhanTich(false)
                hienLoi(getString(R.string.anh_loi_doc_anh))
                return@launch
            }
            anhHienTai = anh
            binding.ivAnh.setImageBitmap(anh)
            binding.tvChuaCoAnh.visibility = View.GONE
            thucHienPhanTich(anh)
        }
    }

    /** Đọc ảnh, xoay đúng chiều theo EXIF và thu nhỏ để gửi nhanh và đỡ tốn bộ nhớ. */
    private suspend fun docAnh(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val resolver = contentResolver

            val kichThuoc = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, kichThuoc) }
            if (kichThuoc.outWidth <= 0 || kichThuoc.outHeight <= 0) return@withContext null

            var mau = 1
            while (max(kichThuoc.outWidth, kichThuoc.outHeight) / mau > CANH_TOI_DA_DOC) mau *= 2

            val tuyChon = BitmapFactory.Options().apply { inSampleSize = mau }
            val anhGoc = resolver.openInputStream(uri)
                ?.use { BitmapFactory.decodeStream(it, null, tuyChon) }
                ?: return@withContext null

            val gocXoay = resolver.openInputStream(uri)?.use { docGocXoay(it) } ?: 0
            val daXoay = if (gocXoay != 0) {
                val maTran = Matrix().apply { postRotate(gocXoay.toFloat()) }
                Bitmap.createBitmap(anhGoc, 0, 0, anhGoc.width, anhGoc.height, maTran, true)
            } else {
                anhGoc
            }
            thuNho(daXoay, CANH_TOI_DA_GUI_AI)
        } catch (e: Exception) {
            null
        }
    }

    private fun docGocXoay(luong: InputStream): Int {
        val exif = ExifInterface(luong)
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun thuNho(anh: Bitmap, canhToiDa: Int): Bitmap {
        val canhDai = max(anh.width, anh.height)
        if (canhDai <= canhToiDa) return anh
        val tyLe = canhToiDa.toFloat() / canhDai
        return Bitmap.createScaledBitmap(
            anh, (anh.width * tyLe).roundToInt(), (anh.height * tyLe).roundToInt(), true
        )
    }

    // ================================================================ Nhận diện bằng AI

    private suspend fun thucHienPhanTich(anh: Bitmap) {
        hienThiDangPhanTich(true)
        binding.khungKetQua.visibility = View.GONE
        try {
            val ketQua = withTimeoutOrNull(THOI_GIAN_CHO_ANH_MS) { NhanDienAnhMonAn.nhanDien(anh) }
            when {
                ketQua == null -> hienLoi(getString(R.string.anh_loi_het_gio))
                !ketQua.laMonAn -> hienLoi(getString(R.string.anh_loi_khong_phai_mon_an))
                else -> {
                    cacMon.clear()
                    cacMon.addAll(ketQua.cacMon)
                    loaiBua = ketQua.loaiBuaGoiY ?: loaiBuaTheoGio()
                    chonLoaiTrenSpinner(loaiBua)
                    xoaGoiY()

                    if (ketQua.ghiChu.isNotBlank()) {
                        binding.tvGhiChuAi.visibility = View.VISIBLE
                        binding.tvGhiChuAi.text = getString(R.string.anh_ghi_chu_ai, ketQua.ghiChu)
                    } else {
                        binding.tvGhiChuAi.visibility = View.GONE
                    }

                    anTrangThai()
                    binding.khungKetQua.visibility = View.VISIBLE
                    capNhatDanhSachVaKetQua()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            hienLoi(moTaLoi(e))
        } finally {
            binding.progressPhanTich.visibility = View.GONE
            binding.btnChup.isEnabled = true
            binding.btnChon.isEnabled = true
        }
    }

    private fun moTaLoi(e: Exception): String {
        val thongDiep = e.message ?: ""
        return if (thongDiep.contains("503") || thongDiep.contains("UNAVAILABLE") ||
            thongDiep.contains("429")
        ) {
            getString(R.string.anh_loi_ai_ban)
        } else {
            getString(R.string.anh_loi_chung, thongDiep)
        }
    }

    private fun loaiBuaTheoGio(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..9 -> "Sáng"
            in 10..13 -> "Trưa"
            in 17..21 -> "Tối"
            else -> "Phụ"
        }
    }

    private fun chonLoaiTrenSpinner(loai: String) {
        val viTri = resources.getStringArray(R.array.mang_loai_bua).indexOf(loai)
        if (viTri >= 0) binding.spinnerLoaiBua.setSelection(viTri)
    }

    // ================================================================ Hiển thị

    private fun hienThiDangPhanTich(dang: Boolean) {
        binding.progressPhanTich.visibility = if (dang) View.VISIBLE else View.GONE
        binding.btnChup.isEnabled = !dang
        binding.btnChon.isEnabled = !dang
        if (dang) {
            binding.tvTrangThai.visibility = View.VISIBLE
            binding.tvTrangThai.text = getString(R.string.anh_dang_phan_tich)
            binding.btnPhanTichLai.visibility = View.GONE
        }
    }

    private fun hienLoi(thongBao: String) {
        binding.tvTrangThai.visibility = View.VISIBLE
        binding.tvTrangThai.text = thongBao
        binding.btnPhanTichLai.visibility = if (anhHienTai != null) View.VISIBLE else View.GONE
    }

    private fun anTrangThai() {
        binding.tvTrangThai.visibility = View.GONE
        binding.btnPhanTichLai.visibility = View.GONE
    }

    private fun xoaGoiY() {
        goiYThayThe = ""
        binding.tvGoiY.visibility = View.GONE
    }

    private fun capNhatDanhSachVaKetQua() {
        adapter.capNhat(cacMon.toList())
        hienThiTong()
        danhGiaLai()
    }

    private fun hienThiTong() {
        val tongCalo = cacMon.sumOf { it.calo }
        val dam = cacMon.sumOf { it.dam.toDouble() }.roundToInt()
        val botDuong = cacMon.sumOf { it.botDuong.toDouble() }.roundToInt()
        val beo = cacMon.sumOf { it.beo.toDouble() }.roundToInt()

        binding.tvTongCalo.text = getString(R.string.anh_tong_calo, tongCalo)
        binding.tvTongDinhDuong.text = getString(R.string.anh_tong_dinh_duong, dam, botDuong, beo)
        binding.tvTongGhiChu.visibility =
            if (cacMon.any { it.tuThem }) View.VISIBLE else View.GONE
    }

    // ================================================================ Đánh giá bằng quy tắc

    private fun danhGiaLai() {
        jobDanhGia?.cancel()
        jobDanhGia = lifecycleScope.launch {
            val quanLyHoSo = QuanLyHoSo(this@PhanTichAnhActivity)
            coHoSo = quanLyHoSo.daCoHoSo()
            if (!coHoSo || cacMon.isEmpty()) {
                danhGia = null
                hienThiDanhGia()
                return@launch
            }

            val bayGio = System.currentTimeMillis()
            val dauNgay = dauNgay(bayGio)
            val daAnHomNay = db.buaAnDao().tinhTongCaloTrongNgay(dauNgay, dauNgay + MOT_NGAY) ?: 0
            val soMonNgot = db.buaAnDao()
                .layBuaAnTrongNgay(dauNgay - 6 * MOT_NGAY, bayGio + 1)
                .count { DanhGiaBuaAn.laMonNhieuDuong(it.tenMon) }

            danhGia = DanhGiaBuaAn.danhGia(
                cacMon.toList(), loaiBua, quanLyHoSo.docHoSo(), daAnHomNay, soMonNgot
            )
            hienThiDanhGia()
        }
    }

    private fun hienThiDanhGia() {
        val ketQua = danhGia
        if (!coHoSo || ketQua == null) {
            binding.tvPhanLoai.text = ""
            binding.tvLyDo.text = getString(R.string.anh_can_ho_so)
            return
        }
        binding.tvPhanLoai.text = "● ${DanhGiaBuaAn.ten(ketQua.phanLoai)}"
        binding.tvPhanLoai.setTextColor(DanhGiaBuaAn.mau(ketQua.phanLoai))
        binding.tvLyDo.text = ketQua.lyDo.joinToString("\n") { "• $it" }
    }

    private fun dauNgay(thoiGian: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = thoiGian
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ================================================================ Sửa món

    /** viTri = null là thêm món mới, khác null là sửa hoặc xóa món đó. */
    private fun moDialogMon(viTri: Int?) {
        val monCu = viTri?.let { cacMon.getOrNull(it) }
        val dialogBinding = DialogSuaMonNhanDienBinding.inflate(layoutInflater)

        if (monCu != null) {
            dialogBinding.etTen.setText(monCu.ten)
            dialogBinding.etKhauPhan.setText(monCu.khauPhan)
            dialogBinding.etCalo.setText(monCu.calo.toString())
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(if (monCu == null) R.string.anh_dialog_them else R.string.anh_dialog_sua)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_luu, null)
            .setNegativeButton(R.string.btn_huy, null)
        if (monCu != null) builder.setNeutralButton(R.string.dd_btn_xoa, null)

        val dialog = builder.create()
        // Gắn sự kiện sau khi hiện để dữ liệu sai thì hộp thoại không tự đóng
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val ten = dialogBinding.etTen.text.toString().trim()
                val khauPhan = dialogBinding.etKhauPhan.text.toString().trim()
                val calo = dialogBinding.etCalo.text.toString().trim().toIntOrNull()
                val tenHopLe = ten.isNotEmpty()
                val caloHopLe = calo != null && calo in 1..5000

                dialogBinding.tilTen.error =
                    if (tenHopLe) null else getString(R.string.dd_loi_ten_trong)
                dialogBinding.tilCalo.error =
                    if (caloHopLe) null else getString(R.string.dd_loi_calo)
                if (!tenHopLe || !caloHopLe || calo == null) return@setOnClickListener

                if (monCu == null || viTri == null) {
                    // Món tự thêm: chưa có số liệu đạm, bột đường, chất béo
                    cacMon.add(MonNhanDien(ten, khauPhan, calo, tuThem = true))
                } else {
                    // Đổi calo thì đạm, bột đường, chất béo co giãn theo cùng tỷ lệ
                    val heSo = if (monCu.calo > 0) calo.toFloat() / monCu.calo else 1f
                    cacMon[viTri] = monCu.copy(
                        ten = ten,
                        khauPhan = khauPhan,
                        calo = calo,
                        dam = monCu.dam * heSo,
                        botDuong = monCu.botDuong * heSo,
                        beo = monCu.beo * heSo
                    )
                }
                xoaGoiY()
                capNhatDanhSachVaKetQua()
                dialog.dismiss()
            }

            dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
                if (viTri != null && viTri in cacMon.indices) {
                    cacMon.removeAt(viTri)
                    xoaGoiY()
                    capNhatDanhSachVaKetQua()
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // ================================================================ Gợi ý món thay thế

    private fun goiYThayTheMon() {
        if (cacMon.isEmpty()) return
        binding.progressGoiY.visibility = View.VISIBLE
        binding.btnGoiY.isEnabled = false

        lifecycleScope.launch {
            try {
                val prompt = xayDungPromptThayThe()
                val tuAi = try {
                    withTimeoutOrNull(THOI_GIAN_CHO_GOI_Y_MS) { GoiYThayTheAi.hoi(prompt) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }

                // AI bận hoặc mất mạng thì dùng danh sách món có sẵn trong app
                goiYThayThe = if (!tuAi.isNullOrBlank()) {
                    tuAi
                } else {
                    getString(R.string.anh_goi_y_ngoai_tuyen) + "\n" + taoGoiYCoSan()
                }
                binding.tvGoiY.text = goiYThayThe
                binding.tvGoiY.visibility = View.VISIBLE
            } finally {
                binding.progressGoiY.visibility = View.GONE
                binding.btnGoiY.isEnabled = true
            }
        }
    }

    private suspend fun xayDungPromptThayThe(): String {
        val tongCalo = cacMon.sumOf { it.calo }
        val quanLyHoSo = QuanLyHoSo(this)
        val ketQua = danhGia

        val sb = StringBuilder()
        sb.append("Bạn là chuyên gia dinh dưỡng Việt Nam. Người dùng vừa chụp bữa $loaiBua và đã xác nhận ")
        sb.append("các món sau (calo chỉ là ước tính):\n")
        cacMon.forEach {
            val khau = if (it.khauPhan.isNotBlank()) " (${it.khauPhan})" else ""
            sb.append("- ${it.ten}$khau: ${it.calo} kcal\n")
        }
        sb.append("Tổng bữa này: $tongCalo kcal.\n")

        if (quanLyHoSo.daCoHoSo()) {
            val hoSo = quanLyHoSo.docHoSo()
            sb.append("Mục tiêu của người dùng: ${hoSo.mucTieu}, calo mục tiêu mỗi ngày khoảng ")
            sb.append("${hoSo.caloMucTieu().roundToInt()} kcal.\n")
        }
        if (ketQua != null) {
            sb.append("Hôm nay đã ăn khoảng ${ketQua.caloDaAn} kcal trước bữa này, phần dự kiến cho bữa này ")
            sb.append("khoảng ${ketQua.caloMucBua} kcal.\n")
            sb.append("Ứng dụng đánh giá bữa này: ${DanhGiaBuaAn.ten(ketQua.phanLoai)}. Lý do: ")
            sb.append(ketQua.lyDo.joinToString(" ")).append("\n")
        }

        val thu = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val monKeHoach = db.monTrongKeHoachDao().layCuaKeHoachMoiNhat(thu)
            .filter { it.loaiBua == loaiBua }
        if (monKeHoach.isNotEmpty()) {
            sb.append("Kế hoạch hôm nay cho bữa này: ")
            sb.append(monKeHoach.joinToString("; ") { "${it.tenMon} (${it.calo} kcal)" }).append("\n")
        } else {
            sb.append("Người dùng chưa có kế hoạch cho bữa này.\n")
        }

        sb.append("\nYÊU CẦU: Đề xuất 3 cách thay thế hoặc điều chỉnh THỰC TẾ cho bữa này, dùng nguyên liệu ")
        sb.append("phổ biến ở Việt Nam, dễ mua. Nếu bữa này khác kế hoạch (người dùng có thể thiếu nguyên liệu), ")
        sb.append("hãy gợi ý cách thay thành phần để gần với kế hoạch và mục tiêu hơn. Mỗi gợi ý một dòng, ")
        sb.append("gồm: tên món hoặc cách chỉnh, calo ước tính, và lý do ngắn.\n")
        sb.append("Quy tắc trình bày: tiếng Việt, tối đa 150 từ, KHÔNG dùng ký hiệu markdown như ** hay #, ")
        sb.append("mỗi gợi ý bắt đầu bằng \"- \". Không chẩn đoán bệnh. Dòng cuối nhắc đây chỉ là gợi ý tham khảo.")
        return sb.toString()
    }

    private fun taoGoiYCoSan(): String {
        val tongCalo = cacMon.sumOf { it.calo }
        val mucCalo = danhGia?.caloMucBua ?: tongCalo
        val tenChinh = cacMon.maxByOrNull { it.calo }?.ten ?: ""
        val daDung = cacMon.map { it.ten }.toMutableList()

        return (1..3).joinToString("\n") {
            val goiY = KhoMonAn.goiYThay(loaiBua, mucCalo, tenChinh, daDung)
            daDung.add(goiY.ten)
            "- ${goiY.ten} (khoảng ${goiY.calo} kcal)"
        }
    }

    // ================================================================ Lưu

    private fun tenHienThi(mon: MonNhanDien): String {
        val ten = if (mon.khauPhan.isNotBlank()) "${mon.ten} (${mon.khauPhan})" else mon.ten
        return if (ten.length > 80) ten.take(77) + "..." else ten
    }

    private fun luu() {
        if (cacMon.isEmpty()) {
            Toast.makeText(this, getString(R.string.anh_khong_co_mon), Toast.LENGTH_SHORT).show()
            return
        }
        if (dangLuu) return
        dangLuu = true
        binding.btnLuu.isEnabled = false

        val danhSachMon = cacMon.toList()
        val loai = loaiBua
        val ketQuaDanhGia = danhGia
        val goiY = goiYThayThe

        lifecycleScope.launch {
            val bayGio = System.currentTimeMillis()

            val nhanXet = StringBuilder()
            if (ketQuaDanhGia != null) {
                nhanXet.append("Đánh giá: ${DanhGiaBuaAn.ten(ketQuaDanhGia.phanLoai)}\n")
                ketQuaDanhGia.lyDo.forEach { nhanXet.append("• $it\n") }
            } else {
                nhanXet.append("Chưa đánh giá (chưa có hồ sơ sức khỏe).\n")
            }
            if (goiY.isNotBlank()) nhanXet.append("\nGợi ý thay thế:\n").append(goiY)

            // Lưu các món vào nhật ký và bản ghi lịch sử trong cùng một giao dịch
            db.withTransaction {
                danhSachMon.forEachIndexed { chiSo, mon ->
                    db.buaAnDao().themBuaAn(
                        BuaAn(
                            tenMon = tenHienThi(mon),
                            loaiBua = loai,
                            calo = mon.calo,
                            thoiGianGhiNhan = bayGio + chiSo
                        )
                    )
                }
                db.phanTichAnhDao().them(
                    PhanTichAnh(
                        thoiGian = bayGio,
                        loaiBua = loai,
                        tongCalo = danhSachMon.sumOf { it.calo },
                        phanLoai = ketQuaDanhGia?.phanLoai ?: DanhGiaBuaAn.CHUA_DANH_GIA,
                        monJson = DanhGiaBuaAn.monSangJson(danhSachMon),
                        nhanXet = nhanXet.toString().trim()
                    )
                )
            }

            Toast.makeText(
                this@PhanTichAnhActivity,
                getString(R.string.anh_da_luu, danhSachMon.size),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    // ================================================================ Lịch sử

    private fun moLichSu() {
        lifecycleScope.launch {
            val danhSach = db.phanTichAnhDao().layTatCa().take(50)
            if (danhSach.isEmpty()) {
                Toast.makeText(
                    this@PhanTichAnhActivity, getString(R.string.anh_lich_su_trong), Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val nhan = danhSach.map {
                "${dinhDangNgayGio.format(Date(it.thoiGian))} • ${it.loaiBua} • " +
                        "${it.tongCalo} kcal • ${DanhGiaBuaAn.ten(it.phanLoai)}"
            }.toTypedArray()

            MaterialAlertDialogBuilder(this@PhanTichAnhActivity)
                .setTitle(R.string.anh_tieu_de_lich_su)
                .setItems(nhan) { _, viTri -> moChiTietLichSu(danhSach[viTri]) }
                .setNegativeButton(R.string.btn_dong, null)
                .show()
        }
    }

    private fun moChiTietLichSu(banGhi: PhanTichAnh) {
        val noiDung = DanhGiaBuaAn.monJsonSangDong(banGhi.monJson) + "\n\n" + banGhi.nhanXet

        MaterialAlertDialogBuilder(this)
            .setTitle(
                "${dinhDangNgayGio.format(Date(banGhi.thoiGian))} • ${banGhi.loaiBua} • ${banGhi.tongCalo} kcal"
            )
            .setMessage(noiDung.trim())
            .setPositiveButton(R.string.btn_dong, null)
            .setNeutralButton(R.string.dd_btn_xoa) { _, _ -> hoiXoaLichSu(banGhi) }
            .show()
    }

    private fun hoiXoaLichSu(banGhi: PhanTichAnh) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.anh_xoa_lich_su_tieu_de)
            .setPositiveButton(R.string.dd_btn_xoa) { _, _ ->
                lifecycleScope.launch {
                    db.phanTichAnhDao().xoa(banGhi.id)
                    Toast.makeText(
                        this@PhanTichAnhActivity, getString(R.string.anh_da_xoa), Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.btn_huy, null)
            .show()
    }
}
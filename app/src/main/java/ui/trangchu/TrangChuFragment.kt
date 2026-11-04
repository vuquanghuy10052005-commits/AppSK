package com.example.appsk.ui.trangchu

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appsk.R
import com.example.appsk.canhbao.caloMucTieu
import com.example.appsk.data.HoSoSucKhoe
import com.example.appsk.data.QuanLyBuocChan
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.databinding.FragmentTrangChuBinding
import com.example.appsk.databinding.ItemCongCuBinding
import com.example.appsk.thucdon.KeHoachMonAn
import com.example.appsk.ui.canhbao.CanhBaoSucKhoeActivity
import com.example.appsk.ui.kehoach.KeHoachAiActivity
import com.example.appsk.ui.phantich.PhanTichAiActivity
import com.example.appsk.ui.thoitiet.ThoiTietActivity
import com.example.appsk.ui.trolyai.TroLyAiActivity
import com.example.appsk.ui.anhmonan.PhanTichAnhActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class TrangChuFragment : Fragment() {

    private var _binding: FragmentTrangChuBinding? = null
    private val binding get() = _binding!!

    private class CongCu(
        val bieuTuong: String,
        val tieuDe: String,
        val moTa: String,
        val mauNen: String,
        val moMan: () -> Unit
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrangChuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dungLuoiCongCu()

        binding.theTroLy.setOnClickListener { mo(TroLyAiActivity::class.java) }
        binding.btnThietLapHoSo.setOnClickListener {
            findNavController().navigate(R.id.nav_ho_so)
        }
    }

    override fun onResume() {
        super.onResume()
        capNhat()
    }

    // ------------------------------------------------------------ Công cụ

    /** Thêm công cụ mới chỉ cần thêm một dòng CongCu(...) vào danh sách này. */
    private fun dungLuoiCongCu() {
        val danhSach = listOf(
            CongCu(
                "🗓️", getString(R.string.th_ke_hoach), getString(R.string.th_ke_hoach_mo_ta), "#E8F5E9"
            ) { mo(KeHoachAiActivity::class.java) },
            CongCu(
                "📸", getString(R.string.anh_cong_cu), getString(R.string.anh_cong_cu_mo_ta), "#F3E5F5"
            ) { mo(PhanTichAnhActivity::class.java) },
            CongCu(
                "☀️", getString(R.string.th_thoi_tiet), getString(R.string.th_thoi_tiet_mo_ta), "#FFF8E1"
            ) { mo(ThoiTietActivity::class.java) },
            CongCu(
                "⚠️", getString(R.string.th_canh_bao), getString(R.string.th_canh_bao_mo_ta), "#FFEBEE"
            ) { mo(CanhBaoSucKhoeActivity::class.java) },
            CongCu(
                "📊", getString(R.string.th_phan_tich), getString(R.string.th_phan_tich_mo_ta), "#E3F2FD"
            ) { mo(PhanTichAiActivity::class.java) }
        )

        binding.khungCongCu.removeAllViews()
        danhSach.chunked(2).forEach { cap ->
            val hang = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            cap.forEach { hang.addView(taoTheCongCu(hang, it, hienThi = true)) }
            // Hàng lẻ: thêm một thẻ ẩn để các thẻ luôn rộng bằng nhau
            if (cap.size == 1) hang.addView(taoTheCongCu(hang, cap[0], hienThi = false))
            binding.khungCongCu.addView(hang)
        }
    }

    private fun taoTheCongCu(hang: LinearLayout, congCu: CongCu, hienThi: Boolean): View {
        val the = ItemCongCuBinding.inflate(layoutInflater, hang, false)
        the.tvBieuTuong.text = congCu.bieuTuong
        the.tvBieuTuong.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(congCu.mauNen))
        the.tvTieuDe.text = congCu.tieuDe
        the.tvMoTa.text = congCu.moTa
        the.root.visibility = if (hienThi) View.VISIBLE else View.INVISIBLE
        if (hienThi) the.root.setOnClickListener { congCu.moMan() }
        return the.root
    }

    private fun mo(lop: Class<*>) {
        startActivity(Intent(requireContext(), lop))
    }

    // ------------------------------------------------------------ Hiển thị

    private fun capNhat() {
        capNhatDauTrang()

        val quanLyHoSo = QuanLyHoSo(requireContext())
        if (quanLyHoSo.daCoHoSo()) {
            hienThiHoSo(quanLyHoSo.docHoSo())
        } else {
            hienThiChuaCoHoSo()
        }
    }

    private fun capNhatDauTrang() {
        val bayGio = Calendar.getInstance()
        val ngayChuoi = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(bayGio.time)
        binding.tvNgayThang.text =
            "${KeHoachMonAn.tenThu(bayGio.get(Calendar.DAY_OF_WEEK))}, $ngayChuoi"

        val ten = layTenGoi()
        binding.tvLoiChao.text = if (ten.isEmpty()) {
            getString(R.string.th_chao_khong_ten)
        } else {
            getString(R.string.th_chao, ten)
        }
    }

    /** Tên gọi là từ cuối của họ tên (Vũ Quang Huy → Huy); chưa có tên thì dùng phần đầu email. */
    private fun layTenGoi(): String {
        val nguoiDung = FirebaseAuth.getInstance().currentUser ?: return ""
        val tenDayDu = nguoiDung.displayName?.trim().orEmpty()
        if (tenDayDu.isNotEmpty()) return tenDayDu.split(Regex("\\s+")).last()
        return nguoiDung.email?.substringBefore("@").orEmpty()
    }

    private fun hienThiChuaCoHoSo() {
        binding.theChuaCoHoSo.visibility = View.VISIBLE
        binding.khungChiSo.visibility = View.GONE
        binding.theThangBmi.visibility = View.GONE
        binding.theMucTieu.visibility = View.GONE
        binding.nhomChip.visibility = View.GONE
    }

    private fun hienThiHoSo(hoSo: HoSoSucKhoe) {
        binding.theChuaCoHoSo.visibility = View.GONE
        binding.khungChiSo.visibility = View.VISIBLE
        binding.theThangBmi.visibility = View.VISIBLE
        binding.theMucTieu.visibility = View.VISIBLE
        binding.nhomChip.visibility = View.VISIBLE

        // Thông tin của bạn
        binding.chipTuoi.text = getString(R.string.th_chip_tuoi, hoSo.tuoi)
        binding.chipGioiTinh.text = hoSo.gioiTinh
        binding.chipChieuCao.text =
            getString(R.string.th_chip_chieu_cao, gonSo(hoSo.chieuCaoCm))
        binding.chipCanNang.text =
            getString(R.string.th_chip_can_nang, gonSo(hoSo.canNangKg))

        // BMI
        val bmi = hoSo.tinhBMI()
        val phanLoai = hoSo.phanLoaiBMI()
        val mau = mauPhanLoai(phanLoai)

        binding.tvGiaTriBmi.text = if (bmi > 0) String.format("%.1f", bmi) else "--"
        binding.tvGiaTriBmi.setTextColor(mau)
        binding.tvPhanLoaiBmi.text = if (phanLoai.isEmpty()) "--" else phanLoai
        binding.tvPhanLoaiBmi.setTextColor(mau)
        binding.tvPhanLoaiBmi.backgroundTintList =
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(mau, 0x26))

        // BMR và TDEE
        binding.tvGiaTriBmr.text = hoSo.tinhBMR().roundToInt().toString()
        binding.tvGiaTriTdee.text = hoSo.tinhTDEE().roundToInt().toString()

        capNhatThangBmi(bmi, mau)

        // Mục tiêu đề ra
        binding.tvMucTieu.text = hoSo.mucTieu.ifBlank { "--" }
        binding.tvCaloMucTieu.text =
            getString(R.string.th_kcal, hoSo.caloMucTieu().roundToInt())
        binding.tvGiaiThichCalo.text = getString(
            when {
                hoSo.mucTieu.startsWith("Giảm") -> R.string.th_giai_thich_giam
                hoSo.mucTieu.startsWith("Tăng") -> R.string.th_giai_thich_tang
                else -> R.string.th_giai_thich_duy_tri
            }
        )
        binding.tvBuocMucTieu.text = getString(
            R.string.th_so_buoc,
            String.format("%,d", QuanLyBuocChan(requireContext()).docMucTieuBuoc())
        )
        binding.tvMucDoHoatDong.text = hoSo.mucDoHoatDong.ifBlank { "--" }
    }

    /** Thang BMI hiển thị từ 15 đến 35, mũi tên nằm đúng vị trí BMI của bạn. */
    private fun capNhatThangBmi(bmi: Double, mau: Int) {
        if (bmi <= 0) {
            binding.tvDauBmi.visibility = View.INVISIBLE
            return
        }
        binding.tvDauBmi.visibility = View.VISIBLE
        binding.tvDauBmi.setTextColor(mau)

        val viTri = ((bmi - 15.0) / 20.0).coerceIn(0.02, 0.98).toFloat()
        val thongSo = binding.guidelineBmi.layoutParams as ConstraintLayout.LayoutParams
        thongSo.guidePercent = viTri
        binding.guidelineBmi.layoutParams = thongSo
    }

    private fun mauPhanLoai(phanLoai: String): Int = Color.parseColor(
        when (phanLoai) {
            "Thiếu cân" -> "#1976D2"
            "Bình thường" -> "#2E7D32"
            "Thừa cân" -> "#EF6C00"
            else -> "#C62828"
        }
    )

    /** 50.0 → "50", 50.5 → "50,5". */
    private fun gonSo(giaTri: Float): String =
        if (giaTri % 1f == 0f) giaTri.toInt().toString() else String.format("%.1f", giaTri)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.appsk.ui.hoso

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appsk.DangNhapActivity
import com.example.appsk.R
import com.example.appsk.TheoDoiChayBoService
import com.example.appsk.data.HoSoSucKhoe
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.LichSuCanNang
import com.example.appsk.databinding.FragmentHoSoBinding
import com.example.appsk.nhacnho.QuanLyNhacNho
import com.example.appsk.ui.caidat.CaiDatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HoSoFragment : Fragment() {

    private var _binding: FragmentHoSoBinding? = null
    private val binding get() = _binding!!
    private lateinit var quanLyHoSo: QuanLyHoSo
    private lateinit var quanLyNhacNho: QuanLyNhacNho

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHoSoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quanLyHoSo = QuanLyHoSo(requireContext())
        quanLyNhacNho = QuanLyNhacNho(requireContext())

        val nguoiDungHienTai = FirebaseAuth.getInstance().currentUser
        binding.tvEmailNguoiDung.text = nguoiDungHienTai?.email ?: ""

        napDuLieuDaLuu()

        binding.btnLuuHoSo.setOnClickListener { luuHoSo() }

        binding.btnCaiDat.setOnClickListener {
            startActivity(Intent(requireContext(), CaiDatActivity::class.java))
        }

        binding.btnDangXuat.setOnClickListener { hoiDangXuat() }
    }

    // ------------------------------------------------------------ Đăng xuất

    private fun hoiDangXuat() {
        // Đang theo dõi buổi tập thì chưa cho đăng xuất, tránh buổi tập lưu nhầm tài khoản
        if (TheoDoiChayBoService.dangTheoDoi) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.tk_khong_the_dang_xuat)
                .setMessage(R.string.tk_hay_dung_buoi_tap)
                .setPositiveButton(R.string.btn_dong, null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tk_hoi_dang_xuat)
            .setMessage(R.string.tk_noi_dung_dang_xuat)
            .setPositiveButton(R.string.btn_dang_xuat) { _, _ -> thucHienDangXuat() }
            .setNegativeButton(R.string.btn_huy, null)
            .show()
    }

    private fun thucHienDangXuat() {
        // Hủy các việc nền của tài khoản này, cài đặt đã lưu vẫn được giữ cho lần đăng nhập sau
        quanLyNhacNho.huyTatCaViecNen()

        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), DangNhapActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ------------------------------------------------------------ Hồ sơ

    private fun napDuLieuDaLuu() {
        if (!quanLyHoSo.daCoHoSo()) return
        val hoSo = quanLyHoSo.docHoSo()

        binding.etTuoi.setText(if (hoSo.tuoi > 0) hoSo.tuoi.toString() else "")
        binding.etChieuCao.setText(if (hoSo.chieuCaoCm > 0) hoSo.chieuCaoCm.toString() else "")
        binding.etCanNang.setText(if (hoSo.canNangKg > 0) hoSo.canNangKg.toString() else "")

        val mangGioiTinh = resources.getStringArray(R.array.mang_gioi_tinh)
        val viTriGioiTinh = mangGioiTinh.indexOf(hoSo.gioiTinh)
        if (viTriGioiTinh >= 0) binding.spinnerGioiTinh.setSelection(viTriGioiTinh)

        val mangMucDo = resources.getStringArray(R.array.mang_muc_do_hoat_dong)
        val viTriMucDo = mangMucDo.indexOf(hoSo.mucDoHoatDong)
        if (viTriMucDo >= 0) binding.spinnerMucDoHoatDong.setSelection(viTriMucDo)

        val mangMucTieu = resources.getStringArray(R.array.mang_muc_tieu)
        val viTriMucTieu = mangMucTieu.indexOf(hoSo.mucTieu)
        if (viTriMucTieu >= 0) binding.spinnerMucTieu.setSelection(viTriMucTieu)
    }

    private fun luuHoSo() {
        val tuoiText = binding.etTuoi.text.toString().trim()
        val chieuCaoText = binding.etChieuCao.text.toString().trim()
        val canNangText = binding.etCanNang.text.toString().trim()

        if (tuoiText.isEmpty() || chieuCaoText.isEmpty() || canNangText.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.loi_thieu_thong_tin), Toast.LENGTH_SHORT).show()
            return
        }

        val tuoi = tuoiText.toIntOrNull() ?: 0
        val chieuCao = chieuCaoText.toFloatOrNull() ?: 0f
        val canNang = canNangText.toFloatOrNull() ?: 0f
        val gioiTinh = binding.spinnerGioiTinh.selectedItem.toString()
        val mucDoHoatDong = binding.spinnerMucDoHoatDong.selectedItem.toString()
        val mucTieu = binding.spinnerMucTieu.selectedItem.toString()
        val heSo = QuanLyHoSo.HE_SO_THEO_MUC_DO[mucDoHoatDong] ?: 1.2

        val hoSo = HoSoSucKhoe(
            tuoi = tuoi,
            gioiTinh = gioiTinh,
            chieuCaoCm = chieuCao,
            canNangKg = canNang,
            mucDoHoatDong = mucDoHoatDong,
            mucTieu = mucTieu,
            heSoHoatDong = heSo
        )

        quanLyHoSo.luuHoSo(hoSo)
        luuLichSuCanNang(canNang)
        Toast.makeText(requireContext(), getString(R.string.da_luu_ho_so), Toast.LENGTH_SHORT).show()
    }

    private fun luuLichSuCanNang(canNang: Float) {
        // Lấy database của tài khoản hiện tại ngay lúc bấm lưu
        val db = AppDatabase.layInstance(requireContext())
        lifecycleScope.launch {
            db.lichSuCanNangDao().themBanGhi(
                LichSuCanNang(
                    canNangKg = canNang,
                    thoiGianGhiNhan = System.currentTimeMillis()
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
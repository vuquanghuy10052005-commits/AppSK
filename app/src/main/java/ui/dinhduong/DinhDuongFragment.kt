package com.example.appsk.ui.dinhduong

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsk.R
import com.example.appsk.canhbao.caloMucTieu
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.BuaAn
import com.example.appsk.data.csdl.MonTrongKeHoach
import com.example.appsk.data.csdl.NuocUong
import com.example.appsk.databinding.DialogThemBuaAnBinding
import com.example.appsk.databinding.FragmentDinhDuongBinding
import com.example.appsk.thucdon.KeHoachMonAn
import com.example.appsk.thucdon.ThayMonAn
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class DinhDuongFragment : Fragment() {

    private var _binding: FragmentDinhDuongBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BuaAnAdapter
    private var dangThayMon = false

    // Ngày đang xem, luôn được đưa về 00:00:00
    private val ngayDangXem: Calendar = Calendar.getInstance().also { chuanHoa(it) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDinhDuongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BuaAnAdapter { buaAn -> moLuaChonMon(buaAn) }
        binding.rvDanhSachBuaAn.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDanhSachBuaAn.adapter = adapter

        binding.btnNgayTruoc.setOnClickListener { doiNgay(-1) }
        binding.btnNgaySau.setOnClickListener { doiNgay(1) }
        binding.tvNgay.setOnClickListener { moChonNgay() }

        binding.btnThemNuoc.setOnClickListener { themNuoc(250) }
        binding.btnHoanTacNuoc.setOnClickListener { hoanTacNuoc() }
        binding.btnThemBuaAn.setOnClickListener { moLuaChonThem() }
        binding.btnBieuDo.setOnClickListener {
            startActivity(Intent(requireContext(), BieuDoDinhDuongActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        capNhatNhanNgay()
        taiLaiDuLieu()
    }

    // ------------------------------------------------------------ Ngày đang xem

    private fun doiNgay(soNgay: Int) {
        ngayDangXem.add(Calendar.DAY_OF_YEAR, soNgay)
        chuanHoa(ngayDangXem)
        capNhatNhanNgay()
        taiLaiDuLieu()
    }

    private fun moChonNgay() {
        val hopThoai = DatePickerDialog(
            requireContext(),
            { _, nam, thang, ngay ->
                ngayDangXem.set(nam, thang, ngay)
                chuanHoa(ngayDangXem)
                capNhatNhanNgay()
                taiLaiDuLieu()
            },
            ngayDangXem.get(Calendar.YEAR),
            ngayDangXem.get(Calendar.MONTH),
            ngayDangXem.get(Calendar.DAY_OF_MONTH)
        )
        hopThoai.datePicker.maxDate = System.currentTimeMillis()
        hopThoai.show()
    }

    private fun capNhatNhanNgay() {
        val homNay = Calendar.getInstance().also { chuanHoa(it) }
        val homQua = (homNay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val ngayChuoi = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ngayDangXem.time)

        val tien = when (ngayDangXem.timeInMillis) {
            homNay.timeInMillis -> getString(R.string.dd_hom_nay)
            homQua.timeInMillis -> getString(R.string.dd_hom_qua)
            else -> tenThuDangXem()
        }
        binding.tvNgay.text = "$tien • $ngayChuoi"
        binding.btnNgaySau.isEnabled = ngayDangXem.timeInMillis < homNay.timeInMillis
    }

    private fun tenThuDangXem(): String =
        KeHoachMonAn.tenThu(ngayDangXem.get(Calendar.DAY_OF_WEEK))

    private fun laHomNay(): Boolean {
        val homNay = Calendar.getInstance().also { chuanHoa(it) }
        return homNay.timeInMillis == ngayDangXem.timeInMillis
    }

    private fun khoangNgayDangXem(): Pair<Long, Long> {
        val tu = ngayDangXem.timeInMillis
        val sang = (ngayDangXem.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return tu to sang.timeInMillis
    }

    /** Hôm nay thì ghi giờ hiện tại, ngày cũ thì ghi vào 12:00 của ngày đó. */
    private fun thoiGianGhiMoi(): Long =
        if (laHomNay()) System.currentTimeMillis()
        else ngayDangXem.timeInMillis + 12 * 60 * 60 * 1000L

    /** Giờ thường lệ của từng loại bữa, dùng cho món lấy từ kế hoạch. */
    private fun gioMacDinh(loaiBua: String): Long {
        val gio = when (loaiBua) {
            "Sáng" -> 7.0
            "Trưa" -> 12.0
            "Phụ" -> 15.5
            "Tối" -> 19.0
            else -> 12.0
        }
        return (gio * 60 * 60 * 1000).toLong()
    }

    // ------------------------------------------------------------ Hiển thị dữ liệu

    private fun taiLaiDuLieu() {
        val appContext = requireContext().applicationContext
        val (tu, den) = khoangNgayDangXem()
        val homNay = laHomNay()

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.layInstance(appContext)
            val danhSach = db.buaAnDao().layBuaAnTrongNgay(tu, den).sortedBy { it.thoiGianGhiNhan }
            val tongCalo = danhSach.sumOf { it.calo }
            val tongNuoc = db.nuocUongDao().tinhTongNuocTrongNgay(tu, den) ?: 0

            val quanLyHoSo = QuanLyHoSo(appContext)
            val hoSo = if (quanLyHoSo.daCoHoSo()) quanLyHoSo.docHoSo() else null

            if (_binding == null) return@launch

            binding.tvGiaTriCalo.text = "$tongCalo kcal"
            if (hoSo != null) {
                val mucTieu = hoSo.caloMucTieu().roundToInt()
                binding.progressCalo.visibility = View.VISIBLE
                binding.progressCalo.progress =
                    ((tongCalo * 100f) / mucTieu).roundToInt().coerceIn(0, 100)

                val dong2 = when {
                    tongCalo > mucTieu -> getString(R.string.dd_vuot, tongCalo - mucTieu)
                    homNay -> getString(R.string.dd_con_lai, mucTieu - tongCalo)
                    else -> getString(R.string.dd_thap_hon, mucTieu - tongCalo)
                }
                binding.tvChiTietCalo.text =
                    getString(R.string.dd_muc_tieu_calo, mucTieu) + "\n" + dong2
            } else {
                binding.progressCalo.visibility = View.GONE
                binding.tvChiTietCalo.text = getString(R.string.dd_chua_co_ho_so_calo)
            }

            val thamKhaoNuoc = if (hoSo != null && hoSo.canNangKg > 0f) {
                (hoSo.canNangKg * 30).roundToInt()
            } else 2000
            binding.tvGiaTriNuoc.text = "$tongNuoc ml"
            binding.progressNuoc.progress =
                ((tongNuoc * 100f) / thamKhaoNuoc).roundToInt().coerceIn(0, 100)
            binding.tvChiTietNuoc.text = getString(R.string.dd_muc_tham_khao_nuoc, thamKhaoNuoc)

            adapter.capNhatDanhSach(danhSach)
            binding.tvDanhSachTrong.visibility = if (danhSach.isEmpty()) View.VISIBLE else View.GONE
            binding.tvGoiYCham.visibility = if (danhSach.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    // ------------------------------------------------------------ Nước uống

    private fun themNuoc(soMl: Int) {
        val appContext = requireContext().applicationContext
        val thoiGian = thoiGianGhiMoi()
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.layInstance(appContext).nuocUongDao()
                .themNuocUong(NuocUong(soMl = soMl, thoiGianGhiNhan = thoiGian))
            taiLaiDuLieu()
        }
    }

    private fun hoanTacNuoc() {
        val appContext = requireContext().applicationContext
        val (tu, den) = khoangNgayDangXem()
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.layInstance(appContext).nuocUongDao()
            val lanGanNhat = dao.layTrongNgay(tu, den).firstOrNull()
            if (lanGanNhat == null) {
                Toast.makeText(
                    appContext, getString(R.string.dd_khong_co_nuoc_de_hoan_tac), Toast.LENGTH_SHORT
                ).show()
            } else {
                dao.xoaNuocUong(lanGanNhat.id)
                taiLaiDuLieu()
            }
        }
    }

    // ------------------------------------------------------------ Thêm bữa ăn từ kế hoạch

    /**
     * Lấy các món của kế hoạch mới nhất cho đúng thứ của ngày đang xem.
     * Bữa nào trong ngày đã có món thì không đề xuất lại.
     */
    private fun moLuaChonThem() {
        val appContext = requireContext().applicationContext
        val thu = ngayDangXem.get(Calendar.DAY_OF_WEEK)
        val (tu, den) = khoangNgayDangXem()

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.layInstance(appContext)
            val monKeHoach = db.monTrongKeHoachDao().layCuaKeHoachMoiNhat(thu)
            val cacBuaDaCo = db.buaAnDao().layBuaAnTrongNgay(tu, den).map { it.loaiBua }.toSet()
            val chuaThem = monKeHoach.filter { it.loaiBua !in cacBuaDaCo }

            if (_binding == null) return@launch

            when {
                monKeHoach.isEmpty() -> {
                    Toast.makeText(
                        appContext, getString(R.string.td_khong_co_ke_hoach), Toast.LENGTH_LONG
                    ).show()
                    moDialogBuaAn(null)
                }
                chuaThem.isEmpty() -> {
                    Toast.makeText(
                        appContext, getString(R.string.td_da_them_het), Toast.LENGTH_LONG
                    ).show()
                    moDialogBuaAn(null)
                }
                else -> moDialogChonTuKeHoach(chuaThem)
            }
        }
    }

    private fun moDialogChonTuKeHoach(danhSach: List<MonTrongKeHoach>) {
        val nhan = danhSach
            .map { "${it.loaiBua}: ${it.tenMon} (${it.calo} kcal)" }
            .toTypedArray()
        val daChon = BooleanArray(danhSach.size) { true }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.td_tieu_de_chon_ke_hoach, tenThuDangXem()))
            .setMultiChoiceItems(nhan, daChon) { _, viTri, duocChon -> daChon[viTri] = duocChon }
            .setPositiveButton(R.string.td_btn_them_da_chon) { _, _ ->
                val dsChon = danhSach.filterIndexed { viTri, _ -> daChon[viTri] }
                if (dsChon.isNotEmpty()) themTuKeHoach(dsChon)
            }
            .setNeutralButton(R.string.td_btn_nhap_khac) { _, _ -> moDialogBuaAn(null) }
            .setNegativeButton(R.string.btn_huy, null)
            .show()
    }

    private fun themTuKeHoach(danhSach: List<MonTrongKeHoach>) {
        val appContext = requireContext().applicationContext
        val dauNgay = ngayDangXem.timeInMillis

        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.layInstance(appContext).buaAnDao()
            danhSach.forEachIndexed { viTri, mon ->
                dao.themBuaAn(
                    BuaAn(
                        tenMon = mon.tenMon,
                        loaiBua = mon.loaiBua,
                        calo = mon.calo,
                        thoiGianGhiNhan = dauNgay + gioMacDinh(mon.loaiBua) + viTri * 1000L
                    )
                )
            }
            taiLaiDuLieu()
            Toast.makeText(
                appContext, getString(R.string.td_da_them_n_mon, danhSach.size), Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ------------------------------------------------------------ Chạm vào món: thay / sửa / xóa

    private fun moLuaChonMon(buaAn: BuaAn) {
        val cacLuaChon = arrayOf(
            getString(R.string.td_thay_mon),
            getString(R.string.td_sua_mon),
            getString(R.string.td_xoa_mon)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${buaAn.tenMon} (${buaAn.calo} kcal)")
            .setItems(cacLuaChon) { _, viTri ->
                when (viTri) {
                    0 -> thayMon(buaAn)
                    1 -> moDialogBuaAn(buaAn)
                    else -> xoaBuaAn(buaAn)
                }
            }
            .show()
    }

    /** Thay bằng món cùng loại bữa, chênh lệch không quá ±100 kcal so với món cũ. */
    private fun thayMon(buaAn: BuaAn) {
        if (dangThayMon) return
        dangThayMon = true
        binding.progressThayMon.visibility = View.VISIBLE

        val appContext = requireContext().applicationContext
        val (tu, den) = khoangNgayDangXem()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = AppDatabase.layInstance(appContext)
                val dao = db.buaAnDao()
                val cacMonKhac = dao.layBuaAnTrongNgay(tu, den).filter { it.id != buaAn.id }

                val quanLyHoSo = QuanLyHoSo(appContext)
                val hoSo = if (quanLyHoSo.daCoHoSo()) quanLyHoSo.docHoSo() else null

                val monMoi = ThayMonAn.timMonThay(buaAn, cacMonKhac, hoSo)
                dao.capNhatBuaAn(buaAn.copy(tenMon = monMoi.ten, calo = monMoi.calo))
                taiLaiDuLieu()

                if (_binding != null) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.td_da_thay, buaAn.tenMon, monMoi.ten, monMoi.calo),
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.dd_hoan_tac) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            dao.capNhatBuaAn(buaAn) // khôi phục đúng món cũ
                            taiLaiDuLieu()
                        }
                    }.show()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(
                    appContext, getString(R.string.td_loi_thay_mon), Toast.LENGTH_SHORT
                ).show()
            } finally {
                dangThayMon = false
                if (_binding != null) binding.progressThayMon.visibility = View.GONE
            }
        }
    }

    // ------------------------------------------------------------ Bữa ăn: nhập tay / sửa / xóa

    /** buaAnCu = null là thêm mới, khác null là sửa (kèm nút Xóa). */
    private fun moDialogBuaAn(buaAnCu: BuaAn?) {
        val dialogBinding = DialogThemBuaAnBinding.inflate(layoutInflater)

        if (buaAnCu != null) {
            dialogBinding.etTenMon.setText(buaAnCu.tenMon)
            dialogBinding.etCalo.setText(buaAnCu.calo.toString())
            val viTri = resources.getStringArray(R.array.mang_loai_bua).indexOf(buaAnCu.loaiBua)
            if (viTri >= 0) dialogBinding.spinnerLoaiBua.setSelection(viTri)
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (buaAnCu == null) getString(R.string.tieu_de_them_bua_an)
                else getString(R.string.dd_tieu_de_sua)
            )
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_luu, null)
            .setNegativeButton(R.string.btn_huy, null)
        if (buaAnCu != null) builder.setNeutralButton(R.string.dd_btn_xoa, null)

        val dialog = builder.create()
        // Gắn sự kiện sau khi hiện để dữ liệu sai thì hộp thoại không tự đóng
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val tenMon = dialogBinding.etTenMon.text.toString().trim()
                val calo = dialogBinding.etCalo.text.toString().trim().toIntOrNull()
                val tenHopLe = tenMon.isNotEmpty()
                val caloHopLe = calo != null && calo in 1..5000

                dialogBinding.tilTenMon.error =
                    if (tenHopLe) null else getString(R.string.dd_loi_ten_trong)
                dialogBinding.tilCalo.error =
                    if (caloHopLe) null else getString(R.string.dd_loi_calo)

                if (!tenHopLe || !caloHopLe || calo == null) return@setOnClickListener

                val loaiBua = dialogBinding.spinnerLoaiBua.selectedItem.toString()
                luuBuaAn(buaAnCu, tenMon, loaiBua, calo)
                dialog.dismiss()
            }

            dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
                buaAnCu?.let { xoaBuaAn(it) }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun luuBuaAn(buaAnCu: BuaAn?, tenMon: String, loaiBua: String, calo: Int) {
        val appContext = requireContext().applicationContext
        val thoiGianMoi = thoiGianGhiMoi()
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.layInstance(appContext).buaAnDao()
            if (buaAnCu == null) {
                dao.themBuaAn(
                    BuaAn(tenMon = tenMon, loaiBua = loaiBua, calo = calo, thoiGianGhiNhan = thoiGianMoi)
                )
            } else {
                dao.capNhatBuaAn(buaAnCu.copy(tenMon = tenMon, loaiBua = loaiBua, calo = calo))
            }
            taiLaiDuLieu()
        }
    }

    private fun xoaBuaAn(buaAn: BuaAn) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.layInstance(appContext).buaAnDao()
            dao.xoaBuaAn(buaAn.id)
            taiLaiDuLieu()

            if (_binding == null) return@launch
            Snackbar.make(
                binding.root,
                getString(R.string.dd_da_xoa_bua_an, buaAn.tenMon),
                Snackbar.LENGTH_LONG
            ).setAction(R.string.dd_hoan_tac) {
                viewLifecycleOwner.lifecycleScope.launch {
                    dao.themBuaAn(buaAn) // giữ nguyên id và giờ ghi nhận cũ
                    taiLaiDuLieu()
                }
            }.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private fun chuanHoa(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
    }
}
package com.example.appsk.ui.canhbao

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appsk.R
import com.example.appsk.canhbao.BoPhanTichXuHuong
import com.example.appsk.canhbao.KetQuaPhanTich
import com.example.appsk.canhbao.MucDoCanhBao
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.databinding.ActivityCanhBaoSucKhoeBinding
import com.example.appsk.databinding.ItemCanhBaoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CanhBaoSucKhoeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCanhBaoSucKhoeBinding
    private lateinit var db: AppDatabase
    private val dinhDangNgayGio = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCanhBaoSucKhoeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.layInstance(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnKiemTra.setOnClickListener { kiemTra() }

        kiemTra()
    }

    private fun kiemTra() {
        hienThiDangTai(true)

        lifecycleScope.launch {
            try {
                if (!QuanLyHoSo(this@CanhBaoSucKhoeActivity).daCoHoSo()) {
                    binding.khungHienTai.removeAllViews()
                    binding.tvThieuDuLieu.visibility = View.GONE
                    binding.tvTomTat.text = getString(R.string.can_ho_so_canh_bao)
                } else {
                    val boPhanTich = BoPhanTichXuHuong(applicationContext)
                    val ketQua = boPhanTich.phanTich()
                    boPhanTich.luuCanhBaoMoi(ketQua.canhBao)
                    hienThiKetQua(ketQua)
                }
                napLichSu()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                binding.tvTomTat.text = "Lỗi: ${e.message}"
            } finally {
                hienThiDangTai(false)
            }
        }
    }

    private fun hienThiKetQua(ketQua: KetQuaPhanTich) {
        binding.khungHienTai.removeAllViews()

        binding.tvTomTat.text = if (ketQua.canhBao.isEmpty()) {
            getString(R.string.khong_co_canh_bao)
        } else {
            getString(R.string.co_n_canh_bao, ketQua.canhBao.size)
        }

        ketQua.canhBao.forEach {
            themThe(binding.khungHienTai, it.mucDo, it.tieuDe, it.noiDung, null)
        }

        if (ketQua.ghiChuThieuDuLieu.isNotEmpty()) {
            binding.tvThieuDuLieu.visibility = View.VISIBLE
            binding.tvThieuDuLieu.text = getString(R.string.tieu_de_thieu_du_lieu) + "\n" +
                    ketQua.ghiChuThieuDuLieu.joinToString("\n") { "- $it" }
        } else {
            binding.tvThieuDuLieu.visibility = View.GONE
        }
    }

    private suspend fun napLichSu() {
        val danhSach = db.canhBaoSucKhoeDao().layTatCa().take(40)
        binding.khungLichSu.removeAllViews()
        binding.tvLichSuTrong.visibility = if (danhSach.isEmpty()) View.VISIBLE else View.GONE

        danhSach.forEach {
            themThe(
                binding.khungLichSu,
                it.mucDo,
                it.tieuDe,
                it.noiDung,
                dinhDangNgayGio.format(Date(it.thoiGianTao))
            )
        }
    }

    private fun themThe(
        cha: LinearLayout,
        mucDo: Int,
        tieuDe: String,
        noiDung: String,
        thoiGian: String?
    ) {
        val the = ItemCanhBaoBinding.inflate(layoutInflater, cha, false)
        val mau = MucDoCanhBao.mau(mucDo)

        the.theCanhBao.strokeColor = mau
        the.tvMucDo.text = MucDoCanhBao.ten(mucDo)
        the.tvMucDo.setTextColor(mau)
        the.tvTieuDe.text = tieuDe
        the.tvNoiDung.text = noiDung

        if (thoiGian != null) {
            the.tvThoiGian.visibility = View.VISIBLE
            the.tvThoiGian.text = thoiGian
        } else {
            the.tvThoiGian.visibility = View.GONE
        }

        cha.addView(the.root)
    }

    private fun hienThiDangTai(dangTai: Boolean) {
        binding.progressBar.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnKiemTra.isEnabled = !dangTai
    }
}
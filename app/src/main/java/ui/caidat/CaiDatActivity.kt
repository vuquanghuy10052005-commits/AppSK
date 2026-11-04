package com.example.appsk.ui.caidat

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appsk.BuildConfig
import com.example.appsk.R
import com.example.appsk.data.QuanLyBuocChan
import com.example.appsk.databinding.ActivityCaiDatBinding
import com.example.appsk.dongbo.CaiDatDongBo
import com.example.appsk.dongbo.DongBo
import com.example.appsk.dongbo.DongBoDuLieu
import com.example.appsk.nhacnho.QuanLyNhacNho
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaiDatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaiDatBinding
    private lateinit var quanLyNhacNho: QuanLyNhacNho
    private lateinit var quanLyBuocChan: QuanLyBuocChan

    private val dinhDangNgayGio = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Loại nhắc nhở đang chờ người dùng trả lời hộp thoại xin quyền thông báo
    private var loaiDangChoQuyen: String? = null

    // Tránh kích hoạt sự kiện công tắc khi chính code đang đặt trạng thái
    private var dangNapGiaoDien = false

    private val xinQuyenThongBao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { duocCap ->
        val loai = loaiDangChoQuyen
        loaiDangChoQuyen = null
        if (loai == null) return@registerForActivityResult

        if (duocCap) {
            quanLyNhacNho.datTrangThai(loai, true)
        } else {
            datCongTac(loai, false)
            Toast.makeText(this, getString(R.string.cd_can_quyen_thong_bao), Toast.LENGTH_LONG).show()
        }
        capNhatCanhBaoQuyen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaiDatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quanLyNhacNho = QuanLyNhacNho(this)
        quanLyBuocChan = QuanLyBuocChan(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvPhienBan.text = getString(R.string.cd_phien_ban, BuildConfig.VERSION_NAME)

        napGiaoDien()

        binding.btnLuuMucTieu.setOnClickListener { luuMucTieuBuoc() }
        binding.btnMoCaiDatThongBao.setOnClickListener { moCaiDatThongBaoHeThong() }

        QuanLyNhacNho.TAT_CA_LOAI.forEach { loai ->
            congTac(loai).setOnCheckedChangeListener { _, bat ->
                if (!dangNapGiaoDien) khiDoiCongTac(loai, bat)
            }
        }

        binding.switchDongBo.setOnCheckedChangeListener { _, bat ->
            if (!dangNapGiaoDien) khiDoiCongTacDongBo(bat)
        }
        binding.btnDongBoNgay.setOnClickListener { dongBoNgay() }
        binding.btnXoaDamMay.setOnClickListener { hoiXoaDamMay() }
    }

    override fun onResume() {
        super.onResume()
        capNhatCanhBaoQuyen()
        capNhatTrangThaiDongBo()
    }

    // ------------------------------------------------------------ Hiển thị

    private fun napGiaoDien() {
        dangNapGiaoDien = true
        binding.etMucTieuBuoc.setText(quanLyBuocChan.docMucTieuBuoc().toString())
        QuanLyNhacNho.TAT_CA_LOAI.forEach { loai ->
            congTac(loai).isChecked = quanLyNhacNho.dangBat(loai)
        }
        binding.switchDongBo.isChecked = CaiDatDongBo.dangBat(this)
        dangNapGiaoDien = false
        capNhatTrangThaiDongBo()
    }

    private fun congTac(loai: String): SwitchMaterial = when (loai) {
        QuanLyNhacNho.LOAI_NUOC -> binding.switchNuoc
        QuanLyNhacNho.LOAI_VAN_DONG -> binding.switchVanDong
        else -> binding.switchXuHuong
    }

    private fun datCongTac(loai: String, bat: Boolean) {
        dangNapGiaoDien = true
        congTac(loai).isChecked = bat
        dangNapGiaoDien = false
    }

    // ------------------------------------------------------------ Mục tiêu bước chân

    private fun luuMucTieuBuoc() {
        val soBuoc = binding.etMucTieuBuoc.text.toString().trim().toIntOrNull()
        if (soBuoc == null || soBuoc !in 1000..50000) {
            binding.tilMucTieuBuoc.error = getString(R.string.cd_loi_muc_tieu)
            return
        }
        binding.tilMucTieuBuoc.error = null
        quanLyBuocChan.luuMucTieuBuoc(soBuoc)
        Toast.makeText(this, getString(R.string.cd_da_luu_muc_tieu), Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------------------------ Thông báo

    private fun khiDoiCongTac(loai: String, bat: Boolean) {
        if (!bat) {
            quanLyNhacNho.datTrangThai(loai, false)
            capNhatCanhBaoQuyen()
            return
        }

        if (thieuQuyenThongBao()) {
            loaiDangChoQuyen = loai
            xinQuyenThongBao.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            quanLyNhacNho.datTrangThai(loai, true)
            capNhatCanhBaoQuyen()
        }
    }

    private fun thieuQuyenThongBao(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
    }

    /** Hiện cảnh báo khi có mục đang bật nhưng thông báo của app bị tắt trong hệ thống. */
    private fun capNhatCanhBaoQuyen() {
        val coMucDangBat = QuanLyNhacNho.TAT_CA_LOAI.any { quanLyNhacNho.dangBat(it) }
        val thongBaoDuocPhep = NotificationManagerCompat.from(this).areNotificationsEnabled()
        binding.khungCanhBaoQuyen.visibility =
            if (coMucDangBat && !thongBaoDuocPhep) View.VISIBLE else View.GONE
    }

    private fun moCaiDatThongBaoHeThong() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.cd_khong_mo_duoc_cai_dat), Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------ Đồng bộ đám mây

    private fun capNhatTrangThaiDongBo() {
        val bat = CaiDatDongBo.dangBat(this)
        binding.btnDongBoNgay.isEnabled = bat

        val lanCuoi = CaiDatDongBo.layThoiGianCuoi(this)
        val thongBao = CaiDatDongBo.layThongBao(this)

        binding.tvTrangThaiDongBo.text = when {
            !bat -> getString(R.string.cd_dong_bo_dang_tat)
            lanCuoi == 0L && thongBao.isEmpty() -> getString(R.string.cd_dong_bo_chua_chay)
            else -> {
                val dongThoiGian = if (lanCuoi > 0L) {
                    getString(R.string.cd_dong_bo_lan_cuoi, dinhDangNgayGio.format(Date(lanCuoi)))
                } else ""
                (dongThoiGian + "\n" + thongBao).trim()
            }
        }
    }

    private fun hienThiDangDongBo(dangChay: Boolean) {
        binding.progressDongBo.visibility = if (dangChay) View.VISIBLE else View.GONE
        binding.btnDongBoNgay.isEnabled = !dangChay && CaiDatDongBo.dangBat(this)
        binding.btnXoaDamMay.isEnabled = !dangChay
        binding.switchDongBo.isEnabled = !dangChay
    }

    private fun khiDoiCongTacDongBo(bat: Boolean) {
        CaiDatDongBo.datBat(this, bat)
        if (bat) {
            DongBo.kichHoat(this)
            dongBoNgay()
        } else {
            DongBo.huyLich(this)
            capNhatTrangThaiDongBo()
        }
    }

    private fun dongBoNgay() {
        hienThiDangDongBo(true)
        lifecycleScope.launch {
            try {
                DongBoDuLieu.dongBo(applicationContext)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                hienThiDangDongBo(false)
                // Đồng bộ có thể vừa cập nhật mục tiêu và công tắc nhắc nhở từ máy khác
                napGiaoDien()
            }
        }
    }

    private fun hoiXoaDamMay() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cd_xoa_dam_may_tieu_de)
            .setMessage(R.string.cd_xoa_dam_may_noi_dung)
            .setPositiveButton(R.string.dd_btn_xoa) { _, _ -> xoaDamMay() }
            .setNegativeButton(R.string.btn_huy, null)
            .show()
    }

    private fun xoaDamMay() {
        // Tắt đồng bộ trước để dữ liệu vừa xóa không bị gửi lên lại
        CaiDatDongBo.datBat(this, false)
        DongBo.huyLich(this)
        dangNapGiaoDien = true
        binding.switchDongBo.isChecked = false
        dangNapGiaoDien = false

        hienThiDangDongBo(true)
        lifecycleScope.launch {
            try {
                val ketQua = DongBoDuLieu.xoaDuLieuDamMay(applicationContext)
                Toast.makeText(this@CaiDatActivity, ketQua.thongBao, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                hienThiDangDongBo(false)
                capNhatTrangThaiDongBo()
            }
        }
    }
}
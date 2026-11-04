package com.example.appsk.ui.vandong

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appsk.ChayBoActivity
import com.example.appsk.R
import com.example.appsk.buocchan.GhiNhanBuocChan
import com.example.appsk.data.QuanLyBuocChan
import com.example.appsk.databinding.FragmentVanDongBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class VanDongFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentVanDongBinding? = null
    private val binding get() = _binding!!

    private lateinit var quanLyBuocChan: QuanLyBuocChan
    private var sensorManager: SensorManager? = null
    private var cambienBuocChan: Sensor? = null

    private val xinQuyen = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { duocCap ->
        if (duocCap && _binding != null) {
            binding.tvCanhBao.visibility = View.GONE
            binding.btnCapQuyen.visibility = View.GONE
            binding.khungSoBuoc.visibility = View.VISIBLE
            dangKyCamBien()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVanDongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quanLyBuocChan = QuanLyBuocChan(requireContext())
        sensorManager = requireContext().getSystemService(SensorManager::class.java)
        cambienBuocChan = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val mucTieu = quanLyBuocChan.docMucTieuBuoc()
        binding.tvMucTieu.text = getString(R.string.nhan_muc_tieu_buoc, mucTieu)

        binding.btnCapQuyen.setOnClickListener {
            xinQuyen.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        binding.btnMoChayBo.setOnClickListener {
            startActivity(Intent(requireContext(), ChayBoActivity::class.java))
        }

        binding.btnHuongDanPin.setOnClickListener {
            moHuongDanPin()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvMucTieu.text =
            getString(R.string.nhan_muc_tieu_buoc, quanLyBuocChan.docMucTieuBuoc())
        kiemTraCamBienVaQuyen()
        hienThiSoBuocDaLuu()
    }
    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    private fun kiemTraCamBienVaQuyen() {
        if (cambienBuocChan == null) {
            binding.tvCanhBao.text = getString(R.string.loi_thieu_cam_bien)
            binding.tvCanhBao.visibility = View.VISIBLE
            binding.khungSoBuoc.visibility = View.GONE
            return
        }

        val coQuyen = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED

        if (!coQuyen) {
            binding.tvCanhBao.text = getString(R.string.loi_thieu_quyen_van_dong)
            binding.tvCanhBao.visibility = View.VISIBLE
            binding.btnCapQuyen.visibility = View.VISIBLE
            binding.khungSoBuoc.visibility = View.GONE
        } else {
            binding.tvCanhBao.visibility = View.GONE
            binding.btnCapQuyen.visibility = View.GONE
            binding.khungSoBuoc.visibility = View.VISIBLE
            dangKyCamBien()
        }
    }

    private fun dangKyCamBien() {
        cambienBuocChan?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /** Hiện ngay số bước đã lưu, không cần chờ cảm biến báo về. */
    private fun hienThiSoBuocDaLuu() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val tong = GhiNhanBuocChan.docSoBuocHomNay(appContext)
            if (_binding != null) capNhatGiaoDien(tong)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val giaTri = event?.values?.get(0)?.toLong() ?: return
        val appContext = context?.applicationContext ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val tong = GhiNhanBuocChan.ghiNhan(appContext, giaTri)
            if (_binding != null) capNhatGiaoDien(tong)
        }
    }

    private fun capNhatGiaoDien(soBuoc: Int) {
        val mucTieu = quanLyBuocChan.docMucTieuBuoc()
        binding.tvSoBuoc.text = soBuoc.toString()
        binding.vongTronTienDo.progress =
            ((soBuoc.toFloat() / mucTieu) * 100).roundToInt().coerceIn(0, 100)

        val quangDuongKm = (soBuoc * 0.75f) / 1000f
        val caloTieuHao = soBuoc * 0.04f

        binding.tvGiaTriQuangDuong.text = String.format("%.2f km", quangDuongKm)
        binding.tvGiaTriCalo.text = "${caloTieuHao.roundToInt()} kcal"
    }

    private fun moHuongDanPin() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tieu_de_huong_dan_pin)
            .setMessage(R.string.noi_dung_huong_dan_pin)
            .setPositiveButton(R.string.btn_mo_cai_dat_pin) { _, _ ->
                moCaiDat(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            .setNeutralButton(R.string.btn_thong_tin_ung_dung) { _, _ ->
                moCaiDat(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                )
            }
            .setNegativeButton(R.string.btn_dong, null)
            .show()
    }

    private fun moCaiDat(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.loi_mo_cai_dat), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
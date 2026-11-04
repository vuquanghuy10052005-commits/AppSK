package com.example.appsk.ui.lichsu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appsk.data.DinhDang
import com.example.appsk.data.LoaiHoatDong
import com.example.appsk.data.csdl.LichSuChayBo
import com.example.appsk.databinding.ItemBuoiTapBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BuoiTapHienThi(
    val phien: LichSuChayBo,
    val loai: String?
)

class BuoiTapAdapter(
    private val khiBam: (Long) -> Unit
) : RecyclerView.Adapter<BuoiTapAdapter.BuoiTapViewHolder>() {

    private var danhSach: List<BuoiTapHienThi> = emptyList()
    private val dinhDangNgayGio = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class BuoiTapViewHolder(val binding: ItemBuoiTapBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuoiTapViewHolder {
        val binding = ItemBuoiTapBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuoiTapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuoiTapViewHolder, position: Int) {
        val muc = danhSach[position]
        val phien = muc.phien

        with(holder.binding) {
            tvBieuTuong.text = LoaiHoatDong.bieuTuong(muc.loai)
            tvTenLoai.text = LoaiHoatDong.ten(muc.loai)
            tvThoiDiem.text = dinhDangNgayGio.format(Date(phien.thoiGianGhiNhan))
            tvQuangDuong.text = String.format(Locale.getDefault(), "%.2f km", phien.quangDuongKm)
            tvChiTiet.text = DinhDang.thoiLuong(phien.thoiGianGiay) + " • " +
                    String.format(Locale.getDefault(), "%.1f km/h", phien.tocDoTrungBinh)
            root.setOnClickListener { khiBam(phien.id) }
        }
    }

    override fun getItemCount() = danhSach.size

    fun capNhat(danhSachMoi: List<BuoiTapHienThi>) {
        danhSach = danhSachMoi
        notifyDataSetChanged()
    }
}
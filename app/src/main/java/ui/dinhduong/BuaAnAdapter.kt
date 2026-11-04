package com.example.appsk.ui.dinhduong

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appsk.data.csdl.BuaAn
import com.example.appsk.databinding.ItemBuaAnBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BuaAnAdapter(
    private val khiBam: (BuaAn) -> Unit
) : RecyclerView.Adapter<BuaAnAdapter.BuaAnViewHolder>() {

    private var danhSach: List<BuaAn> = emptyList()
    private val dinhDangGio = SimpleDateFormat("HH:mm", Locale.getDefault())

    class BuaAnViewHolder(val binding: ItemBuaAnBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuaAnViewHolder {
        val binding = ItemBuaAnBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuaAnViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuaAnViewHolder, position: Int) {
        val buaAn = danhSach[position]
        with(holder.binding) {
            tvTenMon.text = buaAn.tenMon
            tvLoaiBua.text = "${buaAn.loaiBua} • ${dinhDangGio.format(Date(buaAn.thoiGianGhiNhan))}"
            tvCalo.text = "${buaAn.calo} kcal"
            root.setOnClickListener { khiBam(buaAn) }
        }
    }

    override fun getItemCount() = danhSach.size

    fun capNhatDanhSach(danhSachMoi: List<BuaAn>) {
        danhSach = danhSachMoi
        notifyDataSetChanged()
    }
}
package com.example.appsk.ui.anhmonan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appsk.databinding.ItemMonNhanDienBinding
import com.example.appsk.thucdon.MonNhanDien

class MonNhanDienAdapter(
    private val khiBam: (Int) -> Unit
) : RecyclerView.Adapter<MonNhanDienAdapter.MonViewHolder>() {

    private var danhSach: List<MonNhanDien> = emptyList()

    class MonViewHolder(val binding: ItemMonNhanDienBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonViewHolder {
        val binding = ItemMonNhanDienBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonViewHolder, position: Int) {
        val mon = danhSach[position]
        with(holder.binding) {
            tvTen.text = mon.ten
            tvKhauPhan.text = if (mon.khauPhan.isBlank()) "—" else mon.khauPhan
            tvCalo.text = "${mon.calo} kcal"
            root.setOnClickListener {
                val viTri = holder.bindingAdapterPosition
                if (viTri != RecyclerView.NO_POSITION) khiBam(viTri)
            }
        }
    }

    override fun getItemCount() = danhSach.size

    fun capNhat(danhSachMoi: List<MonNhanDien>) {
        danhSach = danhSachMoi
        notifyDataSetChanged()
    }
}
package com.example.appsk.ui.trolyai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appsk.databinding.ItemTinNhanAiBinding
import com.example.appsk.databinding.ItemTinNhanNguoiDungBinding

class TinNhanAdapter(private val danhSach: MutableList<TinNhan>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val KIEU_NGUOI_DUNG = 0
        private const val KIEU_AI = 1
    }

    inner class ViewHolderNguoiDung(val binding: ItemTinNhanNguoiDungBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ViewHolderAi(val binding: ItemTinNhanAiBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (danhSach[position].laCuaNguoiDung) KIEU_NGUOI_DUNG else KIEU_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == KIEU_NGUOI_DUNG) {
            ViewHolderNguoiDung(ItemTinNhanNguoiDungBinding.inflate(inflater, parent, false))
        } else {
            ViewHolderAi(ItemTinNhanAiBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val tinNhan = danhSach[position]
        when (holder) {
            is ViewHolderNguoiDung -> holder.binding.tvNoiDung.text = tinNhan.noiDung
            is ViewHolderAi -> holder.binding.tvNoiDung.text = tinNhan.noiDung
        }
    }

    override fun getItemCount() = danhSach.size

    fun themTinNhan(tinNhan: TinNhan) {
        danhSach.add(tinNhan)
        notifyItemInserted(danhSach.size - 1)
    }
}
package com.example.appsk.canhbao

import android.graphics.Color

object MucDoCanhBao {
    const val NHE = 1
    const val VUA = 2
    const val CAO = 3

    fun ten(mucDo: Int): String = when (mucDo) {
        CAO -> "Nên quan tâm"
        VUA -> "Cần chú ý"
        else -> "Lưu ý nhẹ"
    }

    fun mau(mucDo: Int): Int = when (mucDo) {
        CAO -> Color.parseColor("#C62828")
        VUA -> Color.parseColor("#EF6C00")
        else -> Color.parseColor("#C77700")
    }
}
package com.example.appsk.thoitiet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object OpenMeteoClient {

    suspend fun layThoiTiet(viDo: Double, kinhDo: Double): ThoiTiet = withContext(Dispatchers.IO) {
        val duongDan = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                    "precipitation,weather_code,wind_speed_10m" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&forecast_days=1&timezone=auto",
            viDo, kinhDo
        )

        val ketNoi = URL(duongDan).openConnection() as HttpURLConnection
        try {
            ketNoi.requestMethod = "GET"
            ketNoi.connectTimeout = 10000
            ketNoi.readTimeout = 10000

            if (ketNoi.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Máy chủ thời tiết trả về mã ${ketNoi.responseCode}")
            }

            val noiDung = ketNoi.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(noiDung)
            val hienTai = json.getJSONObject("current")
            val homNay = json.getJSONObject("daily")

            ThoiTiet(
                nhietDo = hienTai.getDouble("temperature_2m"),
                camGiac = hienTai.getDouble("apparent_temperature"),
                doAm = hienTai.getInt("relative_humidity_2m"),
                luongMua = hienTai.getDouble("precipitation"),
                maThoiTiet = hienTai.getInt("weather_code"),
                tocDoGio = hienTai.getDouble("wind_speed_10m"),
                nhietDoCaoNhat = homNay.getJSONArray("temperature_2m_max").getDouble(0),
                nhietDoThapNhat = homNay.getJSONArray("temperature_2m_min").getDouble(0),
                xacSuatMua = homNay.getJSONArray("precipitation_probability_max").optInt(0)
            )
        } finally {
            ketNoi.disconnect()
        }
    }
}
package com.example.appsk.buocchan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Mỗi ~15 phút đọc cảm biến bước chân một lần ở chế độ nền rồi cộng phần chênh vào ngày hôm nay.
 */
class LayMauBuocChanWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        private const val TEN_WORK = "lay_mau_buoc_chan"

        fun lenLich(context: Context) {
            val yeuCau = PeriodicWorkRequestBuilder<LayMauBuocChanWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TEN_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                yeuCau
            )
        }
    }

    override suspend fun doWork(): Result {
        val coQuyen = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
        if (!coQuyen) return Result.success()

        val giaTri = docCamBienMotLan() ?: return Result.success()
        GhiNhanBuocChan.ghiNhan(applicationContext, giaTri)
        return Result.success()
    }

    private suspend fun docCamBienMotLan(): Long? {
        val sensorManager = applicationContext.getSystemService(SensorManager::class.java)
            ?: return null
        val camBien = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine<Long> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val giaTri = event?.values?.get(0)?.toLong() ?: return
                        sensorManager.unregisterListener(this)
                        if (cont.isActive) cont.resume(giaTri)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, camBien, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }
        }
    }
}
package com.ultimatetool.app.presentation.tools

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.ultimatetool.app.core.util.formatBytes
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.ScreenScaffold

// ---------- QR-генератор ----------

@Composable
fun QrGeneratorScreen(onBack: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold("QR-код", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it.take(2_000)
                    error = null
                },
                label = { Text("Текст или ссылка") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    bitmap = null
                    if (text.isBlank()) {
                        error = "Введите текст"
                    } else {
                        try {
                            val size = 512
                            val hints = mapOf<EncodeHintType, Any>(
                                EncodeHintType.MARGIN to 1,
                                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
                            )
                            val matrix = QRCodeWriter()
                                .encode(text, BarcodeFormat.QR_CODE, size, size, hints)
                            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                                for (x in 0 until size) {
                                    for (y in 0 until size) {
                                        setPixel(
                                            x, y,
                                            if (matrix[x, y]) android.graphics.Color.BLACK
                                            else android.graphics.Color.WHITE
                                        )
                                    }
                                }
                            }
                            error = null
                        } catch (e: Exception) {
                            error = "Не удалось создать QR: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сгенерировать") }

            error?.let { ErrorBanner(it, Modifier.fillMaxWidth()) }

            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "QR-код",
                    modifier = Modifier.size(280.dp)
                )
            }
        }
    }
}

// ---------- Информация об устройстве ----------

private fun collectDeviceInfo(context: Context): List<Pair<String, String>> {
    val dm = context.resources.displayMetrics
    val rows = mutableListOf(
        "Модель" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Устройство" to Build.DEVICE,
        "Android" to "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        "ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
        "Экран" to "${dm.widthPixels}×${dm.heightPixels} @ ${dm.densityDpi} dpi",
        "Ядра CPU" to Runtime.getRuntime().availableProcessors().toString()
    )
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        rows += "RAM всего" to formatBytes(memInfo.totalMem)
        rows += "RAM свободно" to formatBytes(memInfo.availMem)
    } catch (_: Exception) {
    }
    try {
        val stat = StatFs(Environment.getDataDirectory().path)
        rows += "Память данных всего" to formatBytes(stat.totalBytes)
        rows += "Память данных свободно" to formatBytes(stat.availableBytes)
    } catch (_: Exception) {
    }
    rows += "Аптайм" to "${SystemClock.elapsedRealtime() / 60_000} мин"
    return rows
}

@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val rows = remember { collectDeviceInfo(context) }
    var battery by remember { mutableStateOf("—") }

    // Sticky-broadcast батареи: регистрируем приёмник и снимаем его при выходе.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                battery = describeBattery(intent)
            }
        }
        var sticky: Intent? = null
        try {
            sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
        }
        battery = describeBattery(sticky)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    ScreenScaffold("Устройство", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow("Батарея", battery)
            rows.forEach { (label, value) -> InfoRow(label, value) }
        }
    }
}

private fun describeBattery(intent: Intent?): String {
    if (intent == null) return "нет данных"
    return try {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        if (percent >= 0) "$percent%${if (charging) ", заряжается" else ""}" else "нет данных"
    } catch (_: Exception) {
        "нет данных"
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.55f)
        )
    }
}

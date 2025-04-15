/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthconnectsample.presentation.screen.allInfo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.healthconnectsample.R
import com.example.healthconnectsample.data.SleepSessionData
import com.example.healthconnectsample.data.BloodOxygenData
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp

// Определение enum для типов данных
enum class DataType(val typeName: String) {
    SLEEP_SESSION_DATA("SleepSessionData"), BLOOD_OXYGEN_DATA("BloodOxygenData"), HEART_RATE_RECORD(
        "HeartRateRecord"
    ),
    ACTIVE_CALORIES_BURNED_RECORD("ActiveCaloriesBurnedRecord"), BASAL_METABOLIC_RATE_RECORD("BasalMetabolicRateRecord"), BLOOD_PRESSURE_RECORD(
        "BloodPressureRecord"
    ),
    BODY_FAT_RECORD("BodyFatRecord"), BODY_TEMPERATURE_RECORD("BodyTemperatureRecord"), BONE_MASS_RECORD(
        "BoneMassRecord"
    ),
    DISTANCE_RECORD("DistanceRecord"), EXERCISE_SESSION_RECORD("ExerciseSessionRecord"), HYDRATION_RECORD(
        "HydrationRecord"
    ),
    SPEED_RECORD("SpeedRecord"), STEPS_RECORD("StepsRecord"), TOTAL_CALORIES_BURNED_RECORD("TotalCaloriesBurnedRecord"), WEIGHT_RECORD(
        "WeightRecord"
    ),
    BASAL_BODY_TEMPERATURE_RECORD("BasalBodyTemperatureRecord"), FLOORS_CLIMBED_RECORD("FloorsClimbedRecord"), INTERMENSTRUAL_BLEEDING_RECORD(
        "IntermenstrualBleedingRecord"
    ),
    LEAN_BODY_MASS_RECORD("LeanBodyMassRecord"), MENSTRUATION_FLOW_RECORD("MenstruationFlowRecord"), NUTRITION_RECORD(
        "NutritionRecord"
    ),
    POWER_RECORD("PowerRecord"), RESPIRATORY_RATE_RECORD("RespiratoryRateRecord"), RESTING_HEART_RATE_RECORD(
        "RestingHeartRateRecord"
    ),
    SKIN_TEMPERATURE_RECORD("SkinTemperatureRecord")
}


object GlobalConfig {
    var config: ConfigData? = null
}

// Модель для конфигурационных данных, получаемых по URL из QR-кода.
data class ConfigData(
    val post_here: String,
    val access_token: String,
    val refresh_token: String,
    val refresh_token_url: String,
    val token_type: String
)


fun <T> exportHealthDataInBackground(
    dataList: List<T>, dataType: DataType, onProgressUpdate: (completed: Int, total: Int) -> Unit
) {
    GlobalConfig.config?.let { config ->
        CoroutineScope(Dispatchers.IO).launch {
            // Вызываем suspend функцию exportDataInBatches из корутины
            exportDataInBatches(dataType, dataList, config, onProgressUpdate)
        }
    }
}

data class ExportProgressInfo(
    val dataType: DataType,
    val currentCount: Int,
    val totalCount: Int,
    val dataList: List<Any>
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AllInfoScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    uiState: SleepSessionViewModel.UiState,
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    sleepSessionsList: List<SleepSessionData>,
    bloodOxygenList: List<BloodOxygenData>,
    heartRateList: List<HeartRateRecord>,
    activeCaloriesList: List<ActiveCaloriesBurnedRecord>,
    basalMetabolicRateList: List<BasalMetabolicRateRecord>,
    bloodPressureList: List<BloodPressureRecord>,
    bodyFatList: List<BodyFatRecord>,
    bodyTemperatureList: List<BodyTemperatureRecord>,
    boneMassList: List<BoneMassRecord>,
    distanceList: List<DistanceRecord>,
    exerciseSessionList: List<ExerciseSessionRecord>,
    hydrationList: List<HydrationRecord>,
    speedList: List<SpeedRecord>,
    stepsList: List<StepsRecord>,
    totalCaloriesBurnedList: List<TotalCaloriesBurnedRecord>,
    weightList: List<WeightRecord>,
    basalBodyTemperatureList: List<BasalBodyTemperatureRecord>,
    floorsClimbedList: List<FloorsClimbedRecord>,
    intermenstrualBleedingList: List<IntermenstrualBleedingRecord>,
    leanBodyMassList: List<LeanBodyMassRecord>,
    menstruationFlowList: List<MenstruationFlowRecord>,
    nutritionList: List<NutritionRecord>,
    powerList: List<PowerRecord>,
    respiratoryRateList: List<RespiratoryRateRecord>,
    restingHeartRateList: List<RestingHeartRateRecord>,
    skinTemperatureList: List<SkinTemperatureRecord>
) {
    val context = LocalContext.current

    // Стейт для отслеживания прогресса экспорта
    var exportProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Обработка ошибок для загрузки данных Health Connect
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }
    LaunchedEffect(uiState) {
        if (uiState is SleepSessionViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }
        if (uiState is SleepSessionViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    // Состояния для QR‑код сканера, результата HTTP‑запроса и экспорта
    var qrCodeText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var httpResponse by remember { mutableStateOf<String?>(null) }
    var isCameraMode by remember { mutableStateOf(false) }

    // Состояние разрешения для камеры
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    // Основной контейнер – Box: здесь основной контент и внизу прогресс бар
    Box(modifier = Modifier.fillMaxSize()) {
        // Основной контент
        if (!isCameraMode) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp), // отступ снизу, чтобы прогресс-бар не накладывался
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                if (!permissionsGranted) {
                    item {
                        Button(
                            onClick = { onPermissionsLaunch(permissions) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(text = stringResource(R.string.permissions_button_label))
                        }
                    }
                }
                // Кнопка "Открыть камеру"
                item {
                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                isCameraMode = true
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = "Открыть камеру")
                    }
                }
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    item {
                        Button(
                            onClick = {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(text = "Предоставить разрешение в настройках")
                        }
                    }
                }
                // Вывод ранее полученной информации
                if (qrCodeText != null) {
                    item {
                        Text(
                            text = "Информация с QR-кода: $qrCodeText",
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (errorMessage != null) {
                    item {
                        Text(
                            text = "Ошибка: $errorMessage",
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (httpResponse != null && GlobalConfig.config != null) {
                    item {
                        Text(
                            text = "Ответ с сервера: ${GlobalConfig.config}",
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Режим сканера: превью камеры и кнопка "Назад"
            Box(modifier = Modifier.fillMaxSize()) {
                CameraScannerScreen(
                    onQRCodeScanned = { result ->
                        if (result.isNullOrEmpty() || !isValidUrl(result)) {
                            errorMessage = "Невалидный URL"
                            isCameraMode = false
                        } else {
                            qrCodeText = result
                            sendRequestToUrl(result) { response ->
                                try {
                                    val config = parseConfig(response)
                                    if (config == null) {
                                        errorMessage = "Некорректный формат ответа от сервера"
                                    } else {
                                        httpResponse = response
                                        GlobalConfig.config = config

                                        // Запускаем экспорт, например, для heartRateList.
                                        // Передаем callback для обновления прогресса.
                                        exportHealthDataInBackground(
                                            heartRateList,
                                            dataType = DataType.HEART_RATE_RECORD
                                        ) { completed, total ->
                                            exportProgress = completed to total
                                            // При достижении полного экспорта можно выполнить дополнительные действия
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка обработки ответа: ${e.localizedMessage}"
                                }
                            }
                            isCameraMode = false
                        }
                    }
                )
                Button(
                    onClick = { isCameraMode = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Text(text = "Назад")
                }
            }
        }

        // Прогресс-бар размещён в нижней части экрана (по оси Y)
        if (exportProgress != null) {
            // Размещаем прогресс-индикатор у нижней границы Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (completed, total) = exportProgress!!
                val progressFraction = completed.toFloat() / total
                LinearProgressIndicator(
                    progress = progressFraction,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Выгружено $completed из $total записей",
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        }
    }
}


/**
 * Проверяет, является ли строка валидным URL.
 */
fun isValidUrl(url: String): Boolean {
    return Patterns.WEB_URL.matcher(url).matches()
}

/**
 * Выполняет HTTP‑GET‑запрос по данному URL.
 */
fun sendRequestToUrl(url: String, onResult: (String) -> Unit) {
    val client = OkHttpClient()
    try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult("Ошибка запроса: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "Пустой ответ"
                onResult(body)
            }
        })
    } catch (e: Exception) {
        onResult("Некорректный URL или ошибка: ${e.localizedMessage}")
    }
}

/**
 * Парсит JSON-ответ в объект ConfigData.
 */
fun parseConfig(response: String): ConfigData? {
    return try {
        Gson().fromJson(response, ConfigData::class.java)
    } catch (e: Exception) {
        null
    }
}

/**
 * Экспортирует данные пачками по 50 элементов.
 */
suspend fun <T> exportDataInBatches(
    dataType: DataType,
    dataList: List<T>,
    config: ConfigData,
    onProgressUpdate: (completed: Int, total: Int) -> Unit
) {
    val gson = Gson()
    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    val client = OkHttpClient()

    val total = dataList.size
    var completed = 0

    // Логируем общее количество записей
    Log.d("ExportData", "Экспортируется $total записей для типа ${dataType.typeName}")

    // Разбиваем список на пачки по 50
    val batches = dataList.chunked(50)
    for (batch in batches) {
        val endpoint = "${config.post_here}/${dataType.typeName}"
        val jsonBody = gson.toJson(batch)
        val requestBody = jsonBody.toRequestBody(jsonMediaType)
        val request = Request.Builder().url(endpoint).post(requestBody).build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d("ExportData", "Пачка успешно выгружена: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("ExportData", "Ошибка при выгрузке пачки: ${e.localizedMessage}")
            // Вы можете добавить логику по повторной попытке или остановке экспорта
        }

        completed += batch.size

        // Вызываем withContext, так как эта функция теперь вызывается из suspend‑функции
        withContext(Dispatchers.Main) {
            onProgressUpdate(completed, total)
        }
    }
}


/**
 * Отображает превью камеры и запускает обработку кадров для сканирования QR‑кода.
 */
@Composable
fun CameraScannerScreen(
    onQRCodeScanned: (String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    var scannedResult by remember { mutableStateOf<String?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    bindCameraUseCases(previewView, lifecycleOwner) { result ->
                        result?.let {
                            scannedResult = it
                            onQRCodeScanned(it)
                        }
                    }
                }
            }, modifier = Modifier.fillMaxSize()
        )
        scannedResult?.let { result ->
            Text(
                text = "Найден QR-код: $result", modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Привязывает CameraX к жизненному циклу и запускает анализ кадров с помощью ML Kit.
 */
private fun bindCameraUseCases(
    previewView: PreviewView, lifecycleOwner: LifecycleOwner?, onBarcodeFound: (String?) -> Unit
) {
    val context = previewView.context
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageAnalysis =
            ImageAnalysis.Builder().setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST).build()
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy ->
            processImageProxy(imageProxy, onBarcodeFound)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner!!, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

/**
 * Обрабатывает кадр с камеры, пытаясь извлечь QR‑код с помощью ML Kit.
 */
private fun processImageProxy(
    imageProxy: ImageProxy, onBarcodeFound: (String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image).addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
                onBarcodeFound(barcodes.first().rawValue)
            } else {
                onBarcodeFound(null)
            }
        }.addOnFailureListener { e ->
            e.printStackTrace()
            onBarcodeFound(null)
        }.addOnCompleteListener {
            imageProxy.close()
        }
    } else {
        imageProxy.close()
    }
}
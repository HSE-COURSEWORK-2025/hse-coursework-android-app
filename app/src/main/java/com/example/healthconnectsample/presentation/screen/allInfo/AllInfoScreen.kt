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
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

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

// Модель для конфигурационных данных, получаемых по URL из QR‑кода.
data class ConfigData(
    val post_here: String,
    val access_token: String,
    val refresh_token: String,
    val refresh_token_url: String,
    val token_type: String
)

// Функция для экспорта данных в фоне для переданного списка
fun <T> exportHealthDataInBackground(
    dataList: List<T>, dataType: DataType, onProgressUpdate: (completed: Int, total: Int) -> Unit
) {
    GlobalConfig.config?.let { config ->
        CoroutineScope(Dispatchers.IO).launch {
            exportDataInBatches(dataType, dataList, config, onProgressUpdate)
        }
    }
}

data class SampleRecord(
    val value: String, val time: String
)


/**
 * Показывает диалоговое окно с предупреждением
 */
@Composable
fun WarningDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Предупреждение") },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK")
            }
        })
}

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

    val sleepSessionsListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val bloodOxygenListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val heartRateListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val activeCaloriesListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val basalMetabolicRateListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val bloodPressureListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val bodyFatListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val bodyTemperatureListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val boneMassListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val distanceListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val exerciseSessionListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val hydrationListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val speedListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val stepsListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val totalCaloriesBurnedListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val weightListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val basalBodyTemperatureListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val floorsClimbedListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val intermenstrualBleedingListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val leanBodyMassListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val menstruationFlowListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val nutritionListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val powerListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val respiratoryRateListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val restingHeartRateListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val skinTemperatureListProcessed = remember { mutableStateListOf<SampleRecord>() }


    LaunchedEffect(
        sleepSessionsList,
        bloodOxygenList,
        heartRateList,
        activeCaloriesList,
        basalMetabolicRateList,
        bloodPressureList,
        bodyFatList,
        bodyTemperatureList,
        boneMassList,
        distanceList,
        exerciseSessionList,
        hydrationList,
        speedList,
        stepsList,
        totalCaloriesBurnedList,
        weightList,
        basalBodyTemperatureList,
        floorsClimbedList,
        intermenstrualBleedingList,
        leanBodyMassList,
        menstruationFlowList,
        nutritionList,
        powerList,
        respiratoryRateList,
        restingHeartRateList,
        skinTemperatureList
    ) {
        // SleepSessionData
        sleepSessionsListProcessed.clear()
        sleepSessionsList.forEach { rec ->
            sleepSessionsListProcessed.add(
                SampleRecord(value = rec.duration.toString(), time = rec.startTime.toString())
            )
        }
        // BloodOxygenData
        bloodOxygenListProcessed.clear()
        bloodOxygenList.forEach { rec ->
            bloodOxygenListProcessed.add(
                SampleRecord(value = rec.value, time = rec.startTime.toString())
            )
        }
        // HeartRateRecord
        heartRateListProcessed.clear()
        heartRateList.forEach { record ->
            record.samples.forEach { sample ->
                heartRateListProcessed.add(
                    SampleRecord(
                        value = sample.beatsPerMinute.toString(), time = sample.time.toString()
                    )
                )
            }
        }
        // ActiveCaloriesBurnedRecord
        activeCaloriesListProcessed.clear()
        activeCaloriesList.forEach { rec ->
            activeCaloriesListProcessed.add(
                SampleRecord(
                    value = rec.energy.inKilocalories.toString(), time = rec.startTime.toString()
                )
            )
        }
        // BasalMetabolicRateRecord
        basalMetabolicRateListProcessed.clear()
        basalMetabolicRateList.forEach { rec ->
            basalMetabolicRateListProcessed.add(
                SampleRecord(value = rec.basalMetabolicRate.toString(), time = rec.time.toString())
            )
        }
        // BloodPressureRecord
        bloodPressureListProcessed.clear()
        bloodPressureList.forEach { rec ->
            bloodPressureListProcessed.add(
                SampleRecord(value = "${rec.systolic}/${rec.diastolic}", time = rec.time.toString())
            )
        }
        // BodyFatRecord
        bodyFatListProcessed.clear()
        bodyFatList.forEach { rec ->
            bodyFatListProcessed.add(
                SampleRecord(value = rec.percentage.toString(), time = rec.time.toString())
            )
        }
        // BodyTemperatureRecord
        bodyTemperatureListProcessed.clear()
        bodyTemperatureList.forEach { rec ->
            bodyTemperatureListProcessed.add(
                SampleRecord(value = rec.temperature.toString(), time = rec.time.toString())
            )
        }
        // BoneMassRecord
        boneMassListProcessed.clear()
        boneMassList.forEach { rec ->
            boneMassListProcessed.add(
                SampleRecord(value = rec.mass.inKilograms.toString(), time = rec.time.toString())
            )
        }
        // DistanceRecord
        distanceListProcessed.clear()
        distanceList.forEach { rec ->
            distanceListProcessed.add(
                SampleRecord(value = rec.distance.toString(), time = rec.startTime.toString())
            )
        }
        // ExerciseSessionRecord
        exerciseSessionListProcessed.clear()
        exerciseSessionList.forEach { rec ->
            exerciseSessionListProcessed.add(
                SampleRecord(value = rec.title.toString(), time = rec.startTime.toString())
            )
        }
        // HydrationRecord
        hydrationListProcessed.clear()
        hydrationList.forEach { rec ->
            hydrationListProcessed.add(
                SampleRecord(value = rec.volume.toString(), time = rec.startTime.toString())
            )
        }
        // SpeedRecord
        speedListProcessed.clear()
        speedList.forEach { rec ->
            speedListProcessed.add(
                SampleRecord(value = rec.samples.toString(), time = rec.startTime.toString())
            )
        }
        // StepsRecord
        stepsListProcessed.clear()
        stepsList.forEach { rec ->
            stepsListProcessed.add(
                SampleRecord(value = rec.count.toString(), time = rec.startTime.toString())
            )
        }
        // TotalCaloriesBurnedRecord
        totalCaloriesBurnedListProcessed.clear()
        totalCaloriesBurnedList.forEach { rec ->
            totalCaloriesBurnedListProcessed.add(
                SampleRecord(
                    value = rec.energy.inKilocalories.toString(), time = rec.startTime.toString()
                )
            )
        }
        // WeightRecord
        weightListProcessed.clear()
        weightList.forEach { rec ->
            weightListProcessed.add(
                SampleRecord(value = rec.weight.toString(), time = rec.time.toString())
            )
        }
        // BasalBodyTemperatureRecord
        basalBodyTemperatureListProcessed.clear()
        basalBodyTemperatureList.forEach { rec ->
            basalBodyTemperatureListProcessed.add(
                SampleRecord(value = rec.temperature.toString(), time = rec.time.toString())
            )
        }
        // FloorsClimbedRecord
        floorsClimbedListProcessed.clear()
        floorsClimbedList.forEach { rec ->
            floorsClimbedListProcessed.add(
                SampleRecord(value = rec.floors.toString(), time = rec.startTime.toString())
            )
        }
        // IntermenstrualBleedingRecord
        intermenstrualBleedingListProcessed.clear()
        intermenstrualBleedingList.forEach { rec ->
            intermenstrualBleedingListProcessed.add(
                SampleRecord(value = rec.zoneOffset.toString(), time = rec.time.toString())
            )
        }
        // LeanBodyMassRecord
        leanBodyMassListProcessed.clear()
        leanBodyMassList.forEach { rec ->
            leanBodyMassListProcessed.add(
                SampleRecord(value = rec.mass.toString(), time = rec.time.toString())
            )
        }
        // MenstruationFlowRecord
        menstruationFlowListProcessed.clear()
        menstruationFlowList.forEach { rec ->
            menstruationFlowListProcessed.add(
                SampleRecord(value = rec.flow.toString(), time = rec.time.toString())
            )
        }
        // NutritionRecord
        nutritionListProcessed.clear()
        nutritionList.forEach { rec ->
            nutritionListProcessed.add(
                SampleRecord(value = rec.energy.toString(), time = rec.startTime.toString())
            )
        }
        // PowerRecord
        powerListProcessed.clear()
        powerList.forEach { rec ->
            powerListProcessed.add(
                SampleRecord(value = rec.samples.toString(), time = rec.startTime.toString())
            )
        }
        // RespiratoryRateRecord
        respiratoryRateListProcessed.clear()
        respiratoryRateList.forEach { rec ->
            respiratoryRateListProcessed.add(
                SampleRecord(value = rec.rate.toString(), time = rec.time.toString())
            )
        }
        // RestingHeartRateRecord
        restingHeartRateListProcessed.clear()
        restingHeartRateList.forEach { rec ->
            restingHeartRateListProcessed.add(
                SampleRecord(value = rec.beatsPerMinute.toString(), time = rec.time.toString())
            )
        }
        // SkinTemperatureRecord
        skinTemperatureListProcessed.clear()
        skinTemperatureList.forEach { rec ->
            skinTemperatureListProcessed.add(
                SampleRecord(
                    value = rec.baseline?.inCelsius.toString(), time = rec.startTime.toString()
                )
            )
        }
    }


    // Локальное состояние для конфигурации – после успешного сканирования обновится и инициирует экспорт
    var configState by remember { mutableStateOf<ConfigData?>(null) }
    // Состояние для отслеживания прогресса экспорта для каждого типа
    val exportProgressMap = remember { mutableStateMapOf<DataType, Pair<Int, Int>>() }
    // Состояние для отображения предупреждений
    var warningMessage by remember { mutableStateOf<String?>(null) }

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

    // Состояния для режима сканера
    var isCameraMode by remember { mutableStateOf(false) }

    // Состояние разрешения для камеры
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Кнопки для запроса разрешений и открытия камеры
            if (!permissionsGranted) {
                Button(onClick = { onPermissionsLaunch(permissions) }) {
                    Text(text = stringResource(R.string.permissions_button_label))
                }
            }
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
                }, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Открыть камеру")
            }
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Button(
                    onClick = {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Предоставить разрешение в настройках")
                }
            }


            // Секция для progressbar-ов для каждого типа экспорта
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val exports = listOf(
                    DataType.SLEEP_SESSION_DATA to sleepSessionsListProcessed,
                    DataType.BLOOD_OXYGEN_DATA to bloodOxygenListProcessed,
                    DataType.HEART_RATE_RECORD to heartRateListProcessed,
                    DataType.ACTIVE_CALORIES_BURNED_RECORD to activeCaloriesListProcessed,
                    DataType.BASAL_METABOLIC_RATE_RECORD to basalMetabolicRateListProcessed,
                    DataType.BLOOD_PRESSURE_RECORD to bloodPressureListProcessed,
                    DataType.BODY_FAT_RECORD to bodyFatListProcessed,
                    DataType.BODY_TEMPERATURE_RECORD to bodyTemperatureListProcessed,
                    DataType.BONE_MASS_RECORD to boneMassListProcessed,
                    DataType.DISTANCE_RECORD to distanceListProcessed,
                    DataType.EXERCISE_SESSION_RECORD to exerciseSessionListProcessed,
                    DataType.HYDRATION_RECORD to hydrationListProcessed,
                    DataType.SPEED_RECORD to speedListProcessed,
                    DataType.STEPS_RECORD to stepsListProcessed,
                    DataType.TOTAL_CALORIES_BURNED_RECORD to totalCaloriesBurnedListProcessed,
                    DataType.WEIGHT_RECORD to weightListProcessed,
                    DataType.BASAL_BODY_TEMPERATURE_RECORD to basalBodyTemperatureListProcessed,
                    DataType.FLOORS_CLIMBED_RECORD to floorsClimbedListProcessed,
                    DataType.INTERMENSTRUAL_BLEEDING_RECORD to intermenstrualBleedingListProcessed,
                    DataType.LEAN_BODY_MASS_RECORD to leanBodyMassListProcessed,
                    DataType.MENSTRUATION_FLOW_RECORD to menstruationFlowListProcessed,
                    DataType.NUTRITION_RECORD to nutritionListProcessed,
                    DataType.POWER_RECORD to powerListProcessed,
                    DataType.RESPIRATORY_RATE_RECORD to respiratoryRateListProcessed,
                    DataType.RESTING_HEART_RATE_RECORD to restingHeartRateListProcessed,
                    DataType.SKIN_TEMPERATURE_RECORD to skinTemperatureListProcessed
                )
                configState?.let {
                    exports.forEach { (type, dataList) ->
                        if (dataList.isNotEmpty()) {
                            if (exportProgressMap[type] == null) {
                                exportHealthDataInBackground(dataList, type) { completed, total ->
                                    exportProgressMap[type] = completed to total
                                }
                            }
                            val progress = exportProgressMap[type] ?: (0 to dataList.size)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = type.typeName,
                                    fontSize = 16.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                LinearProgressIndicator(
                                    progress = if (progress.second > 0) progress.first.toFloat() / progress.second else 0f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Выгружено ${progress.first} из ${progress.second}",
                                    fontSize = 16.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Если включён режим сканера, отображаем его поверх основного экрана
        if (isCameraMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraScannerScreen(
                    onQRCodeScanned = { result ->
                        // Сначала сбрасываем предыдущее сообщение об ошибке
                        warningMessage = null
                        // Если результат пустой
                        if (result.isNullOrEmpty()) {
                            warningMessage = "Сканирование не дало результата."
                            isCameraMode = false
                        } else if (!isValidUrl(result)) {
                            // Если строка не является валидным URL
                            warningMessage = "Сканированный QR-код не содержит корректного URL."
                            isCameraMode = false
                        } else {
                            // Отправляем запрос по полученному URL
                            sendRequestToUrl(result) { code, responseBody ->
                                if (code !in 200..299) {
                                    // Если код ответа не 2xx — показываем предупреждение
                                    warningMessage =
                                        "Запрос по сканированному URL вернул ошибку: $code"
                                } else {
                                    // Пытаемся распарсить конфигурацию
                                    val config = parseConfig(responseBody)
                                    if (config == null) {
                                        warningMessage =
                                            "Полученные данные не соответствуют ожидаемому формату."
                                    } else {
                                        GlobalConfig.config = config
                                        configState = config
                                        // Явно сбрасываем сообщение об ошибке, если всё успешно
                                        warningMessage = null
                                    }
                                }
                            }
                            isCameraMode = false
                        }
                    })
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

        // Отображение предупреждения, если оно установлено
        warningMessage?.let { message ->
            WarningDialog(message = message) { warningMessage = null }
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
 * Результат возвращается через onResult, содержащий HTTP-код и тело ответа.
 */
fun sendRequestToUrl(url: String, onResult: (code: Int, body: String) -> Unit) {
    val client = OkHttpClient()
    try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(-1, "Ошибка запроса: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "Пустой ответ"
                onResult(response.code, body)
            }
        })
    } catch (e: Exception) {
        onResult(-1, "Некорректный URL или ошибка: ${e.localizedMessage}")
    }
}

/**
 * Парсит JSON-ответ в объект ConfigData.
 * Ожидается, что JSON имеет формат:
 * {
 *   "post_here": "str",
 *   "access_token": "str",
 *   "refresh_token": "str",
 *   "refresh_token_url": "str",
 *   "token_type": "str"
 * }
 */
fun parseConfig(response: String): ConfigData? {
    return try {
        Gson().fromJson(response, ConfigData::class.java)
    } catch (e: Exception) {
        null
    }
}

/**
 * Обновляет access_token, используя refresh_token.
 * Если обновление успешно, возвращает true и обновляет GlobalConfig.
 */
fun refreshAccessToken(config: ConfigData): Boolean {
    val client = OkHttpClient()
    val gson = Gson()
    val requestBodyJson = gson.toJson(mapOf("refresh_token" to config.refresh_token))
    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    val requestBody = requestBodyJson.toRequestBody(jsonMediaType)

    val request = Request.Builder().url(config.refresh_token_url).post(requestBody)
        .addHeader("accept", "application/json")
        .addHeader("Authorization", "Bearer ${config.access_token}")
        .addHeader("Content-Type", "application/json").build()

    try {
        client.newCall(request).execute().use { response ->
            if (response.code == 200) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val refreshedData = gson.fromJson(responseBody, ConfigData::class.java)
                    if (!refreshedData.access_token.isNullOrEmpty()) {
                        GlobalConfig.config = config.copy(access_token = refreshedData.access_token)
                        return true
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("RefreshToken", "Ошибка при обновлении access_token: ${e.localizedMessage}")
    }
    return false
}

/**
 * Экспортирует данные пачками по 50 элементов с учетом проверки HTTP-кода и обновления токена.
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

    dataList.chunked(50).forEach { batch ->
        val jsonBody = gson.toJson(batch)
        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        fun buildRequest(token: String): Request {
            return Request.Builder().url("${config.post_here}/${dataType.typeName}")
                .post(requestBody).addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json").build()
        }

        var request = buildRequest(config.access_token)
        var responseSuccess = false

        try {
            client.newCall(request).execute().use { response ->
                if (response.code in 200..299) {
                    Log.d("ExportData", "Пачка успешно выгружена: ${response.code}")
                    responseSuccess = true
                } else {
                    Log.e("ExportData", "Ошибка: получен код ${response.code} при экспорте")
                    if (response.code == 403) {
                        if (refreshAccessToken(config)) {
                            request = buildRequest(GlobalConfig.config!!.access_token)
                            client.newCall(request).execute().use { refreshedResponse ->
                                if (refreshedResponse.code in 200..299) {
                                    Log.d(
                                        "ExportData",
                                        "Пачка успешно выгружена после обновления токена: ${refreshedResponse.code}"
                                    )
                                    responseSuccess = true
                                } else {
                                    Log.e(
                                        "ExportData",
                                        "Ошибка после обновления токена: ${refreshedResponse.code}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ExportData", "Ошибка при выгрузке пачки: ${e.localizedMessage}")
        }

        if (!responseSuccess) {
            Log.e("ExportData", "Не удалось экспортировать пачку данных для ${dataType.typeName}")
        }

        completed += batch.size
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
                        result?.let { res: String ->
                            scannedResult = res
                            onQRCodeScanned(res)
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

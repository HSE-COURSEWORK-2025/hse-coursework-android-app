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
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.health.connect.client.records.*
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


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.OffsetDateTime
import java.time.ZoneOffset

import java.time.format.DateTimeFormatter
import kotlin.jvm.java
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent


// Определение enum для типов данных
enum class DataType(val typeName: String) {
    SLEEP_SESSION_STAGES_DATA("SleepSessionStagesData"), SLEEP_SESSION_TIME_DATA("SleepSessionTimeData"), BLOOD_OXYGEN_DATA("BloodOxygenData"), HEART_RATE_RECORD(
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

data class ConfigData(
    val post_here: String,
    val access_token: String,
    val refresh_token: String,
    val refresh_token_url: String,
    val token_type: String,
    val email: String
)

var globalPercent = 0

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
    val value: String, val time: String, var email: String
)

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


@Composable
fun OverlayProgress(
    completedJobs: Int,
    totalJobs: Int,
    isExportInProgress: Boolean
) {
    if (completedJobs < totalJobs || isExportInProgress) {
        Box(
            Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (completedJobs < totalJobs) {
                    val pct = completedJobs * 100f / totalJobs
                    CircularProgressIndicator(progress = pct / 100f)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Считывание данных из Health Connect… $completedJobs из $totalJobs",
                        color = Color.White
                    )

                    Text(
                        text = "Внимание: на прогресс-барах пока могут отображаться не все данные (пока идет считывание из Health Connect).",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
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
    skinTemperatureList: List<SkinTemperatureRecord>,
    completedJobs: Int,
    totalJobs: Int,

    ) {
    val context = LocalContext.current

    // Lists of processed sample records
    val sleepSessionsTimeListProcessed = remember { mutableStateListOf<SampleRecord>() }
    val sleepSessionsStagesListProcessed = remember { mutableStateListOf<SampleRecord>() }
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


    // State for scanned config and export progress
    var configState by remember { mutableStateOf<ConfigData?>(null) }
    val exportProgressMap = remember { mutableStateMapOf<DataType, Pair<Int, Int>>() }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    // Handle UI state changes and errors
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

    var isCameraMode by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)



    Box(modifier = Modifier.fillMaxSize()) {


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
            // Process each record type into SampleRecord lists with initial progress=0

            val gson = Gson()
            sleepSessionsStagesListProcessed.apply {
                clear()
                sleepSessionsList.forEach { rec ->
                    // Сериализуем весь массив stages в JSON
                    val stagesJson: String = gson.toJson(rec.stages)

                    add(
                        SampleRecord(
                            value    = stagesJson,
                            time     = rec.startTime.toString(),
                            
                            email    = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }

            sleepSessionsTimeListProcessed.apply {
                clear()
                sleepSessionsList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.duration.toString(),           // здесь берём длительность сна
                            time = rec.startTime.toString(),            // время начала сессии
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }

            bloodOxygenListProcessed.apply {
                clear()
                bloodOxygenList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.value.toString().replace(Regex("[^\\d.,]"), ""),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            heartRateListProcessed.apply {
                clear()
                heartRateList.forEach { rec ->
                    rec.samples.forEach { sample ->
                        add(
                            SampleRecord(
                                value = sample.beatsPerMinute.toString(),
                                time = sample.time.toString(),
                                
                                email = GlobalConfig.config?.email ?: ""
                            )
                        )
                    }
                }
            }
            activeCaloriesListProcessed.apply {
                clear()
                activeCaloriesList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.energy.inKilocalories.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            basalMetabolicRateListProcessed.apply {
                clear()
                basalMetabolicRateList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.basalMetabolicRate.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            bloodPressureListProcessed.apply {
                clear()
                bloodPressureList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = "${rec.systolic}/${rec.diastolic}",
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            bodyFatListProcessed.apply {
                clear()
                bodyFatList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.percentage.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            bodyTemperatureListProcessed.apply {
                clear()
                bodyTemperatureList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.temperature.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            boneMassListProcessed.apply {
                clear()
                boneMassList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.mass.inKilograms.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            distanceListProcessed.apply {
                clear()
                distanceList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.distance.toString().replace(Regex("[^\\d.,]"), ""),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            exerciseSessionListProcessed.apply {
                clear()
                exerciseSessionList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.title.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            hydrationListProcessed.apply {
                clear()
                hydrationList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.volume.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            speedListProcessed.apply {
                clear()
                speedList.forEach { rec ->
                    rec.samples.forEach { sample ->
                        val rawValue = sample.speed.inKilometersPerHour.toString()
                        // Оставляем только цифры, точки и запятые
                        val cleanedValue = rawValue.replace(Regex("[^\\d.,]"), "")
                        add(
                            SampleRecord(
                                value = cleanedValue,                  // очищенное значение
                                time = sample.time.toString(),         // время конкретного sample
                                
                                email = GlobalConfig.config?.email ?: ""
                            )
                        )
                    }
                }
            }


            stepsListProcessed.apply {
                clear()
                stepsList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.count.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }

            totalCaloriesBurnedListProcessed.apply {
                clear()
                totalCaloriesBurnedList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.energy.inKilocalories.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            weightListProcessed.apply {
                clear()
                weightList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.weight.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            basalBodyTemperatureListProcessed.apply {
                clear()
                basalBodyTemperatureList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.temperature.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            floorsClimbedListProcessed.apply {
                clear()
                floorsClimbedList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.floors.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            intermenstrualBleedingListProcessed.apply {
                clear()
                intermenstrualBleedingList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.zoneOffset.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            leanBodyMassListProcessed.apply {
                clear()
                leanBodyMassList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.mass.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            menstruationFlowListProcessed.apply {
                clear()
                menstruationFlowList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.flow.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            nutritionListProcessed.apply {
                clear()
                nutritionList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.energy.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            powerListProcessed.apply {
                clear()
                powerList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.samples.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            respiratoryRateListProcessed.apply {
                clear()
                respiratoryRateList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.rate.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            restingHeartRateListProcessed.apply {
                clear()
                restingHeartRateList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.beatsPerMinute.toString(),
                            time = rec.time.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
            skinTemperatureListProcessed.apply {
                clear()
                skinTemperatureList.forEach { rec ->
                    add(
                        SampleRecord(
                            value = rec.baseline?.inCelsius.toString(),
                            time = rec.startTime.toString(),
                            
                            email = GlobalConfig.config?.email ?: ""
                        )
                    )
                }
            }
        }




        Spacer(modifier = Modifier.height(8.dp))

        if ((completedJobs < totalJobs) && configState == null) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val percent = completedJobs * 100f / totalJobs
                    CircularProgressIndicator(progress = percent / 100f)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Считывание данных из Health Connect… $completedJobs из $totalJobs",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }




        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permissions and camera buttons
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
                        Text(
                            text = "Предоставить разрешение на камеру в настройках",
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "(для случая, когда не появляется окно выдачи разрешения на камеру)",
                        textAlign = TextAlign.Center
                    )

                }

                // Progress bars for each data type
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val exports = listOf(
                        DataType.SLEEP_SESSION_STAGES_DATA to sleepSessionsStagesListProcessed,
                        DataType.SLEEP_SESSION_TIME_DATA to sleepSessionsTimeListProcessed,
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
                        // Individual progress bars
                        exports.forEach { (type, dataList) ->
                            if (dataList.isNotEmpty()) {
                                val (completed, total) = exportProgressMap[type]
                                    ?: (0 to dataList.size)
                                var totalCompleted = exportProgressMap.values.sumOf { it.first }
                                val totalTotal = exportProgressMap.values.sumOf { it.second }
                                globalPercent =
                                    if (totalTotal > 0) (totalCompleted * 100 / totalTotal) else 0
                                globalPercent =
                                    if (globalPercent < 100) (globalPercent + 1) else globalPercent
                                exports.forEach { (_, dataList) ->
                                    dataList.forEach { rec ->
                                        rec.email = configState?.email.toString()
                                    }
                                }


                                if (exportProgressMap[type] == null) {
                                    exportHealthDataInBackground(
                                        dataList,
                                        type
                                    ) { completed, total ->
                                        exportProgressMap[type] = completed to total
                                    }
                                }

                            
                                sendProgressUpdateAsync(globalPercent, GlobalConfig.config!!.email)



                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = type.typeName,
                                        fontSize = 16.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                    LinearProgressIndicator(
                                        progress = if (total > 0) completed.toFloat() / total else 0f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Выгружено $completed из $total",
                                        fontSize = 16.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Общий прогресс всех типов
                        if (exportProgressMap.isNotEmpty()) {
                            val totalCompleted = exportProgressMap.values.sumOf { it.first }
                            val totalTotal = exportProgressMap.values.sumOf { it.second }


                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Общий прогресс: $globalPercent%",
                                    fontSize = 18.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                LinearProgressIndicator(
//                                progress = if (totalTotal > 0) totalCompleted.toFloat() / totalTotal else 0f,
                                    progress = globalPercent / 100f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Выгружено $totalCompleted из $totalTotal",
                                    fontSize = 18.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (configState != null) {


                        Spacer(modifier = Modifier.height(8.dp))

                        OverlayProgress(
                            completedJobs = completedJobs,
                            totalJobs = totalJobs,
                            isExportInProgress = exportProgressMap.isNotEmpty()
                        )

                        Text(
                            text = "Внимание: для повторной выгрузки нужно перезапустить приложение и обновить QR-код в веб-приложении",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                    }
                }


            }

        }


    }


    // Camera scanner overlay
    if (isCameraMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraScannerScreen(
                onQRCodeScanned = { result ->
                    warningMessage = null
                    if (result.isNullOrEmpty()) {
                        warningMessage = "Сканирование не дало результата."
                        isCameraMode = false
                    } else if (!isValidUrl(result)) {
                        warningMessage = "Сканированный QR-код не содержит корректного URL."
                        isCameraMode = false
                    } else {
                        sendRequestToUrl(result) { code, responseBody ->
                            if (code !in 200..299) {
                                warningMessage = "Запрос по сканированному URL вернул ошибку: $code"
                            } else {
                                val config = parseConfig(responseBody)
                                if (config == null) {
                                    warningMessage =
                                        "Полученные данные не соответствуют ожидаемому формату."
                                } else {
                                    GlobalConfig.config = config
                                    configState = config
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

    // Warning dialog
    warningMessage?.let { msg ->
        WarningDialog(message = msg) { warningMessage = null }
    }
}


fun isValidUrl(url: String): Boolean {
    return Patterns.WEB_URL.matcher(url).matches()
}

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

fun parseConfig(response: String): ConfigData? {
    return try {
        Gson().fromJson(response, ConfigData::class.java)
    } catch (e: Exception) {
        null
    }
}

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
                val refreshedData =
                    response.body?.string()?.let { Gson().fromJson(it, ConfigData::class.java) }
                if (refreshedData != null && refreshedData.access_token.isNotEmpty()) {
                    GlobalConfig.config = config.copy(access_token = refreshedData.access_token)
                    return true
                }
            }
        }
    } catch (e: Exception) {
        Log.e("RefreshToken", "Ошибка при обновлении access_token: ${e.localizedMessage}")
    }
    return false
}

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

    if (dataType.toString() == "SPEED_RECORD"){
        Log.i("lmao", "lmao")
    }

    dataList.chunked(50).forEach { batch ->
        val jsonString = gson.toJson(batch)
        Log.e("jsonString", "${jsonString}")
        val requestBody = gson.toJson(batch).toRequestBody(jsonMediaType)
        val sentAt = OffsetDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val url = "${config.post_here}/${dataType.typeName}".toHttpUrlOrNull()!!
            .newBuilder()
            .build()

        fun buildRequest(token: String) =
            Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

        var request = buildRequest(config.access_token)
        var responseSuccess = false
        try {
            client.newCall(request).execute().use { response ->
                if (response.code in 200..299) {
                    responseSuccess = true
                } else if (response.code == 403 && refreshAccessToken(config)) {
                    client.newCall(buildRequest(GlobalConfig.config!!.access_token)).execute()
                        .use { resp2 ->
                            if (resp2.code in 200..299) {
                                responseSuccess = true
                            }
                        }
                }
            }
        } catch (e: Exception) {
            Log.e("ExportData", "Ошибка при выгрузке пачки: ${e.localizedMessage}")
        }
        completed += batch.size







        withContext(Dispatchers.Main) {
            onProgressUpdate(completed, total)
        }
    }
}

@Composable
fun CameraScannerScreen(onQRCodeScanned: (String?) -> Unit) {
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
        scannedResult?.let {
            Text(
                text = "Найден QR-код: $it", modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

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
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
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

private fun processImageProxy(
    imageProxy: ImageProxy, onBarcodeFound: (String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image).addOnSuccessListener { barcodes ->
            onBarcodeFound(barcodes.firstOrNull()?.rawValue)
        }.addOnFailureListener {
            it.printStackTrace()
            onBarcodeFound(null)
        }.addOnCompleteListener {
            imageProxy.close()
        }
    } else {
        imageProxy.close()
    }
}


fun sendProgressUpdateAsync(progress: Int, email: String) {
    val client = OkHttpClient()
    val gson = Gson()
    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    val payload = mapOf("progress" to progress.toString(), "email" to email)
    val body = gson.toJson(payload).toRequestBody(jsonMediaType)

    val url = "http://hse-coursework-health.ru/data-collection-api/api/v1/post_data/progress"
        .toHttpUrlOrNull()!!

    val req = Request.Builder()
        .url(url)
        .post(body)
        .addHeader("accept", "application/json")
        .addHeader("Content-Type", "application/json")
        .build()

    client.newCall(req).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("ProgressUpdate", "Async failed: ${e.localizedMessage}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                Log.e("ProgressUpdate", "Async failed: ${response.code}")
            }
            response.close()
        }
    })
}

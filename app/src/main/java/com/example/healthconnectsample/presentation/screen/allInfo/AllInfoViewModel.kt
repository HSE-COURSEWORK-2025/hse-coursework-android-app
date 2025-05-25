package com.example.healthconnectsample.presentation.screen.allInfo

import android.os.RemoteException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthconnectsample.data.HealthConnectManager
import com.example.healthconnectsample.data.SleepSessionData
import com.example.healthconnectsample.data.BloodOxygenData
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord

import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.BloodPressureRecord

class SleepSessionViewModel(private val healthConnectManager: HealthConnectManager) : ViewModel() {

    // Здесь перечисляем ТОЛЬКО те Record-типы, которые есть на скриншотах:
    private val changesDataTypes = setOf(
        ExerciseSessionRecord::class,
        StepsRecord::class,
        SpeedRecord::class,
        DistanceRecord::class,
        TotalCaloriesBurnedRecord::class,
        HeartRateRecord::class,
        SleepSessionRecord::class,
        WeightRecord::class,
        BloodPressureRecord::class,
        BodyTemperatureRecord::class,
        HydrationRecord::class,
        BodyFatRecord::class,
        BoneMassRecord::class,
        ActiveCaloriesBurnedRecord::class,
        BasalMetabolicRateRecord::class,
        BasalBodyTemperatureRecord::class,
        IntermenstrualBleedingRecord::class,
        FloorsClimbedRecord::class,
        HeightRecord::class,
        LeanBodyMassRecord::class,
        MenstruationFlowRecord::class,
        NutritionRecord::class,
        OxygenSaturationRecord::class,
        PowerRecord::class,
        RespiratoryRateRecord::class,
        RestingHeartRateRecord::class,
        SkinTemperatureRecord::class
    )

    val permissions = changesDataTypes.map { HealthPermission.getReadPermission(it) }.toSet()

    var permissionsGranted = mutableStateOf(false)
        private set

    // Основные примеры состояний для типов, которые есть на скриншотах
    var sessionsList: MutableState<List<SleepSessionData>> = mutableStateOf(listOf())
        private set

    var heartRateList: MutableState<List<HeartRateRecord>> = mutableStateOf(listOf())
        private set

    var bloodOxygenList: MutableState<List<BloodOxygenData>> = mutableStateOf(listOf())
        private set

    var activeCaloriesList: MutableState<List<ActiveCaloriesBurnedRecord>> =
        mutableStateOf(listOf())
        private set

    var basalMetabolicRateList: MutableState<List<BasalMetabolicRateRecord>> =
        mutableStateOf(listOf())
        private set

    var bloodPressureList: MutableState<List<BloodPressureRecord>> = mutableStateOf(listOf())
        private set

    var bodyFatList: MutableState<List<BodyFatRecord>> = mutableStateOf(listOf())
        private set

    var bodyTemperatureList: MutableState<List<BodyTemperatureRecord>> = mutableStateOf(listOf())
        private set

    var boneMassList: MutableState<List<BoneMassRecord>> = mutableStateOf(listOf())
        private set

    var distanceList: MutableState<List<DistanceRecord>> = mutableStateOf(listOf())
        private set

    var exerciseSessionList: MutableState<List<ExerciseSessionRecord>> = mutableStateOf(listOf())
        private set

    var floorsClimbedList: MutableState<List<FloorsClimbedRecord>> = mutableStateOf(listOf())
        private set

    var hydrationList: MutableState<List<HydrationRecord>> = mutableStateOf(listOf())
        private set

    var speedList: MutableState<List<SpeedRecord>> = mutableStateOf(listOf())
        private set

    var stepsList: MutableState<List<StepsRecord>> = mutableStateOf(listOf())
        private set

    var totalCaloriesBurnedList: MutableState<List<TotalCaloriesBurnedRecord>> =
        mutableStateOf(listOf())
        private set

    var weightList: MutableState<List<WeightRecord>> = mutableStateOf(listOf())
        private set

    var basalBodyTemperatureList: MutableState<List<BasalBodyTemperatureRecord>> =
        mutableStateOf(listOf())
        private set

    var intermenstrualBleedingList: MutableState<List<IntermenstrualBleedingRecord>> =
        mutableStateOf(listOf())
        private set

    var leanBodyMassList: MutableState<List<LeanBodyMassRecord>> = mutableStateOf(listOf())
        private set

    var menstruationFlowList: MutableState<List<MenstruationFlowRecord>> = mutableStateOf(listOf())
        private set

    var nutritionList: MutableState<List<NutritionRecord>> = mutableStateOf(listOf())
        private set

    var oxygenSaturationList: MutableState<List<OxygenSaturationRecord>> = mutableStateOf(listOf())
        private set

    var powerList: MutableState<List<PowerRecord>> = mutableStateOf(listOf())
        private set

    var respiratoryRateList: MutableState<List<RespiratoryRateRecord>> = mutableStateOf(listOf())
        private set

    var restingHeartRateList: MutableState<List<RestingHeartRateRecord>> = mutableStateOf(listOf())
        private set

    var skinTemperatureList: MutableState<List<SkinTemperatureRecord>> = mutableStateOf(listOf())
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

//    val isAllDataTypesExportComplete = healthConnectManager.isAllDataTypesExportComplete()


    fun initialLoad() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                // Чтение только тех типов данных, которые нужны
                sessionsList.value = healthConnectManager.readSleepSessions()


                activeCaloriesList.value = healthConnectManager.readActiveCaloriesBurnedRecords()
                basalMetabolicRateList.value = healthConnectManager.readBasalMetabolicRateRecords()
                bloodPressureList.value = healthConnectManager.readBloodPressureRecords()
                bodyFatList.value = healthConnectManager.readBodyFatRecords()
                boneMassList.value = healthConnectManager.readBoneMassRecords()
                distanceList.value = healthConnectManager.readDistanceRecords()
                exerciseSessionList.value = healthConnectManager.readExerciseSessions()
                floorsClimbedList.value = healthConnectManager.readFloorsClimbedRecords()
                hydrationList.value = healthConnectManager.readHydrationRecords()
                speedList.value = healthConnectManager.readSpeedRecords()
                stepsList.value = healthConnectManager.readStepsRecords()
                totalCaloriesBurnedList.value =
                    healthConnectManager.readTotalCaloriesBurnedRecords()
                weightList.value = healthConnectManager.readWeightInputs()
                basalBodyTemperatureList.value =
                    healthConnectManager.readBasalBodyTemperatureRecords()
                intermenstrualBleedingList.value =
                    healthConnectManager.readIntermenstrualBleedingRecords()
                leanBodyMassList.value = healthConnectManager.readLeanBodyMassRecords()
                menstruationFlowList.value = healthConnectManager.readMenstruationFlowRecords()
                nutritionList.value = healthConnectManager.readNutritionRecords()
                oxygenSaturationList.value = healthConnectManager.readOxygenSaturationRecords()
                powerList.value = healthConnectManager.readPowerRecords()
                respiratoryRateList.value = healthConnectManager.readRespiratoryRateRecords()
                restingHeartRateList.value = healthConnectManager.readRestingHeartRateRecords()
                skinTemperatureList.value = healthConnectManager.readSkinTemperatureRecords()

                heartRateList.value = healthConnectManager.readHeartRateRecords()
                bloodOxygenList.value = healthConnectManager.readBloodOxygen()

                permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)

            }
        }
    }

    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
        uiState = try {
            if (permissionsGranted.value) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    sealed class UiState {
        object Uninitialized : UiState()
        object Done : UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}

class HealthConnectDataViewModelFactory(
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SleepSessionViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

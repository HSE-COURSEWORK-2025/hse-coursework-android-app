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
package com.example.healthconnectsample.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources.NotFoundException
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord





import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WheelchairPushesRecord




import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest



import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.example.healthconnectsample.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.io.InvalidObjectException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.reflect.KClass
import java.util.UUID

import java.time.LocalDate

import kotlinx.coroutines.delay



// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

/** Demonstrates reading and writing from Health Connect. */
class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }


    private val dataTypesToTrack: Set<KClass<out Record>> = setOf(
        HeartRateRecord::class,
        OxygenSaturationRecord::class,
        SleepSessionRecord::class,
        DistanceRecord::class,
        StepsRecord::class,
        TotalCaloriesBurnedRecord::class,
        WeightRecord::class,
        ActiveCaloriesBurnedRecord::class,
        BasalMetabolicRateRecord::class,
        BloodPressureRecord::class,
        BodyFatRecord::class,
        BoneMassRecord::class,
        HydrationRecord::class,
        SpeedRecord::class,
        ExerciseSessionRecord::class,
        BloodGlucoseRecord::class,
        BasalBodyTemperatureRecord::class,
        FloorsClimbedRecord::class,
        CervicalMucusRecord::class,
        LeanBodyMassRecord::class,
        MenstruationFlowRecord::class,
        NutritionRecord::class,
        PlannedExerciseSessionRecord::class,
        PowerRecord::class,
        RespiratoryRateRecord::class,
        RestingHeartRateRecord::class,
        SkinTemperatureRecord::class,
        Vo2MaxRecord::class,
        WheelchairPushesRecord::class,
        IntermenstrualBleedingRecord::class,
        HeightRecord::class,
        BodyWaterMassRecord::class,
        HeartRateVariabilityRmssdRecord::class,
        MenstruationPeriodRecord::class,
        OvulationTestRecord::class
    )



    val healthConnectCompatibleApps by lazy {
        val intent = Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE")

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )
        }

        packages.associate {
            val icon = try {
                context.packageManager.getApplicationIcon(it.activityInfo.packageName)
            } catch (e: NotFoundException) {
                null
            }
            val label = context.packageManager.getApplicationLabel(it.activityInfo.applicationInfo)
                .toString()
            it.activityInfo.packageName to
                    HealthConnectAppInfo(
                        packageName = it.activityInfo.packageName,
                        icon = icon,
                        appLabel = label
                    )
        }
    }

    var availability = mutableStateOf(SDK_UNAVAILABLE)
        private set

    fun checkAvailability() {
        availability.value = HealthConnectClient.getSdkStatus(context)
    }

    init {
        checkAvailability()
    }


    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }



    suspend fun readExerciseSessions(): List<ExerciseSessionRecord> {
        val lastDay = ZonedDateTime.now()
            .minusDays(1)
            
        val firstDay = lastDay
            .minusDays(7)

        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant())
        )
        val response = healthConnectClient.readRecords(request)

        return response.records
    }


    suspend fun readSleepSessions(): List<SleepSessionData> {
        val lastDay = ZonedDateTime.now()
            .minusDays(1)
            
        val firstDay = lastDay
            .minusDays(7)

        val sessions = mutableListOf<SleepSessionData>()
        val sleepSessionRequest = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )
        val sleepSessions = healthConnectClient.readRecords(sleepSessionRequest)
        sleepSessions.records.forEach { session ->
            val sessionTimeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            val durationAggregateRequest = AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = sessionTimeFilter
            )
            val aggregateResponse = healthConnectClient.aggregate(durationAggregateRequest)
            sessions.add(
                SleepSessionData(
                    uid = session.metadata.id,
                    title = session.title,
                    notes = session.notes,
                    startTime = session.startTime,
                    startZoneOffset = session.startZoneOffset,
                    endTime = session.endTime,
                    endZoneOffset = session.endZoneOffset,
                    duration = aggregateResponse[SleepSessionRecord.SLEEP_DURATION_TOTAL],
                    stages = session.stages
                )
            )
        }

        return sessions
    }



    suspend fun readBloodOxygen(): List<BloodOxygenData> {
        val lastDay = ZonedDateTime.now()
            
        val firstDay = lastDay
            .minusDays(30)

        val sessions = mutableListOf<BloodOxygenData>()
        val bloodOxygenRequest = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )
        val bloodOxygenRecords = healthConnectClient.readRecords(bloodOxygenRequest)
        bloodOxygenRecords.records.forEach { session ->

            sessions.add(
                BloodOxygenData(
                    uid = session.metadata.id,
                    startTime = session.time,
                    value = session.percentage.toString()
                )
            )
        }

        return sessions
    }






    suspend fun readDistanceRecords(): List<DistanceRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )


        return healthConnectClient.readRecords(request).records
    }

    /**
     * Возвращает записи типа HeartRateRecord.
     */
    suspend fun readHeartRateRecords(): List<HeartRateRecord> {
        val zone = ZonedDateTime.now().zone
        var currentDate = LocalDate.of(2025, 1, 1)
        val today = LocalDate.now(zone)
        val allRecords = mutableListOf<HeartRateRecord>()

        while (!currentDate.isAfter(today)) {
            val startInstant = currentDate.atStartOfDay(zone).toInstant()
            val endInstant = currentDate.plusDays(1).atStartOfDay(zone).toInstant()

            val request = ReadRecordsRequest(
                recordType      = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                ascendingOrder  = false
            )

            var attempt = 0
            var backoffMs = 1L

            // Пытаемся выполнить запрос, при любой ошибке — retry с backoff
            while (true) {
                try {
                    val response = healthConnectClient.readRecords(request)
                    allRecords += response.records
                    break
                } catch (e: Exception) {
                    if (true) {
//                        delay(backoffMs)
                        attempt++
                    }
                }
            }

            // небольшой throttle между днями
//            delay(1L)
            currentDate = currentDate.plusDays(1)
        }


        return allRecords
    }

    /**
     * Возвращает записи типа OxygenSaturationRecord.
     */
    suspend fun readOxygenSaturationRecords(): List<OxygenSaturationRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    /**
     * Возвращает записи типа SleepSessionRecord.
     */
    suspend fun readSleepSessionRecords(): List<SleepSessionRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    /**
     * Возвращает записи типа SpeedRecord.
     */
    suspend fun readSpeedRecords(): List<SpeedRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = SpeedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    /**
     * Возвращает записи типа StepsRecord.
     */
    suspend fun readStepsRecords(): List<StepsRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    /**
     * Возвращает записи типа TotalCaloriesBurnedRecord.
     */
    suspend fun readTotalCaloriesBurnedRecords(): List<TotalCaloriesBurnedRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    /**
     * Возвращает записи типа WeightRecord.
     */
    suspend fun readWeightRecords(): List<WeightRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    // Функции для остальных Record-подобных классов

    suspend fun readActiveCaloriesBurnedRecords(): List<ActiveCaloriesBurnedRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readBasalBodyTemperatureRecords(): List<BasalBodyTemperatureRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = BasalBodyTemperatureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readBasalMetabolicRateRecords(): List<BasalMetabolicRateRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = BasalMetabolicRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readBloodGlucoseRecords(): List<BloodGlucoseRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = BloodGlucoseRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readBloodPressureRecords(): List<BloodPressureRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = BloodPressureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readBodyFatRecords(): List<BodyFatRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = BodyFatRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readBodyWaterMassRecords(): List<BodyWaterMassRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = BodyWaterMassRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readBoneMassRecords(): List<BoneMassRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = BoneMassRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readCervicalMucusRecords(): List<CervicalMucusRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = CervicalMucusRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readCyclingPedalingCadenceRecords(): List<CyclingPedalingCadenceRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = CyclingPedalingCadenceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readElevationGainedRecords(): List<ElevationGainedRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = ElevationGainedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readFloorsClimbedRecords(): List<FloorsClimbedRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = FloorsClimbedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readHeartRateVariabilityRmssdRecords(): List<HeartRateVariabilityRmssdRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = HeartRateVariabilityRmssdRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readHeightRecords(): List<HeightRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = HeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readHydrationRecords(): List<HydrationRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = HydrationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readIntermenstrualBleedingRecords(): List<IntermenstrualBleedingRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = IntermenstrualBleedingRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readLeanBodyMassRecords(): List<LeanBodyMassRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = LeanBodyMassRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readMenstruationFlowRecords(): List<MenstruationFlowRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = MenstruationFlowRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readMenstruationPeriodRecords(): List<MenstruationPeriodRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = MenstruationPeriodRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readNutritionRecords(): List<NutritionRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readOvulationTestRecords(): List<OvulationTestRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = OvulationTestRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readPlannedExerciseSessionRecords(): List<PlannedExerciseSessionRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = PlannedExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readPowerRecords(): List<PowerRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = PowerRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readRespiratoryRateRecords(): List<RespiratoryRateRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = RespiratoryRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readRestingHeartRateRecords(): List<RestingHeartRateRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = RestingHeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readSexualActivityRecords(): List<SexualActivityRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = SexualActivityRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readSkinTemperatureRecords(): List<SkinTemperatureRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = SkinTemperatureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readVo2MaxRecords(): List<Vo2MaxRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = Vo2MaxRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }

    suspend fun readWheelchairPushesRecords(): List<WheelchairPushesRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = WheelchairPushesRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )

        return healthConnectClient.readRecords(request).records
    }


//
//    fun isAllDataTypesExportComplete(): Boolean {
//        return exportCompletionFlags.values.all { it }
//    }


    suspend fun readWeightInputs(): List<WeightRecord> {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant())
        )
        val response = healthConnectClient.readRecords(request)

        return response.records
    }

    suspend fun computeWeeklyAverage(): Mass? {
        val lastDay = ZonedDateTime.now()
        val firstDay = lastDay.minusDays(30)
        val request = AggregateRequest(
            metrics = setOf(WeightRecord.WEIGHT_AVG),
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant())
        )
        val response = healthConnectClient.aggregate(request)
        return response[WeightRecord.WEIGHT_AVG]
    }

    suspend fun getChangesToken(dataTypes: Set<KClass<out Record>>): String {
        val request = ChangesTokenRequest(dataTypes)
        return healthConnectClient.getChangesToken(request)
    }

    /**
     * Creates a [Flow] of change messages, using a changes token as a start point. The flow will
     * terminate when no more changes are available, and the final message will contain the next
     * changes token to use.
     */
    suspend fun getChanges(token: String): Flow<ChangesMessage> = flow {
        var nextChangesToken = token
        do {
            val response = healthConnectClient.getChanges(nextChangesToken)
            if (response.changesTokenExpired) {
                // As described here: https://developer.android.com/guide/health-and-fitness/health-connect/data-and-data-types/differential-changes-api
                // tokens are only valid for 30 days. It is important to check whether the token has
                // expired. As well as ensuring there is a fallback to using the token (for example
                // importing data since a certain date), more importantly, the app should ensure
                // that the changes API is used sufficiently regularly that tokens do not expire.
                throw IOException("Changes token has expired")
            }
            emit(ChangesMessage.ChangeList(response.changes))
            nextChangesToken = response.nextChangesToken
        } while (response.hasMore)
        emit(ChangesMessage.NoMoreChanges(nextChangesToken))
    }











    // Represents the two types of messages that can be sent in a Changes flow.
    sealed class ChangesMessage {
        data class NoMoreChanges(val nextChangesToken: String) : ChangesMessage()

        data class ChangeList(val changes: List<Change>) : ChangesMessage()
    }
}

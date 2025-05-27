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


    private suspend fun <T : Record> readRecordsByWeek(
        recordType: KClass<T>,
        endDate: LocalDate = LocalDate.now(ZonedDateTime.now().zone)
    ): List<T> {
        val zone = ZonedDateTime.now().zone
        val startDate = LocalDate.of(endDate.year, 1, 1)
        val weeks = ChronoUnit.WEEKS.between(startDate, endDate)
        val result = mutableListOf<T>()
        var currentStart = startDate

        repeat((weeks + 1).toInt()) {
            val currentEnd = currentStart.plusWeeks(1)
                .let { if (it.isAfter(endDate)) endDate.plusDays(1) else it }

            while (true) {
                try {
                    // Инлайн-запрос без лишних переменных
                    result += healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            recordType      = recordType,
                            timeRangeFilter = TimeRangeFilter.between(
                                currentStart.atStartOfDay(zone).toInstant(),
                                currentEnd.atStartOfDay(zone).toInstant()
                            ),
                            ascendingOrder  = false
                        )
                    ).records
                    break
                } catch (_: Exception) {
                    // Можно добавить логирование или delay здесь при необходимости
                }
            }

            currentStart = currentStart.plusWeeks(1)
        }
        return result
    }





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





    suspend fun readActiveCaloriesBurnedRecords(): List<ActiveCaloriesBurnedRecord> =
        readRecordsByWeek(ActiveCaloriesBurnedRecord::class)

    suspend fun readBasalBodyTemperatureRecords(): List<BasalBodyTemperatureRecord> =
        readRecordsByWeek(BasalBodyTemperatureRecord::class)

    suspend fun readBasalMetabolicRateRecords(): List<BasalMetabolicRateRecord> =
        readRecordsByWeek(BasalMetabolicRateRecord::class)

    suspend fun readBloodGlucoseRecords(): List<BloodGlucoseRecord> =
        readRecordsByWeek(BloodGlucoseRecord::class)

    suspend fun readBloodPressureRecords(): List<BloodPressureRecord> =
        readRecordsByWeek(BloodPressureRecord::class)

    suspend fun readBodyFatRecords(): List<BodyFatRecord> =
        readRecordsByWeek(BodyFatRecord::class)

    suspend fun readBodyWaterMassRecords(): List<BodyWaterMassRecord> =
        readRecordsByWeek(BodyWaterMassRecord::class)

    suspend fun readBoneMassRecords(): List<BoneMassRecord> =
        readRecordsByWeek(BoneMassRecord::class)

    suspend fun readCervicalMucusRecords(): List<CervicalMucusRecord> =
        readRecordsByWeek(CervicalMucusRecord::class)

    suspend fun readCyclingPedalingCadenceRecords(): List<CyclingPedalingCadenceRecord> =
        readRecordsByWeek(CyclingPedalingCadenceRecord::class)

    suspend fun readDistanceRecords(): List<DistanceRecord> =
        readRecordsByWeek(DistanceRecord::class)

    suspend fun readElevationGainedRecords(): List<ElevationGainedRecord> =
        readRecordsByWeek(ElevationGainedRecord::class)

    suspend fun readExerciseSessionRecords(): List<ExerciseSessionRecord> =
        readRecordsByWeek(ExerciseSessionRecord::class)

    suspend fun readFloorsClimbedRecords(): List<FloorsClimbedRecord> =
        readRecordsByWeek(FloorsClimbedRecord::class)

    suspend fun readHeartRateRecords(): List<HeartRateRecord> =
        readRecordsByWeek(HeartRateRecord::class)

    suspend fun readHeartRateVariabilityRmssdRecords(): List<HeartRateVariabilityRmssdRecord> =
        readRecordsByWeek(HeartRateVariabilityRmssdRecord::class)

    suspend fun readHeightRecords(): List<HeightRecord> =
        readRecordsByWeek(HeightRecord::class)

    suspend fun readHydrationRecords(): List<HydrationRecord> =
        readRecordsByWeek(HydrationRecord::class)

    suspend fun readNutritionRecords(): List<NutritionRecord> =
        readRecordsByWeek(NutritionRecord::class)

    suspend fun readOxygenSaturationRecords(): List<OxygenSaturationRecord> =
        readRecordsByWeek(OxygenSaturationRecord::class)

    suspend fun readOvulationTestRecords(): List<OvulationTestRecord> =
        readRecordsByWeek(OvulationTestRecord::class)

    suspend fun readPlannedExerciseSessionRecords(): List<PlannedExerciseSessionRecord> =
        readRecordsByWeek(PlannedExerciseSessionRecord::class)

    suspend fun readPowerRecords(): List<PowerRecord> =
        readRecordsByWeek(PowerRecord::class)

    suspend fun readRespiratoryRateRecords(): List<RespiratoryRateRecord> =
        readRecordsByWeek(RespiratoryRateRecord::class)

    suspend fun readRestingHeartRateRecords(): List<RestingHeartRateRecord> =
        readRecordsByWeek(RestingHeartRateRecord::class)

    suspend fun readSexualActivityRecords(): List<SexualActivityRecord> =
        readRecordsByWeek(SexualActivityRecord::class)

    suspend fun readSkinTemperatureRecords(): List<SkinTemperatureRecord> =
        readRecordsByWeek(SkinTemperatureRecord::class)

    suspend fun readSleepSessionRecords(): List<SleepSessionRecord> =
        readRecordsByWeek(SleepSessionRecord::class)

    suspend fun readSpeedRecords(): List<SpeedRecord> =
        readRecordsByWeek(SpeedRecord::class)

    suspend fun readStepsRecords(): List<StepsRecord> =
        readRecordsByWeek(StepsRecord::class)

    suspend fun readTotalCaloriesBurnedRecords(): List<TotalCaloriesBurnedRecord> =
        readRecordsByWeek(TotalCaloriesBurnedRecord::class)




    suspend fun readWeightInputs(): List<WeightRecord> =
        readRecordsByWeek(WeightRecord::class)


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
        // 1) Собираем все записи SleepSessionRecord с начала года до текущего дня
        val sleepRecords: List<SleepSessionRecord> =
            readRecordsByWeek(SleepSessionRecord::class)

        // 2) Маппим их в DTO, делая для каждой сессии агрегатный запрос по длительности
        return sleepRecords.map { session ->
            // фильтр по времени именно этой записи
            val sessionTimeFilter = TimeRangeFilter.between(session.startTime, session.endTime)

            // агрегируем общую длительность сна в этой сессии
            val aggregateResponse = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = sessionTimeFilter
                )
            )
            // строим DTO
            SleepSessionData(
                uid             = session.metadata.id,
                title           = session.title,
                notes           = session.notes,
                startTime       = session.startTime,
                startZoneOffset = session.startZoneOffset,
                endTime         = session.endTime,
                endZoneOffset   = session.endZoneOffset,
                duration        = aggregateResponse[SleepSessionRecord.SLEEP_DURATION_TOTAL],
                stages          = session.stages
            )
        }
    }


    suspend fun readBloodOxygen(): List<BloodOxygenData> {
        val records = readRecordsByWeek(OxygenSaturationRecord::class)
        return records.map {
            BloodOxygenData(
                uid = it.metadata.id,
                startTime = it.time,
                value = it.percentage.toString()
            )
        }
    }



    suspend fun readIntermenstrualBleedingRecords(): List<IntermenstrualBleedingRecord> =
        readRecordsByWeek(IntermenstrualBleedingRecord::class)

    suspend fun readLeanBodyMassRecords(): List<LeanBodyMassRecord> =
        readRecordsByWeek(LeanBodyMassRecord::class)

    suspend fun readMenstruationFlowRecords(): List<MenstruationFlowRecord> =
        readRecordsByWeek(MenstruationFlowRecord::class)






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

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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.healthconnectsample.R
import com.example.healthconnectsample.data.SleepSessionData
import com.example.healthconnectsample.presentation.theme.HealthConnectTheme
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import com.example.healthconnectsample.data.HeartRateData

/**
 * Shows a week's worth of sleep data.
 */
@Composable
fun AllInfoSessionScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    sleepSessionsList: List<SleepSessionData>,
    uiState: SleepSessionViewModel.UiState,
    onInsertClick: () -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    HeartRateList: List<HeartRateData>,

    ) {

    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        // If the initial data load has not taken place, attempt to load the data.
        if (uiState is SleepSessionViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        // The [SleepSessionViewModel.UiState] provides details of whether the last action was a
        // success or resulted in an error. Where an error occurred, for example in reading and
        // writing to Health Connect, the user is notified, and where the error is one that can be
        // recovered from, an attempt to do so is made.
        if (uiState is SleepSessionViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    val changesDataTypes = setOf(
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
        BasalMetabolicRateRecord::class
    )
    val permissions2 = changesDataTypes.map { HealthPermission.getReadPermission(it) }.toSet()

    if (uiState != SleepSessionViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionsGranted) {
                item {
                    Button(
                        onClick = { onPermissionsLaunch(permissions2) }
                    ) {
                        Text(text = stringResource(R.string.permissions_button_label))
                    }
                }
            } else {
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "test"
                    )
                }


                val lastDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
                    .minusYears(1)
                    .withHour(12)
                val firstDay = lastDay
                    .minusYears(1).minusDays(30)

                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "lastDay: $lastDay"
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "firstDay: $firstDay"
                    )
                }


                items(sleepSessionsList) { session ->
//                    Text(
//                        modifier = Modifier.fillMaxWidth(),
//                        textAlign = TextAlign.Center,
//                        text = session.uid)


                }

                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "heart rate"
                    )

                }

                items(HeartRateList) { session ->
                    val startTime = session.startTime.toString()
                    val data = session.value
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "$startTime $data"
                    )
                }

            }
        }
    }
}

@Preview
@Composable
fun SleepSessionScreenPreview() {
    HealthConnectTheme {
        val end2 = ZonedDateTime.now()
        val start2 = end2.minusHours(5)
        val end1 = end2.minusDays(1)
        val start1 = end1.minusHours(5)
        AllInfoSessionScreen(
            permissions = setOf(),
            permissionsGranted = true,
            sleepSessionsList = listOf(
                SleepSessionData(
                    uid = "123",
                    title = "My sleep",
                    notes = "Slept well",
                    startTime = start1.toInstant(),
                    startZoneOffset = start1.offset,
                    endTime = end1.toInstant(),
                    endZoneOffset = end1.offset,
                    duration = Duration.between(start1, end1),
                    stages = listOf(
                        SleepSessionRecord.Stage(
                            stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                            startTime = start1.toInstant(),
                            endTime = end1.toInstant()
                        )
                    )
                ),
                SleepSessionData(
                    uid = "123",
                    title = "My sleep",
                    notes = "Slept well",
                    startTime = start2.toInstant(),
                    startZoneOffset = start2.offset,
                    endTime = end2.toInstant(),
                    endZoneOffset = end2.offset,
                    duration = Duration.between(start2, end2),
                    stages = listOf(
                        SleepSessionRecord.Stage(
                            stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                            startTime = start2.toInstant(),
                            endTime = end2.toInstant()
                        )
                    )
                )
            ),
            uiState = SleepSessionViewModel.UiState.Done,
            HeartRateList = listOf(
                HeartRateData(
                    uid = "123",
                    startTime = start2.toInstant(),
                    value = ""
                )
            )
        )

    }
}

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
package com.example.healthconnectsample.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.healthconnectsample.data.HealthConnectManager
import com.example.healthconnectsample.presentation.screen.SettingsScreen
import com.example.healthconnectsample.presentation.screen.changes.DifferentialChangesScreen
import com.example.healthconnectsample.presentation.screen.changes.DifferentialChangesViewModel
import com.example.healthconnectsample.presentation.screen.changes.DifferentialChangesViewModelFactory
import com.example.healthconnectsample.presentation.screen.allInfo.AllInfoScreen
import com.example.healthconnectsample.presentation.screen.allInfo.SleepSessionViewModel
import com.example.healthconnectsample.presentation.screen.allInfo.HealthConnectDataViewModelFactory
import com.example.healthconnectsample.showExceptionSnackbar

/**
 * Provides the navigation in the app.
 */
@Composable
fun HealthConnectNavigation(
    navController: NavHostController,
    healthConnectManager: HealthConnectManager,
    scaffoldState: ScaffoldState
) {
    val scope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = Screen.WelcomeScreen.route) {

        composable(Screen.SettingsScreen.route) {
            SettingsScreen()
        }

        composable(Screen.WelcomeScreen.route) {
            val viewModel: SleepSessionViewModel = viewModel(
                factory = HealthConnectDataViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionsList by viewModel.sessionsList
            val heartRateList by viewModel.heartRateList
            val bloodOxygenList by viewModel.bloodOxygenList
            val permissions = viewModel.permissions

            val activeCaloriesList by viewModel.activeCaloriesList
            val basalMetabolicRateList by viewModel.basalMetabolicRateList
            val bloodPressureList by viewModel.bloodPressureList
            val bodyFatList by viewModel.bodyFatList
            val bodyTemperatureList by viewModel.bodyTemperatureList
            val boneMassList by viewModel.boneMassList
            val distanceList by viewModel.distanceList
            val exerciseSessionList by viewModel.exerciseSessionList
            val hydrationList by viewModel.hydrationList
            val speedList by viewModel.speedList
            val stepsList by viewModel.stepsList
            val totalCaloriesBurnedList by viewModel.totalCaloriesBurnedList
            val weightList by viewModel.weightList
            val basalBodyTemperatureList by viewModel.basalBodyTemperatureList
            val floorsClimbedList by viewModel.floorsClimbedList
            val intermenstrualBleedingList by viewModel.intermenstrualBleedingList
            val leanBodyMassList by viewModel.leanBodyMassList
            val menstruationFlowList by viewModel.menstruationFlowList
            val nutritionList by viewModel.nutritionList
            val powerList by viewModel.powerList
            val respiratoryRateList by viewModel.respiratoryRateList
            val restingHeartRateList by viewModel.restingHeartRateList
            val skinTemperatureList by viewModel.skinTemperatureList


            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }
            AllInfoScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                sleepSessionsList = sessionsList,
                heartRateList = heartRateList,
                bloodOxygenList = bloodOxygenList,
                activeCaloriesList = activeCaloriesList,
                basalMetabolicRateList = basalMetabolicRateList,
                bloodPressureList = bloodPressureList,
                bodyFatList = bodyFatList,
                bodyTemperatureList = bodyTemperatureList,
                boneMassList = boneMassList,
                distanceList = distanceList,
                exerciseSessionList = exerciseSessionList,
                hydrationList = hydrationList,
                speedList = speedList,
                stepsList = stepsList,
                totalCaloriesBurnedList = totalCaloriesBurnedList,
                weightList = weightList,
                basalBodyTemperatureList = basalBodyTemperatureList,
                floorsClimbedList = floorsClimbedList,
                intermenstrualBleedingList = intermenstrualBleedingList,
                leanBodyMassList = leanBodyMassList,
                menstruationFlowList = menstruationFlowList,
                nutritionList = nutritionList,
                powerList = powerList,
                respiratoryRateList = respiratoryRateList,
                restingHeartRateList = restingHeartRateList,
                skinTemperatureList = skinTemperatureList,

                uiState = viewModel.uiState,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)
                },

                completedJobs = viewModel.completedJobs,
                totalJobs     = viewModel.getTotalJobs(),


                )

        }

        composable(Screen.DifferentialChanges.route) {
            val viewModel: DifferentialChangesViewModel = viewModel(
                factory = DifferentialChangesViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val changesToken by viewModel.changesToken
            val permissionsGranted by viewModel.permissionsGranted
            val permissions = viewModel.permissions
            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }
            DifferentialChangesScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                changesEnabled = changesToken != null,
                onChangesEnable = { enabled ->
                    viewModel.enableOrDisableChanges(enabled)
                },
                changes = viewModel.changes,
                changesToken = changesToken,
                onGetChanges = {
                    viewModel.getChanges()
                },
                uiState = viewModel.uiState,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                }
            ) { values ->
                permissionsLauncher.launch(values)
            }
        }
    }
}

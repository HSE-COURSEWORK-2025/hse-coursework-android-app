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
import com.example.healthconnectsample.presentation.screen.allInfo.AllInfoSessionScreen
import com.example.healthconnectsample.presentation.screen.allInfo.SleepSessionViewModel
import com.example.healthconnectsample.presentation.screen.allInfo.SleepSessionViewModelFactory
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

        composable(Screen.SettingsScreen.route){
            SettingsScreen()
        }

        composable(Screen.WelcomeScreen.route) {
            val viewModel: SleepSessionViewModel = viewModel(
                factory = SleepSessionViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionsList by viewModel.sessionsList
            val heartRateList by viewModel.heartRateList
            val permissions = viewModel.permissions
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                onPermissionsResult()}
            AllInfoSessionScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                sleepSessionsList = sessionsList,
                HeartRateList = heartRateList,
                uiState = viewModel.uiState,
                onInsertClick = {
                    viewModel.generateSleepData()
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
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
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()}
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

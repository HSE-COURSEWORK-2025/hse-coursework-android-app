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
package com.example.healthconnectsample.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.healthconnectsample.R
import com.example.healthconnectsample.presentation.component.InstalledMessage
import com.example.healthconnectsample.presentation.component.NotInstalledMessage
import com.example.healthconnectsample.presentation.component.NotSupportedMessage
import com.example.healthconnectsample.presentation.theme.HealthConnectTheme
import androidx.compose.material.Button
import androidx.compose.material.Text

/**
 * Welcome screen shown when the app is first launched.
 */


@Composable
fun WelcomeScreen(
    healthConnectAvailability: Int,
    onResumeAvailabilityCheck: () -> Unit,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val currentOnAvailabilityCheck by rememberUpdatedState(onResumeAvailabilityCheck)

    // Добавляем наблюдение за жизненным циклом, чтобы выполнить проверку при возобновлении экрана
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnAvailabilityCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(0.5f),
            painter = painterResource(id = R.drawable.ic_health_connect_logo),
            contentDescription = stringResource(id = R.string.health_connect_logo)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.welcome_message),
            color = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (healthConnectAvailability) {
            SDK_AVAILABLE -> InstalledMessage()
            SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> NotInstalledMessage()
            SDK_UNAVAILABLE -> NotSupportedMessage()
        }

        // Если разрешения еще не получены, отображаем кнопку для их запроса
        if (!permissionsGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onRequestPermissions() }
            ) {
                Text(text = stringResource(R.string.permissions_button_label))
            }
        }
    }
}

@Preview
@Composable
fun InstalledMessagePreview() {
    HealthConnectTheme {
        WelcomeScreen(
            healthConnectAvailability = SDK_AVAILABLE,
            onResumeAvailabilityCheck = {},
            permissionsGranted = true,
            onRequestPermissions = {}
        )
    }
}

@Preview
@Composable
fun NotInstalledMessagePreview() {
    HealthConnectTheme {
        WelcomeScreen(
            healthConnectAvailability = SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
            onResumeAvailabilityCheck = {},
            permissionsGranted = false,
            onRequestPermissions = {}
        )
    }
}

@Preview
@Composable
fun NotSupportedMessagePreview() {
    HealthConnectTheme {
        WelcomeScreen(
            healthConnectAvailability = SDK_UNAVAILABLE,
            onResumeAvailabilityCheck = {},
            permissionsGranted = false,
            onRequestPermissions = {}
        )
    }
}
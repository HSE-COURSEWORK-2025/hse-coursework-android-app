package com.example.healthconnectsample.presentation.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Экран настроек, где можно либо выбрать изображение из галереи для сканирования QR-кода,
 * либо запустить камеру для сканирования QR-кода в реальном времени.
 *
 * Добавлена проверка разрешения на использование камеры с помощью Accompanist Permissions.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var qrCodeText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Состояние ответа HTTP-запроса
    var httpResponse by remember { mutableStateOf<String?>(null) }
    // Флаг, указывающий, что включён режим камеры.
    var isCameraMode by remember { mutableStateOf(false) }

    // Состояние разрешения на использование камеры
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    // Если разрешение получено, автоматически включаем режим камеры
    LaunchedEffect(cameraPermissionState.status) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isCameraMode = true
        }
    }

    // Лаунчер для выбора изображения из галереи.
    val galleryLauncher = rememberLauncherForActivityResult(contract = GetContent()) { uri ->
        uri?.let {
            // Получаем Bitmap в зависимости от версии API
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            // Запускаем декодирование QR-кода через ML Kit
            decodeQRCodeMLKit(
                bitmap = bitmap,
                onSuccess = { result ->
                    qrCodeText = result ?: "QR-код не найден"
                    errorMessage = null
                    result?.let { url ->
                        // Выполняем запрос по считанному URL
                        sendRequestToUrl(url) { response ->
                            httpResponse = response
                        }
                    }
                },
                onError = { exception ->
                    errorMessage = exception.localizedMessage ?: "Ошибка декодирования"
                    qrCodeText = null
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Кнопка для выбора изображения из галереи.
        Button(onClick = {
            isCameraMode = false
            galleryLauncher.launch("image/*")
        }) {
            Text(text = "Выбрать изображение с QR-кодом")
        }
        // Кнопка для запуска камеры
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    isCameraMode = true
                } else {
                    // Если разрешение не выдано, запрашиваем разрешение.
                    cameraPermissionState.launchPermissionRequest()
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Сканировать QR-код камерой")
        }

        // Если разрешение запрещено (нет и возможности показать диалог rationale),
        // предлагаем перейти в настройки приложения.
        if (!(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            Button(
                onClick = {
                    // Переход в настройки приложения, где пользователь может включить разрешение.
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

        // Если выбран режим камеры, отображаем превью с камеры
        if (isCameraMode) {
            CameraScannerScreen(
                onQRCodeScanned = { result ->
                    isCameraMode = false
                    qrCodeText = result ?: "QR-код не найден"
                    result?.let { url ->
                        // Выполняем запрос по считанному URL
                        sendRequestToUrl(url) { response ->
                            httpResponse = response
                        }
                    }
                }
            )
        }

        // Отображение результата сканирования
        qrCodeText?.let {
            Text(
                text = "Информация с QR-кода: $it",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        errorMessage?.let {
            Text(
                text = "Ошибка: $it",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        httpResponse?.let {
            Text(
                text = "Ответ с сервера: $it",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * Отправляет HTTP GET-запрос по указанному URL.
 *
 * @param url Строка URL, полученная из QR-кода.
 * @param onResult Функция, вызываемая с ответом сервера или сообщением об ошибке.
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
                // Читаем ответ, ограничиваем его размер или форматируем по необходимости.
                val body = response.body?.string() ?: "Пустой ответ"
                onResult(body)
            }
        })
    } catch (e: Exception) {
        onResult("Некорректный URL или ошибка: ${e.localizedMessage}")
    }
}

/**
 * Декодирование QR-кода из Bitmap с использованием Google ML Kit.
 *
 * @param bitmap Изображение с QR-кодом.
 * @param onSuccess Колбэк при успешном извлечении данных (null, если QR-код не найден).
 * @param onError Колбэк при возникновении ошибки.
 */
fun decodeQRCodeMLKit(bitmap: Bitmap, onSuccess: (String?) -> Unit, onError: (Exception) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val scanner = BarcodeScanning.getClient()
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
                val rawValue = barcodes.first().rawValue
                onSuccess(rawValue)
            } else {
                onSuccess(null)
            }
        }
        .addOnFailureListener { exception ->
            onError(exception)
        }
}

/**
 * Экран, который открывает камеру и сканирует QR-код в реальном времени.
 *
 * @param onQRCodeScanned Функция, вызываемая при успешном сканировании QR-кода.
 */
@Composable
fun CameraScannerScreen(
    onQRCodeScanned: (String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    var scannedResult by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    bindCameraUseCases(previewView, lifecycleOwner) { result ->
                        result?.let {
                            scannedResult = it
                            onQRCodeScanned(it)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        scannedResult?.let { result ->
            Text(
                text = "Найден QR-код: $result",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Привязывает CameraX к жизненному циклу и запускает анализ кадров с помощью ML Kit.
 *
 * @param previewView Элемент предварительного просмотра камеры.
 * @param lifecycleOwner Владелец жизненного цикла (необходим для привязки камеры).
 * @param onBarcodeFound Функция, вызываемая при обнаружении QR-кода.
 */
private fun bindCameraUseCases(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner?,
    onBarcodeFound: (String?) -> Unit
) {
    val context = previewView.context
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Use case для предварительного просмотра
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // Use case для анализа изображений
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            processImageProxy(imageProxy, onBarcodeFound)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner!!,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

/**
 * Обрабатывает кадр с камеры и пытается извлечь данные QR-кода.
 *
 * @param imageProxy Кадр, полученный с камеры.
 * @param onBarcodeFound Функция, вызываемая при обнаружении QR-кода.
 */
private fun processImageProxy(
    imageProxy: ImageProxy,
    onBarcodeFound: (String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onBarcodeFound(barcodes.first().rawValue)
                } else {
                    onBarcodeFound(null)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onBarcodeFound(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

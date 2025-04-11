package com.example.healthconnectsample.presentation.screen

import android.graphics.Bitmap
import android.graphics.ImageDecoder
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
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Экран настроек, где можно либо выбрать изображение из галереи для сканирования QR-кода,
 * либо запустить камеру для сканирования QR-кода в реальном времени.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var qrCodeText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Флаг, указывающий, что активирован режим камеры.
    var isCameraMode by remember { mutableStateOf(false) }

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
        // Кнопки для выбора способа сканирования
        Button(onClick = {
            // Режим выбора из галереи
            isCameraMode = false
            galleryLauncher.launch("image/*")
        }) {
            Text(text = "Выбрать изображение с QR-кодом")
        }
        Button(onClick = {
            // Включаем режим камеры
            isCameraMode = true
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Сканировать QR-код камерой")
        }

        // Если выбран режим камеры, отображаем превью с камеры
        if (isCameraMode) {
            CameraScannerScreen(
                onQRCodeScanned = { result ->
                    // При получении результата выключаем режим камеры
                    isCameraMode = false
                    qrCodeText = result ?: "QR-код не найден"
                }
            )
        }

        // Отображение результата сканирования
        qrCodeText?.let {
            Text(text = "Информация с QR-кода: $it", modifier = Modifier.padding(top = 16.dp))
        }
        errorMessage?.let {
            Text(text = "Ошибка: $it", modifier = Modifier.padding(top = 8.dp))
        }
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
    // Важно: для работы CameraX требуется, чтобы контекст реализовывал LifecycleOwner.
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
            // Можно настроить оформление результата сканирования
            Text(
                text = "Найден QR-код: $result",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Привязывает CameraX к циклу жизни и запускает анализ кадров с помощью ML Kit.
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

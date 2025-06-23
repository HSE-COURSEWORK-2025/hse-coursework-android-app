# HSE Coursework: приложение для Android

Это Android-приложение для взаимодействия с платформой Google Health Connect. Приложение позволяет собирать данные о здоровье пользователя и отправлять их наш [сервис сбора данных](https://github.com/HSE-COURSEWORK-2025/hse-coursework-backend-data-collection-service).

![](https://github.com/HSE-COURSEWORK-2025/hse-coursework-android-app/blob/master/android_demo.gif)

## Основные возможности
- Чтение различных типов данных о здоровье через Health Connect
- Сканирование QR-кодов с помощью камеры


## Используемые технологии
- Kotlin, Android SDK
- Jetpack Compose
- Health Connect (androidx.health.connect)
- CameraX, ML Kit Barcode Scanning
- OkHttp, Gson

## Сборка и запуск
1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/your-username/hse-coursework-android-app.git
   ```
2. Откройте проект в Android Studio.
3. Убедитесь, что установлены все необходимые зависимости.
4. Соберите и запустите приложение на устройстве с Android 10 (API 29) или выше. Для работы Health Connect требуется Android 10+ и установленное приложение Health Connect.

## Разрешения
Приложение запрашивает доступ к различным данным о здоровье, а также к камере. Все разрешения подробно указаны в `AndroidManifest.xml`.

## Структура проекта
- `app/` — основной модуль Android-приложения
- `build.gradle`, `settings.gradle` — конфигурация сборки
- `proguard-rules.pro` — правила для ProGuard

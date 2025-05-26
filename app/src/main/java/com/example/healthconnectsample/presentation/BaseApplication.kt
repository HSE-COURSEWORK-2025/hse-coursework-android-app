package com.example.healthconnectsample.presentation

import android.app.Application
import android.util.Log
import com.example.healthconnectsample.data.HealthConnectManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob

class BaseApplication : Application() {
    lateinit var healthConnectManager: HealthConnectManager
        private set

    override fun onCreate() {
        super.onCreate()
        healthConnectManager = HealthConnectManager(this)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalError", "Uncaught exception in thread ${thread.name}", throwable)
            // приложение не упадёт — просто залогируем
        }
    }

    /**
     * Общий [SupervisorJob] и [CoroutineExceptionHandler], которые можно
     * подключать к своим scope’ам, чтобы игнорировать ошибки в дочерних корутинах.
     */
    val appCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("GlobalError", "Unhandled coroutine exception", throwable)
    }
    val appSupervisorJob = SupervisorJob()
}

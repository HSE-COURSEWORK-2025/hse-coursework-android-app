package com.example.healthconnectsample.presentation.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity


class QrCodeScannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultIntent = Intent()
        resultIntent.putExtra("scanned_qr_code", "результат_сканирования")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
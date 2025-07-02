package com.mo.bell.utils

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QRCodeScanner(
    private val activity: AppCompatActivity,
    private val onResult: (String) -> Unit
) {

    private val scanLauncher: ActivityResultLauncher<ScanOptions> = 
        activity.registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                onResult(result.contents)
            }
        }

    /**
     * بدء مسح رمز QR
     */
    fun startScan() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("امسح رمز QR للجدول المدرسي")
            setCameraId(0) // الكاميرا الخلفية
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
            setCaptureActivity(CustomCaptureActivity::class.java)
        }
        
        scanLauncher.launch(options)
    }

    /**
     * نشاط مخصص لمسح رموز QR
     */
    class CustomCaptureActivity : CaptureActivity() {
        // يمكن تخصيص واجهة المسح هنا
    }
}


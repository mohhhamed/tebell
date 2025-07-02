package com.mo.bell.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mo.bell.service.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var serviceManager: ServiceManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // إعادة تشغيل الخدمات بعد إعادة تشغيل الجهاز أو تحديث التطبيق
                serviceManager.startAllServices()
            }
        }
    }
}


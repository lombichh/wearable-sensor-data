package com.example.wearablesensordata.utils

import android.app.ActivityManager
import android.content.Context


class ServiceUtils {

    companion object {
        fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)

            for (serviceInfo in runningServices) {
                if (serviceClass.name == serviceInfo.service.className) {
                    return true
                }
            }

            return false
        }
    }

}

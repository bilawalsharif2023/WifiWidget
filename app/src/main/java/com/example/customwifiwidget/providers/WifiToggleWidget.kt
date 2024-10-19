package com.example.customwifiwidget.providers

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.customwifiwidget.R

class WifiToggleWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "TOGGLE_WIFI") {
            if (hasLocationPermission(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    openNetworkSettings(context) // Open network settings on Android 10+
                } else {
                    toggleWifiAndData(context) // Toggle Wi-Fi & Mobile Data on Android 9 and below
                }
            } else {
                Toast.makeText(context, "Location Permission Required!", Toast.LENGTH_SHORT).show()
                openPermissionSettings(context)
            }
        }
    }

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val intent = Intent(context, WifiToggleWidget::class.java).apply {
            action = "TOGGLE_WIFI"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set the click listener on the ImageView
        views.setOnClickPendingIntent(R.id.toggleButton, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun toggleWifiAndData(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val isWifiEnabled = wifiManager.isWifiEnabled

        if (isWifiEnabled) {
            wifiManager.isWifiEnabled = false
            setMobileDataEnabled(context, telephonyManager, true)
            Toast.makeText(context, "Wi-Fi OFF, Mobile Data ON", Toast.LENGTH_SHORT).show()
        } else {
            wifiManager.isWifiEnabled = true
            setMobileDataEnabled(context, telephonyManager, false)
            Toast.makeText(context, "Wi-Fi ON, Mobile Data OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setMobileDataEnabled(context: Context, telephonyManager: TelephonyManager, enabled: Boolean) {
        try {
            val method = telephonyManager.javaClass.getDeclaredMethod("setDataEnabled", Boolean::class.java)
            method.isAccessible = true
            method.invoke(telephonyManager, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to toggle mobile data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNetworkSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening Network settings...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open Network settings!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

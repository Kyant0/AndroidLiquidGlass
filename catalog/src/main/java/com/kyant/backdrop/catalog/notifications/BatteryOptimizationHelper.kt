package com.kyant.backdrop.catalog.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper to manage battery optimization settings for reliable background notifications.
 * 
 * On many Android devices (especially Xiaomi, Oppo, Vivo, Samsung, OnePlus),
 * aggressive battery optimization kills background services including FCM,
 * preventing notifications when the app is not in recent apps.
 */
object BatteryOptimizationHelper {
    
    private const val TAG = "BatteryOptHelper"
    private const val PREFS_NAME = "battery_opt_prefs"
    private const val KEY_ASKED_FOR_EXEMPTION = "asked_for_exemption"
    
    /**
     * Check if the app is exempt from battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    /**
     * Request battery optimization exemption.
     * Returns true if request was made, false if already exempted or user already asked.
     */
    @SuppressLint("BatteryLife")
    fun requestBatteryOptimizationExemption(context: Context, forceAsk: Boolean = false): Boolean {
        // Already exempted
        if (isIgnoringBatteryOptimizations(context)) {
            Log.d(TAG, "App is already exempt from battery optimizations")
            return false
        }
        
        // Check if we already asked (unless force)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyAsked = prefs.getBoolean(KEY_ASKED_FOR_EXEMPTION, false)
        
        if (alreadyAsked && !forceAsk) {
            Log.d(TAG, "Already asked for battery optimization exemption")
            return false
        }
        
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            
            // Mark as asked
            prefs.edit().putBoolean(KEY_ASKED_FOR_EXEMPTION, true).apply()
            Log.d(TAG, "Requested battery optimization exemption")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request battery optimization exemption", e)
            // Try opening battery settings instead
            openBatterySettings(context)
            return false
        }
    }
    
    /**
     * Open battery settings for the app
     */
    fun openBatterySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery settings", e)
            // Fallback to general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open settings", e2)
            }
        }
    }
    
    /**
     * Open manufacturer-specific battery/autostart settings
     * Different OEMs have different settings locations
     */
    fun openManufacturerBatterySettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        Log.d(TAG, "Device manufacturer: $manufacturer")
        
        val intents = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> listOf(
                Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                Intent().setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")
            )
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> listOf(
                Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                Intent().setClassName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
            )
            manufacturer.contains("vivo") -> listOf(
                Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            )
            manufacturer.contains("samsung") -> listOf(
                Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
            )
            manufacturer.contains("oneplus") -> listOf(
                Intent().setClassName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            )
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            )
            manufacturer.contains("asus") -> listOf(
                Intent().setClassName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")
            )
            manufacturer.contains("nokia") -> listOf(
                Intent().setClassName("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity")
            )
            else -> emptyList()
        }
        
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Opened manufacturer battery settings: ${intent.component}")
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Failed to open: ${intent.component}")
            }
        }
        
        // Fallback to standard battery settings
        openBatterySettings(context)
        return false
    }
    
    /**
     * Check if we should show battery optimization prompt
     */
    fun shouldShowBatteryPrompt(context: Context): Boolean {
        if (isIgnoringBatteryOptimizations(context)) {
            return false
        }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_ASKED_FOR_EXEMPTION, false)
    }
    
    /**
     * Reset the "asked for exemption" flag (for testing or when user wants to be asked again)
     */
    fun resetAskedFlag(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ASKED_FOR_EXEMPTION, false).apply()
    }
}

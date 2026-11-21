package com.localcam.viewer

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CameraConfigStore(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)

    private fun createEncryptedPrefs(appContext: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getCamera(id: Int): CameraConfig {
        val defaultIp = if (id == 1) "192.168.0.10" else ""
        val defaultName = "Câmera $id"

        val prefix = "cam_${id}_"
        return CameraConfig(
            id = id,
            enabled = prefs.getBoolean("${prefix}enabled", true),
            name = prefs.getString("${prefix}name", defaultName) ?: defaultName,
            ip = prefs.getString("${prefix}ip", defaultIp) ?: defaultIp,
            port = prefs.getInt("${prefix}port", 554),
            path = prefs.getString("${prefix}path", "/onvif1") ?: "/onvif1",
            username = prefs.getString("${prefix}username", "admin") ?: "admin",
            password = prefs.getString("${prefix}password", "") ?: ""
        )
    }

    fun saveCamera(config: CameraConfig) {
        val prefix = "cam_${config.id}_"
        prefs.edit()
            .putBoolean("${prefix}enabled", config.enabled)
            .putString("${prefix}name", config.name.trim().ifEmpty { "Câmera ${config.id}" })
            .putString("${prefix}ip", config.ip.trim())
            .putInt("${prefix}port", config.port)
            .putString("${prefix}path", config.normalizedPath())
            .putString("${prefix}username", config.username.trim())
            .putString("${prefix}password", config.password)
            .apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "localcam_secure_prefs"
    }
}

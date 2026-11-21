package com.localcam.viewer

import android.net.Uri

data class CameraConfig(
    val id: Int,
    val enabled: Boolean,
    val name: String,
    val ip: String,
    val port: Int,
    val path: String,
    val username: String,
    val password: String
) {
    fun isConfigured(): Boolean {
        return enabled && ip.trim().isNotEmpty() && port in 1..65535
    }

    fun normalizedPath(): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) return "/onvif1"
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }

    fun buildRtspUri(): String {
        val cleanIp = ip.trim()
        val cleanPath = normalizedPath()
        val cleanUser = username.trim()

        val userInfo = if (cleanUser.isNotEmpty()) {
            val encodedUser = Uri.encode(cleanUser)
            val encodedPass = Uri.encode(password)
            if (password.isNotEmpty()) "$encodedUser:$encodedPass@" else "$encodedUser@"
        } else {
            ""
        }

        return "rtsp://$userInfo$cleanIp:$port$cleanPath"
    }

    fun buildSafeDisplayAddress(): String {
        val cleanIp = ip.trim().ifEmpty { "IP não definido" }
        return "$cleanIp:$port${normalizedPath()}"
    }
}

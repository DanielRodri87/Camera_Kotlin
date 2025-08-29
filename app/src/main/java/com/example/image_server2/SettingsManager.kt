package com.example.image_server2

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "camera_server_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        const val DEFAULT_IP = "192.168.1.16"
        const val DEFAULT_PORT = 4400
        
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_FIRST_RUN = "first_run"
    }

    fun saveServerSettings(ip: String, port: Int) {
        sharedPreferences.edit()
            .putString(KEY_SERVER_IP, ip)
            .putInt(KEY_SERVER_PORT, port)
            .apply()
    }

    fun getServerIp(): String {
        return sharedPreferences.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    fun getServerPort(): Int {
        return sharedPreferences.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
    }

    fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_RUN, true)
    }

    fun setFirstRunCompleted() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }
}

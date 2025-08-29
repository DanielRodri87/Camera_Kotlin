package com.example.image_server2

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerencia as configurações persistentes do aplicativo usando SharedPreferences.
 * Armazena informações como IP e porta do servidor e estado da primeira execução.
 * @property context Contexto do Android usado para acessar SharedPreferences
 */
class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "camera_server_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        /** IP padrão do servidor */
        const val DEFAULT_IP = "192.168.1.16"
        /** Porta padrão do servidor */
        const val DEFAULT_PORT = 4400
        
        /** Chave para armazenar IP do servidor */
        private const val KEY_SERVER_IP = "server_ip"
        /** Chave para armazenar porta do servidor */
        private const val KEY_SERVER_PORT = "server_port"
        /** Chave para controlar primeira execução */
        private const val KEY_FIRST_RUN = "first_run"
    }

    /**
     * Salva as configurações do servidor.
     * @param ip Endereço IP do servidor
     * @param port Porta do servidor
     */
    fun saveServerSettings(ip: String, port: Int) {
        sharedPreferences.edit()
            .putString(KEY_SERVER_IP, ip)
            .putInt(KEY_SERVER_PORT, port)
            .apply()
    }

    /**
     * Obtém o IP do servidor configurado.
     * @return String contendo o IP ou o IP padrão
     */
    fun getServerIp(): String {
        return sharedPreferences.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    /**
     * Obtém a porta do servidor configurada.
     * @return Int contendo a porta ou a porta padrão
     */
    fun getServerPort(): Int {
        return sharedPreferences.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
    }

    /**
     * Verifica se é a primeira execução do aplicativo.
     * @return true se for primeira execução, false caso contrário
     */
    fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_RUN, true)
    }

    /**
     * Marca que a primeira execução foi completada.
     */
    fun setFirstRunCompleted() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }
}

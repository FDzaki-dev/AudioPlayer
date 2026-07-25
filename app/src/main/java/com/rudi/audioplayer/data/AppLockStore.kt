package com.rudi.audioplayer.data

import android.content.Context
import java.security.MessageDigest

/** Stores a hashed PIN (never plaintext) and whether biometric unlock is preferred. */
class AppLockStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLockEnabled(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean = prefs.getString(KEY_PIN_HASH, null) == hash(pin)

    fun disableLock() {
        prefs.edit().remove(KEY_PIN_HASH).putBoolean(KEY_BIOMETRIC, false).apply()
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "app_lock"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}

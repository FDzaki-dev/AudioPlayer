package com.rudi.audioplayer.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores a salted, PBKDF2-stretched PIN hash (never plaintext, never a raw single-pass
 * hash) and tracks failed attempts with an escalating lockout. A bare SHA-256(pin) with
 * no salt is a lookup table away from being reversed, and with no attempt limit a 6-digit
 * PIN can be brute-forced in well under a second — this store fixes both.
 */
class AppLockStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    sealed class PinResult {
        object Success : PinResult()
        object Wrong : PinResult()
        data class LockedOut(val untilMillis: Long) : PinResult()
    }

    fun isLockEnabled(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .putInt(KEY_FAIL_COUNT, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    /** Current lockout end time (millis since epoch), or null if the user is free to try. */
    fun lockedOutUntil(): Long? {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return if (until > System.currentTimeMillis()) until else null
    }

    fun verifyPin(pin: String): PinResult {
        lockedOutUntil()?.let { return PinResult.LockedOut(it) }

        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return PinResult.Wrong
        val salt = decodedSalt()
        val matches = salt != null && constantTimeEquals(hash(pin, salt), storedHash)

        if (matches) {
            prefs.edit().putInt(KEY_FAIL_COUNT, 0).remove(KEY_LOCKOUT_UNTIL).apply()
            return PinResult.Success
        }

        val fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
        val lockoutMs = PinLockoutPolicy.lockoutDurationMillis(fails)
        val editor = prefs.edit().putInt(KEY_FAIL_COUNT, fails)
        return if (lockoutMs > 0) {
            val until = System.currentTimeMillis() + lockoutMs
            editor.putLong(KEY_LOCKOUT_UNTIL, until).apply()
            PinResult.LockedOut(until)
        } else {
            editor.apply()
            PinResult.Wrong
        }
    }

    fun disableLock() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_SALT)
            .putInt(KEY_FAIL_COUNT, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .putBoolean(KEY_BIOMETRIC, false)
            .apply()
    }

    private fun decodedSalt(): ByteArray? =
        prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    /** PBKDF2-HMAC-SHA1 is available on every API level this app targets (minSdk 23), unlike
     *  the SHA-256 variant of the same algorithm, which only landed in API 26. */
    private fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec)
        return key.encoded.joinToString("") { "%02x".format(it) }
    }

    /** Avoids leaking timing information about how many leading characters matched. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    /**
     * The first 4 wrong attempts cost nothing, so an honest typo never locks anyone out.
     * From the 5th attempt on, the lockout escalates — see [PinLockoutPolicy] for the formula
     * itself, kept separate so it can be unit-tested without a Context.
     */
    companion object {
        private const val PREFS_NAME = "app_lock"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_FAIL_COUNT = "fail_count"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val PBKDF2_ITERATIONS = 12000
    }
}

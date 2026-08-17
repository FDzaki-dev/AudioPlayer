package com.rudi.audioplayer.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Roadmap item #14 — Vault: songs moved here are excluded from every normal library view
 * (Home/Library, same mechanism as [LibraryFilterStore]'s hidden songs) and only listable again
 * from [VaultStore] itself, gated behind a PIN.
 *
 * Deliberately its OWN prefs file + OWN PBKDF2 hash/lockout state, not a reuse of
 * [AppLockStore] with a different prefs name — the two locks are semantically independent
 * (a user may want the app itself open with no PIN, but individual songs still gated), and
 * keeping them structurally separate means a bug in one PIN flow can never cross-contaminate
 * the other's stored hash/salt/lockout state. The escalating-lockout FORMULA is still shared
 * via [PinLockoutPolicy] (already a pure, Context-free object made for exactly this kind of
 * reuse) — only the hashing/storage plumbing around it is duplicated, and only ~15 lines of it.
 *
 * Vaulted-song membership and the PIN itself are intentionally decoupled from
 * [LibraryFilterStore]: hidden songs (that store) are a plain visibility toggle with no
 * protection, vault songs need a PIN to even list. [apply] gives Home/Library a one-line way to
 * additionally exclude vaulted songs, exactly like [LibraryFilterStore.apply] already does for
 * hidden ones.
 */
class VaultStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    sealed class PinResult {
        object Success : PinResult()
        object Wrong : PinResult()
        data class LockedOut(val untilMillis: Long) : PinResult()
    }

    fun isVaultEnabled(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

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

    /**
     * Turns the vault off entirely: clears the PIN AND un-vaults every song currently inside it
     * (they go straight back to being normal, fully visible library entries). A vault with no
     * PIN protecting it would just be a confusing extra hidden-songs list with no actual lock —
     * deliberately not left in that half-state; the caller is expected to confirm this
     * destructive-to-the-vault (not destructive-to-any-file) action with the user first.
     */
    fun disableVault() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_SALT)
            .putInt(KEY_FAIL_COUNT, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .remove(KEY_VAULTED_SONGS)
            .apply()
    }

    fun getVaultedSongIds(): Set<Long> =
        (prefs.getStringSet(KEY_VAULTED_SONGS, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun setSongVaulted(songId: Long, vaulted: Boolean) {
        val current = getVaultedSongIds().map { it.toString() }.toMutableSet()
        if (vaulted) current.add(songId.toString()) else current.remove(songId.toString())
        prefs.edit().putStringSet(KEY_VAULTED_SONGS, current).apply()
    }

    /** Same orphan-cleanup precedent as [FavoritesStore.pruneOrphans]/[RatingStore.pruneOrphans]
     *  (Gap List #9) — a vaulted ID pointing at a song no longer in the freshly-scanned library
     *  (deleted/moved/permission revoked) is dead weight, not a record worth keeping. No-op
     *  write if nothing was actually stale. */
    fun pruneOrphans(validIds: Set<Long>) {
        val current = getVaultedSongIds()
        val pruned = current.filter { it in validIds }.map { it.toString() }.toSet()
        if (pruned.size != current.size) {
            prefs.edit().putStringSet(KEY_VAULTED_SONGS, pruned).apply()
        }
    }

    /** Applies vault exclusion to an already-scanned song list — same one-line shape as
     *  [LibraryFilterStore.apply], meant to be chained with it at each call site. */
    fun apply(songs: List<Song>): List<Song> {
        val vaultedIds = getVaultedSongIds()
        if (vaultedIds.isEmpty()) return songs
        return songs.filterNot { it.id in vaultedIds }
    }

    private fun decodedSalt(): ByteArray? =
        prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    private fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec)
        return key.encoded.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    companion object {
        private const val PREFS_NAME = "vault"
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_SALT = "vault_pin_salt"
        private const val KEY_FAIL_COUNT = "vault_fail_count"
        private const val KEY_LOCKOUT_UNTIL = "vault_lockout_until"
        private const val KEY_VAULTED_SONGS = "vaulted_song_ids"
        private const val PBKDF2_ITERATIONS = 12000
    }
}

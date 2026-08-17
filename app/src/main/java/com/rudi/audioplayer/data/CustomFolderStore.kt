package com.rudi.audioplayer.data

import android.content.Context

/** A folder the user granted access to manually via the system folder picker.
 *
 * Gap List #5 (SAF parity — "Tangani permission revoke"): [permissionGranted] reflects
 * whether the OS still lists an active read grant for [uri] at the moment this was built
 * (checked against `ContentResolver.persistedUriPermissions`, the only reliable source —
 * grants can be revoked from outside the app, e.g. system Settings > "Manage other apps'
 * files" style screens, with zero broadcast to the app). Defaults `true` for older
 * construction sites; `PlayerViewModel.loadCustomFolderInfos()` is the only place that ever
 * computes the real value. */
data class CustomFolderInfo(
    val uri: String,
    val displayName: String,
    val permissionGranted: Boolean = true
)

/**
 * Remembers which folders the user has explicitly granted access to via the system's
 * folder picker (Storage Access Framework), for scanning audio MediaStore might not
 * have indexed yet.
 */
class CustomFolderStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFolderUris(): Set<String> = prefs.getStringSet(KEY_URIS, emptySet()) ?: emptySet()

    fun addFolder(uri: String) {
        prefs.edit().putStringSet(KEY_URIS, getFolderUris() + uri).apply()
    }

    fun removeFolder(uri: String) {
        prefs.edit().putStringSet(KEY_URIS, getFolderUris() - uri).apply()
    }

    companion object {
        private const val PREFS_NAME = "custom_folders"
        private const val KEY_URIS = "folder_uris"
    }
}

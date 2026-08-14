package com.rudi.audioplayer.data

/**
 * A rule-based playlist: unlike [Playlist] it never stores song IDs — it stores criteria and
 * [SmartPlaylistEngine] resolves the matching song list live against the current library every
 * time it's viewed, so a newly added song that fits the rules shows up automatically without the
 * user adding it by hand.
 *
 * Every criterion is optional (null/empty/0 = "don't filter on this"). When more than one is
 * set, a song must match ALL of them (AND) — matches the "narrow down" mental model of stacking
 * filters, not "any of these".
 */
data class SmartPlaylist(
    val id: String,
    val name: String,
    /** Empty = any folder. Matches [Song.folderName] (the short display name), same field the
     *  Folder tab already groups by — not [Song.folderPath], which the user never sees directly. */
    val folderNames: Set<String> = emptySet(),
    val minDurationMs: Long? = null,
    val maxDurationMs: Long? = null,
    /** 0 = no rating filter. 1-5 = song's stored rating must be >= this. */
    val minRating: Int = 0,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    /** Case-insensitive substring match against title, artist, or album. Blank = no filter. */
    val keyword: String = ""
) {
    /** True if every criterion is at its "don't filter" default — used to warn the user before
     *  saving a rule that would just match the entire library. */
    fun isEmpty(): Boolean =
        folderNames.isEmpty() && minDurationMs == null && maxDurationMs == null &&
            minRating == 0 && minYear == null && maxYear == null && keyword.isBlank()
}

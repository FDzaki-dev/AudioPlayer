package com.rudi.audioplayer.ui.lyrics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudi.audioplayer.data.lyrics.LyricsRepository
import com.rudi.audioplayer.data.lyrics.LyricsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class LyricsUiState {
    data object Idle : LyricsUiState()
    data object Loading : LyricsUiState()
    data class Found(val plainLyrics: String?, val syncedLyrics: String?) : LyricsUiState()
    data object NotFound : LyricsUiState()
}

// Batch 245 — Lyrics offline-first 3/4. Debounce 5 detik + skip-lagu-sama (spec item 5)
// diimplementasi DI SINI (bukan di PlaybackService pemanggil, batch 4/4) — 1 sumber kebenaran:
// PlaybackService cukup panggil `loadLyrics()` polos tiap `onMetadataChanged`, ViewModel yg
// jamin request bertubi-tubi (skip lagu / seek cepat / metadata event dobel dari Media3) tidak
// nembak query cache/API berkali-kali dalam window 5 detik.
class LyricsViewModel(context: Context) : ViewModel() {
    private val repository = LyricsRepository(context.applicationContext)

    private val _uiState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    // artist ke title (bukan title ke artist) - urutan bebas, cuma dipakai sbg composite key.
    private val request = MutableStateFlow<Triple<String, String, String?>?>(null)

    init {
        viewModelScope.launch {
            request
                .debounce(DEBOUNCE_MS)
                // Bandingin cuma artist+title (index ke 0,1) — kalau album beda tapi lagu sama
                // ttp dianggap "lagu sama", jangan re-query (album cuma ikut disimpan di
                // cache, bukan bagian identitas unik lagu yg dicek DAO).
                .distinctUntilChanged { old, new -> old?.first == new?.first && old?.second == new?.second }
                .collectLatest { req ->
                    if (req == null) return@collectLatest
                    _uiState.value = LyricsUiState.Loading
                    val result = repository.getLyrics(req.first, req.second, req.third)
                    _uiState.value = when (result) {
                        is LyricsResult.Found -> LyricsUiState.Found(result.plainLyrics, result.syncedLyrics)
                        LyricsResult.NotFound -> LyricsUiState.NotFound
                    }
                }
        }
    }

    fun loadLyrics(artist: String, title: String, album: String? = null) {
        request.value = Triple(artist, title, album)
    }

    companion object {
        private const val DEBOUNCE_MS = 5000L

        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LyricsViewModel(context.applicationContext) as T
        }
    }
}

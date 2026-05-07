package com.example.streamingtext.data.local

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.streamingtext.data.model.ImeHeights
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.imeMetricsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ime_metrics",
)

/**
 * Local data source for the per-orientation peak IME height. Backed by Jetpack
 * Preferences DataStore.
 */
class ImeMetricsDataSource(context: Context) {
    private val dataStore = context.applicationContext.imeMetricsDataStore

    val imeHeights: Flow<ImeHeights> = dataStore.data.map { prefs ->
        ImeHeights(
            portraitPx = prefs[KeyPortrait] ?: 0,
            landscapePx = prefs[KeyLandscape] ?: 0,
        )
    }

    suspend fun saveImeHeight(orientation: Int, heightPx: Int) {
        if (heightPx <= 0) return
        val key = keyFor(orientation)
        dataStore.edit { prefs ->
            // Mirror the in-memory peak-tracking: only grow.
            val current = prefs[key] ?: 0
            if (heightPx > current) prefs[key] = heightPx
        }
    }

    private fun keyFor(orientation: Int): Preferences.Key<Int> = when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> KeyLandscape
        else -> KeyPortrait
    }

    private companion object {
        val KeyPortrait = intPreferencesKey("ime_height_portrait_px")
        val KeyLandscape = intPreferencesKey("ime_height_landscape_px")
    }
}
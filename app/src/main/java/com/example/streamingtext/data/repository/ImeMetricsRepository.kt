package com.example.streamingtext.data.repository

import com.example.streamingtext.data.local.ImeMetricsDataSource
import com.example.streamingtext.data.model.ImeHeights
import kotlinx.coroutines.flow.Flow

/**
 * Reads/writes the most-recently-observed IME height per orientation. The emoji panel
 * uses these values to mirror the system keyboard's footprint, even on the very first
 * frame after a fresh app launch — before the user has opened the keyboard in the
 * current session.
 */
interface ImeMetricsRepository {
    val imeHeights: Flow<ImeHeights>
    suspend fun recordImeHeight(orientation: Int, heightPx: Int)
}

class ImeMetricsRepositoryImpl(
    private val dataSource: ImeMetricsDataSource,
) : ImeMetricsRepository {

    override val imeHeights: Flow<ImeHeights> = dataSource.imeHeights

    override suspend fun recordImeHeight(orientation: Int, heightPx: Int) {
        dataSource.saveImeHeight(orientation, heightPx)
    }
}
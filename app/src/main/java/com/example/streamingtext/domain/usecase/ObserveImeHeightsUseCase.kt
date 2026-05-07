package com.example.streamingtext.domain.usecase

import com.example.streamingtext.data.model.ImeHeights
import com.example.streamingtext.data.repository.ImeMetricsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Domain use case: observe persisted IME heights across orientations. The chat
 * presenter uses this to seed the emoji panel size at first composition.
 */
class ObserveImeHeightsUseCase(private val repository: ImeMetricsRepository) {
    operator fun invoke(): Flow<ImeHeights> = repository.imeHeights
}
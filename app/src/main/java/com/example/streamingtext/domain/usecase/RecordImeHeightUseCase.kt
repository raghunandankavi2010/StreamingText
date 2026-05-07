package com.example.streamingtext.domain.usecase

import com.example.streamingtext.data.repository.ImeMetricsRepository

/**
 * Domain use case: persist the latest observed peak IME height for the given
 * orientation, so subsequent app launches can render the emoji panel at the correct
 * size before the user has reopened the system keyboard.
 */
class RecordImeHeightUseCase(private val repository: ImeMetricsRepository) {
    suspend operator fun invoke(orientation: Int, heightPx: Int) {
        repository.recordImeHeight(orientation, heightPx)
    }
}
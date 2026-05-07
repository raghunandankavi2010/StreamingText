package com.example.streamingtext.data.model

/**
 * Most-recently observed IME (system keyboard) bottom inset, in pixels, for each
 * orientation. Stored separately because portrait and landscape keyboards have very
 * different heights on the same device.
 */
data class ImeHeights(
    val portraitPx: Int = 0,
    val landscapePx: Int = 0,
)
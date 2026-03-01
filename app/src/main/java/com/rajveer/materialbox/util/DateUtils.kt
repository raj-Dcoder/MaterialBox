package com.rajveer.materialbox.util

import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Converts a Date to a human-readable relative string.
 *
 * Examples:
 *   - Just now (< 1 min ago)
 *   - 5 min ago
 *   - 2 hours ago
 *   - Yesterday
 *   - 3 days ago
 *   - 27 Feb (older than 7 days, same year)
 *   - 27 Feb 2025 (different year)
 */
fun Date.toRelativeTimeString(): String {
    val now = System.currentTimeMillis()
    val diff = now - this.time

    // Handle future dates or very recent (< 1 min)
    if (diff < TimeUnit.MINUTES.toMillis(1)) return "Just now"

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    if (minutes < 60) return "$minutes min ago"

    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    if (hours < 24) return if (hours == 1L) "1 hour ago" else "$hours hours ago"

    val days = TimeUnit.MILLISECONDS.toDays(diff)
    if (days == 1L) return "Yesterday"
    if (days < 7) return "$days days ago"

    // Older than 7 days — show date
    val calendar = java.util.Calendar.getInstance().apply { time = this@toRelativeTimeString }
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val month = monthNames[calendar.get(java.util.Calendar.MONTH)]
    val year = calendar.get(java.util.Calendar.YEAR)

    return if (year == currentYear) {
        "$day $month"     // e.g., "27 Feb"
    } else {
        "$day $month $year" // e.g., "27 Feb 2025"
    }
}

package com.rajveer.materialbox.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val historyHeaderFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

fun todayDayKey(): String = dayKeyFormatter.format(Date())

fun String.toDayPlanHeader(): String {
    return try {
        val date = dayKeyFormatter.parse(this) ?: return this
        historyHeaderFormatter.format(date)
    } catch (_: Exception) {
        this
    }
}

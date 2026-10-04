package com.petcare.app.util

import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Times are stored as 24h "HH:mm" strings (so they sort correctly) and shown as 12h "8:00 AM".
 * Dates are stored as epoch millis.
 */
object TimeUtils {

    val dayShortNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") // index = DAY_OF_WEEK - 1

    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun todayDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    fun formatTime(hhmm: String): String {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, if (h >= 12) "PM" else "AM")
    }

    fun formatDate(millis: Long): String =
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(millis))

    fun formatDateTime(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(millis))

    fun pickTime(fm: FragmentManager, initial: String, title: String, onPicked: (String) -> Unit) {
        val parts = initial.split(":")
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(parts.getOrNull(0)?.toIntOrNull() ?: 8)
            .setMinute(parts.getOrNull(1)?.toIntOrNull() ?: 0)
            .setTitleText(title)
            .build()
        picker.addOnPositiveButtonClickListener {
            onPicked(String.format(Locale.US, "%02d:%02d", picker.hour, picker.minute))
        }
        picker.show(fm, "time_picker")
    }

    /** Picks a calendar date; the result is local midnight of that day in millis. */
    fun pickDate(fm: FragmentManager, initialMillis: Long, title: String, onPicked: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(toUtcDay(initialMillis))
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis -> onPicked(fromUtcDay(utcMillis)) }
        picker.show(fm, "date_picker")
    }

    /** Picks a date and then a time; returns the combined local millis. */
    fun pickDateTime(fm: FragmentManager, initialMillis: Long, onPicked: (Long) -> Unit) {
        pickDate(fm, initialMillis, "Select date") { dayMillis ->
            val initial = Calendar.getInstance().apply { timeInMillis = initialMillis }
            val initialTime = String.format(Locale.US, "%02d:%02d",
                initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE))
            pickTime(fm, initialTime, "Select time") { hhmm ->
                val (h, m) = hhmm.split(":").map { it.toInt() }
                val cal = Calendar.getInstance().apply {
                    timeInMillis = dayMillis
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                }
                onPicked(cal.timeInMillis)
            }
        }
    }

    // MaterialDatePicker works in UTC days; convert to/from local calendar days.
    private fun toUtcDay(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    private fun fromUtcDay(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}

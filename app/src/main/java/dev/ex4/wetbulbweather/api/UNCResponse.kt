package dev.ex4.wetbulbweather.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Serializable
data class APIResponse(
    @SerialName("hours_full")
    val hoursFull: List<Long>,
    val hours: List<Long>,
    val sun: List<Float>,
    val shade: List<Float>,
    val actual: List<Float>,
    @SerialName("WBranges")
    private val rangesIn: List<List<String>>,
    val lat: Double,
    val lon: Double,
    @SerialName("nearest grid n")
    val nearestGridN: String,
    @SerialName("nearest grid m")
    val nearestGridM: String
) {
    val ranges by lazy {
        rangesIn.map { WBRange(it[0].toLong(), it[1].toFloat(), it[2].toFloat()) }
    }

    fun getClosestHour(timestamp: Long = System.currentTimeMillis()): Long {
        var closestHour = Long.MAX_VALUE
        var closestHourDifference = Long.MAX_VALUE
        for (hour in hours) {
            val difference = abs(timestamp - hour)
            if (difference < closestHourDifference) {
                closestHour = hour
                closestHourDifference = difference
            }
        }
        return closestHour
    }

    fun getDisplayRanges(): List<WBRange> {
        // Get the offset of the current time zone from UTC
        val offset = Calendar.getInstance().run { get(Calendar.ZONE_OFFSET) + get(Calendar.DST_OFFSET) }

        return ranges
            .sortedBy { abs(System.currentTimeMillis() + offset - it.timestamp) }
            .take(4)
            .sortedBy { it.timestamp - System.currentTimeMillis() }
    }

    fun getClosestHourIndex(timestamp: Long = System.currentTimeMillis()): Int {
        return hours.indexOf(getClosestHour(timestamp))
    }
}

data class WBRange(val timestamp: Long, val min: Float, val max: Float)
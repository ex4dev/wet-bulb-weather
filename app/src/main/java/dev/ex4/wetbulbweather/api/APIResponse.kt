package dev.ex4.wetbulbweather.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    val ranges: List<List<String>>,
    val lat: Double,
    val lon: Double,
    @SerialName("nearest grid n")
    val nearestGridN: String,
    @SerialName("nearest grid m")
    val nearestGridM: String
) {
    fun getWetBulbRanges(): List<WBRange> = ranges.map { WBRange(it[0].toLong(), it[1].toFloat(), it[2].toFloat()) }

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

    fun getClosestHourIndex(timestamp: Long = System.currentTimeMillis()): Int {
        return hours.indexOf(getClosestHour(timestamp))
    }
}

data class WBRange(val timestamp: Long, val min: Float, val max: Float)
package dev.ex4.wetbulbweather.api

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class NWSForecastResponse(val properties: Properties) {

    @Serializable
    data class Properties(val periods: List<Period>) {

        @Serializable
        data class Period(
            val name: String,
            @Contextual val startTime: Date,
            @Contextual val endTime: Date,
            val temperature: Int,
            val temperatureUnit: String
        )
    }
}
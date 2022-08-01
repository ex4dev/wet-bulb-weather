package dev.ex4.wetbulbweather.api

import dev.ex4.wetbulbweather.api.serializer.DateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class NWSObservationResponse(val features: List<Feature>) {

    @Serializable
    data class Feature(val properties: Properties) {

        @Serializable
        data class Properties(
            @Serializable(with = DateSerializer::class) val timestamp: Date,
            val temperature: Reading,
            val dewpoint: Reading,
            val windDirection: Reading,
            val windSpeed: Reading,
            val windGust: Reading,
            val barometricPressure: Reading,
            val seaLevelPressure: Reading,
            val visibility: Reading,
            val maxTemperatureLast24Hours: Reading,
            val minTemperatureLast24Hours: Reading,
            val precipitationLast3Hours: Reading,
            val relativeHumidity: Reading,
            val windChill: Reading,
            val heatIndex: Reading,
        )

        @Serializable
        data class Reading(val unitCode: String?, val value: Float?) {
            override fun toString(): String = value.toString() + unitCode?.substringAfter(':')
        }

    }

}

package dev.ex4.wetbulbweather.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NWSResponse(val properties: Properties) {
    @Serializable
    data class Properties(val timeseries: List<Observation>) {
        @Serializable
        data class Observation(
            val time: String,
            val data: ObservationData,
            @SerialName("next_1_hours")
            val nextHour: PartialObservation? = null,
            @SerialName("next_6_hours")
            val nextSixHours: PartialObservation? = null,
            @SerialName("next_12_hours")
            val nextTwelveHours: PartialObservation? = null
        ) {
            @Serializable
            data class ObservationData(val instant: InstantData) {
                @Serializable
                data class InstantData(val details: InstantDetails) {
                    @Serializable
                    data class InstantDetails(
                        @SerialName("air_pressure_at_sea_level")
                        val airPressureAtSeaLevel: Float,
                        @SerialName("air_temperature")
                        val airTemperature: Float,
                        @SerialName("cloud_area_fraction")
                        val cloudAreaFraction: Float,
                        @SerialName("relative_humidity")
                        val relativeHumidity: Float,
                        @SerialName("wind_from_direction")
                        val windFromDirection: Float,
                        @SerialName("wind_speed")
                        val windSpeed: Float
                    )
                }
            }

            @Serializable
            data class PartialObservation(val summary: PartialObservationSummary? = null) {
                @Serializable
                data class PartialObservationSummary(
                    @SerialName("symbol_code")
                    val symbolCode: String
                )
            }
        }
    }
}

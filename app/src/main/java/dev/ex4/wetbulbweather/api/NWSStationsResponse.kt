package dev.ex4.wetbulbweather.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.hypot

@Serializable
class NWSStationsResponse(val features: List<Station>) {

    @Serializable
    data class Station(val id: String, val properties: Properties, val geometry: Geometry) {

        @Serializable
        data class Properties(
            val forecast: String,
            val name: String,
            @SerialName("@id") val id: String
        )

        @Serializable
        data class Geometry(val coordinates: List<Float>) {
            val lat get() = coordinates[0]
            val long get() = coordinates[1]
        }


    }

    fun getClosestStation(lat: Float, long: Float): Station? = features.minByOrNull {
        hypot(
            lat - it.geometry.lat,
            long - it.geometry.long
        )
    }
}

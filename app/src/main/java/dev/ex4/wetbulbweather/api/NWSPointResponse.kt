package dev.ex4.wetbulbweather.api

data class NWSPointResponse(
    val properties: Properties
) {
    data class Properties(val gridX: Int, val gridY: Int)
}
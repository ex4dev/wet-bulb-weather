package dev.ex4.wetbulbweather.api

import android.location.Location
import android.location.LocationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.reflect.KClass

object NWS {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = OkHttpClient()
    private const val BASE_URL = "https://api.weather.gov"

    @OptIn(InternalSerializationApi::class)
    fun <T : Any> getAbsolute(type: KClass<T>, url: String): T {
        println("Getting URL $url - expecting response of type $type.")
        val req = Request.Builder().url(url).header("User-Agent", "dev.ex4.wetbulbweather").build()
        val text = client.newCall(req).execute().body?.string()
        return json.decodeFromString(type.serializer(), text!!)
    }

    fun <T : Any> get(
        type: KClass<T>,
        endpoint: String,
        query: Map<String, String> = emptyMap()
    ): T {
        val queryString = query.entries.joinToString(",") { it.key + "=" + it.value }
        val url = "$BASE_URL$endpoint?$queryString"
        return getAbsolute(type, url)
    }

    inline fun <reified T : Any> get(endpoint: String, query: Map<String, String> = emptyMap()): T =
        get(T::class, endpoint, query)

    inline fun <reified T : Any> getAbsolute(url: String): T =
        getAbsolute(T::class, url)

    suspend fun getClosestStation(location: Location): NWSStationsResponse.Station? = withContext(Dispatchers.IO) {
        val response: NWSStationsResponse = get("/stations", mapOf("state" to "NC"))
        val station = response.getClosestStation(location.latitude.toFloat(), location.longitude.toFloat())
        println("Got closest station: $station")
        return@withContext station
    }

    suspend fun getStationReading(station: NWSStationsResponse.Station) =
        withContext(Dispatchers.IO) {
            println("Getting station readings from station: $station (id=${station.id})")
            return@withContext getAbsolute<NWSObservationResponse>(station.id + "/observations")
        }
}

package dev.ex4.wetbulbweather.api

import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import java.io.IOException
import java.time.Instant
import java.util.*
import kotlin.coroutines.suspendCoroutine

object API {

    private const val UNC_BASE_URL = "https://sercc.oasis.unc.edu"
    private const val UNC_ENDPOINT = "/wbgt_v4/dataNdates_v2_mixmodel.php"

    private const val NWS_BASE_URL = "https://api.met.no"
    private const val NWS_FORECAST_ENDPOINT = "/weatherapi/locationforecast/2.0/compact.json"

    private const val userAgent = "wbgt-app-android/1.0"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val client = OkHttpClient()

    /**
     * Requests WBGT data from UNC's Wet Bulb Global Temperature Tool:
     * @see <a href="https://convergence.unc.edu/tools/wbgt/">The UNC WBGT Tool</a>
     *
     * @param latitude The user's latitude
     * @param longitude The user's longitude
     * @param hour Seems to be either "12" or "06", representing 1:00 PM and 7:00 PM, respectively
     */
    suspend fun getWBGTForecast(latitude: Double, longitude: Double, hour: String, calendar: Calendar = Calendar.getInstance()): APIResponse? {
        if (Instant.now().toEpochMilli() - calendar.timeInMillis > 259200000) {
            Log.e("API", "Failed to get a valid response up to 3 days ago. Cancelling.")
            return null
        }
        val year = String.format("%02d", calendar.get(Calendar.YEAR))
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))

        val dateString = year + month + day + hour
        Log.i("API", "Date string: $dateString; y: $year, m: $month, d: $day, h: $hour")

        val url = "$UNC_BASE_URL$UNC_ENDPOINT?lat=$latitude&lon=$longitude&date=$dateString"

        return try {
            httpRequest(buildGetRequest(url))?.let {Log.i("API", it); json.decodeFromString<APIResponse>(it)}
        } catch (e: SerializationException) {
            Log.i("API", "Failed to deserialize API response. Some values may be null because the model has not been updated yet. Calling the API again with an older timestamp.")
            getWBGTForecast(latitude, longitude, hour, calendar.apply { add(Calendar.DAY_OF_MONTH, -1)})
        }
    }

    suspend fun getNWSForecast(latitude: Double, longitude: Double): NWSResponse? {
        val url = "$NWS_BASE_URL$NWS_FORECAST_ENDPOINT?lat=$latitude&lon=$longitude"
        return httpRequest(buildGetRequest(url, mapOf("User-Agent" to "wbgt-app-android")))?.let {
            json.decodeFromString<NWSResponse>(it)
        }
    }

    suspend fun getObservation(latitude: Double, longitude: Double): NWSResponse.Properties.Observation? {
        val forecast = getNWSForecast(latitude, longitude)
        return forecast?.properties?.timeseries?.firstOrNull()
    }

    private fun buildGetRequest(url: String, headers: Map<String, String>): Request {
        return Request.Builder().get().url(url).headers(headers.toHeaders()).build()
    }

    private fun buildGetRequest(url: String) =
        buildGetRequest(url, mapOf("User-Agent" to userAgent))

    private suspend fun httpRequest(request: Request): String? = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resumeWith(Result.success(response.body?.string()))
            }
        })
    }
}
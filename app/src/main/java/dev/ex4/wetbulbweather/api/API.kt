package dev.ex4.wetbulbweather.api

import android.util.Log
import dev.ex4.wetbulbweather.WetBulbWeather
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import java.io.IOException
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine


object API {

    private const val UNC_BASE_URL = "https://sercc.oasis.unc.edu"
    private const val UNC_ENDPOINT = "/wbgt_v4/dataNdates_v2_mixmodel.php"

    private const val NWS_BASE_URL = "https://api.met.no"
    private const val NWS_FORECAST_ENDPOINT = "/weatherapi/locationforecast/2.0/compact.json"

    private const val userAgent = "wbgt-app-android/1.0"

    private const val cacheSize = 10L * 1024L * 1024L // 10 MiB
    private val cache = Cache(WetBulbWeather.instance.applicationContext.cacheDir, cacheSize)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder().addNetworkInterceptor(CacheInterceptor()).cache(cache).build()

    /**
     * Requests WBGT data from UNC's Wet Bulb Global Temperature Tool:
     * @see <a href="https://convergence.unc.edu/tools/wbgt/">The UNC WBGT Tool</a>
     *
     * @param latitude The user's latitude
     * @param longitude The user's longitude
     * @param hour Seems to be either "12" or "06", representing 1:00 PM and 7:00 AM, respectively
     */
    suspend fun getWBGTForecast(latitude: Double, longitude: Double, hour: String, calendar: Calendar = Calendar.getInstance(), noCache: Boolean = false): APIResponse? {
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
            httpRequest(buildGetRequest(url, noCache = noCache))?.let {Log.i("API", it); json.decodeFromString<APIResponse>(it)}
        } catch (e: SerializationException) {
            Log.i("API", "Failed to deserialize API response. Some values may be null because the model has not been updated yet. Calling the API again with an older timestamp.")
            getWBGTForecast(latitude, longitude, hour, calendar.apply { add(Calendar.DAY_OF_MONTH, -1)}, noCache)
        }
    }

    suspend fun getNWSForecast(latitude: Double, longitude: Double, noCache: Boolean = false): NWSResponse? {
        val url = "$NWS_BASE_URL$NWS_FORECAST_ENDPOINT?lat=$latitude&lon=$longitude"
        return httpRequest(buildGetRequest(url, headers = mapOf("User-Agent" to "wbgt-app-android"), noCache))?.let {
            json.decodeFromString<NWSResponse>(it)
        }
    }

    suspend fun getObservation(latitude: Double, longitude: Double, noCache: Boolean = false): NWSResponse.Properties.Observation? {
        val forecast = getNWSForecast(latitude, longitude, noCache)
        return forecast?.properties?.timeseries?.firstOrNull()
    }

    private fun buildGetRequest(url: String, headers: Map<String, String> = mapOf("User-Agent" to userAgent), noCache: Boolean = false): Request {
        return Request.Builder().get().url(url).headers(headers.toHeaders()).apply { if (noCache) cacheControl(CacheControl.FORCE_NETWORK) }.build()
    }

    private suspend fun httpRequest(request: Request): String? = suspendCoroutine { continuation ->
        val beforeRequest = Instant.now()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("API", "Request failed in ${Instant.now().toEpochMilli() - beforeRequest.toEpochMilli()}ms (${call.request().url})")
                continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i("API", "Request completed in ${Instant.now().toEpochMilli() - beforeRequest.toEpochMilli()}ms (${call.request().url})")
                continuation.resumeWith(Result.success(response.body?.string()))
            }
        })
    }

    // https://stackoverflow.com/a/49455438
    class CacheInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response: Response = chain.proceed(chain.request())
            val cacheControl: CacheControl = CacheControl.Builder()
                .maxAge(120, TimeUnit.MINUTES)
                .build()
            return response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build()
        }
    }
}
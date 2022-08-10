package dev.ex4.wetbulbweather.api

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlin.coroutines.suspendCoroutine

object API {

    private const val BASE_URL = "https://sercc.oasis.unc.edu"
    private const val ENDPOINT = "/wbgt_v4/dataNdates_v2_mixmodel.php"
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2919.83 Safari/537.36"

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
    suspend fun getForecast(latitude: Double, longitude: Double, hour: String): APIResponse? {
        val calendar = Calendar.getInstance()

        val year = String.format("%02d", calendar.get(Calendar.YEAR))
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))

        val dateString = year + month + day + hour
        Log.i("API", "Date string: $dateString; y: $year, m: $month, d: $day, h: $hour")

        val url = "$BASE_URL$ENDPOINT?lat=$latitude&lon=$longitude&date=$dateString"
        val userAgent = System.getProperty("http.agent") ?: DEFAULT_USER_AGENT
        Log.i("API", "Requesting URL: $url with User-Agent: $userAgent")

        val request = Request.Builder()
            .get()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        val response = suspendCoroutine<String?> { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resumeWith(Result.success(response.body?.string()))
                }
            })
        }

        return response?.let { json.decodeFromString<APIResponse>(it) }
    }
}
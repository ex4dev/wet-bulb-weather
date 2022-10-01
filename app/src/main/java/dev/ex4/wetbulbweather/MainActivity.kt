package dev.ex4.wetbulbweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.*
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentContainerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ex4.wetbulbweather.databinding.ActivityMainBinding
import dev.ex4.wetbulbweather.api.API
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.suspendCoroutine
import kotlin.math.round

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.main_weather_refresh_layout)
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener {
                refresh(noCache = true)
            }
            refresh()
        }
    }

    fun refresh(noCache: Boolean = false) {
        findViewById<SwipeRefreshLayout>(R.id.main_weather_refresh_layout)?.isRefreshing = true
        // GET LOCATION
        Log.i(this::class.simpleName, "Getting location")
        val location = getUserLocation() ?: return
        val uncJob = CoroutineScope(Dispatchers.IO).launch {
            // LOAD DATA
            val response =
                API.getWBGTForecast(location.latitude, location.longitude, "06", noCache = noCache)
            // DISPLAY DATA
            val closestHour = response?.getClosestHour()
            val closestHourIndex = response?.hours?.indexOf(closestHour)
            val wbgtRanges = response?.ranges?.getOrNull(closestHourIndex ?: 0)
            withContext(Dispatchers.Main) {
                if (response == null || closestHourIndex == null || wbgtRanges == null) {
                    AlertDialog.Builder(this@MainActivity).setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.error_description))
                        .show()
                    return@withContext
                }
                findViewById<TextView>(R.id.primary_text).text = getString(R.string.wbgt_temperature, round(wbgtRanges.max).toInt())
                val riskIndex = when {
                    wbgtRanges.max < 80 -> 0
                    wbgtRanges.max < 85 -> 1
                    wbgtRanges.max < 88 -> 2
                    wbgtRanges.max < 90 -> 3
                    else -> 4
                }
                val intro = getString(R.string.risk_intro, wbgtRanges.min, wbgtRanges.max)
                val hint = resources.getStringArray(R.array.risk_description)[riskIndex]
                findViewById<TextView>(R.id.weather_explanation_text).text = "$intro $hint"
                findViewById<TextView>(R.id.weather_explanation_header).text = resources.getStringArray(R.array.risk_header)[riskIndex]
                findViewById<LinearLayout>(R.id.weather_explanation_card).backgroundTintList =
                    ColorStateList.valueOf(
                        resources.getColor(
                            if (wbgtRanges.max < 80) R.color.light_green
                            else if (wbgtRanges.max < 85) R.color.light_green
                            else if (wbgtRanges.max < 88) R.color.light_orange
                            else if (wbgtRanges.max < 90) R.color.light_red
                            else R.color.light_red, null
                        )
                    )
                findViewById<TextView>(R.id.temperature_visualization_shade).text = getString(R.string.shade_temperature, wbgtRanges.min)
                findViewById<TextView>(R.id.temperature_visualization_sun).text = getString(R.string.sun_temperature, wbgtRanges.max)

                // Temperature graph
                val tempGraph = findViewById<FragmentContainerView>(R.id.temperature_graph_view)
                val calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("ha", Locale.US)
                val offset = Calendar.getInstance().run { get(Calendar.ZONE_OFFSET) + get(Calendar.DST_OFFSET) }
                val ranges = response.getDisplayRanges()
                var highestMaxTemp = 0f
                var lowestMinTemp = 0f
                ranges.forEach { if (it.max > highestMaxTemp) highestMaxTemp = it.max; if (it.min < lowestMinTemp) lowestMinTemp = it.min }
                ranges.forEachIndexed { i, range ->
                    val col = tempGraph.findViewById<FragmentContainerView>(resources.getIdentifier("temperature_graph_col${i + 1}", "id", packageName))
                    calendar.timeInMillis = range.timestamp
                    val hourString = sdf.format(calendar.timeInMillis - offset)
                    col.findViewById<TextView>(R.id.temperature_graph_column_time).text = hourString
                    col.findViewById<TextView>(R.id.temperature_graph_column_high).text = range.max.toString()
                    col.findViewById<TextView>(R.id.temperature_graph_column_low).text = range.min.toString()
                    val background = col.findViewById<View>(R.id.temperature_graph_column_background)
                    val marginParams = background.layoutParams as ConstraintLayout.LayoutParams
                    val top = (highestMaxTemp - range.max) * 5
                    val bottom = (range.min - lowestMinTemp) * 5
                    println(bottom)
                    marginParams.topMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, top, resources.displayMetrics).toInt()
                    marginParams.height = -range.max.toInt() + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (col.height - 60 - bottom), resources.displayMetrics).toInt()
                    println(marginParams.height)
                    background.layoutParams = marginParams
                }
            }
        }
        val nwsJob = CoroutineScope(Dispatchers.IO).launch {
            // NWS DATA
            val observation = API.getObservation(location.latitude, location.longitude, noCache)
            val instant = observation?.data?.instant?.details
            val iconName =
                observation?.nextHour?.summary?.symbolCode?.substringBeforeLast('_') ?: "not found"
            Log.i(this::class.simpleName, "Icon name: $iconName")
            val icon = when {
                iconName.contains("snow") -> R.string.icon_snowflake
                iconName.contains("cloudy") -> R.string.icon_cloud
                iconName.contains("rain") -> R.string.icon_cloud_rain
                iconName.contains("thunder") -> R.string.icon_cloud_bolt
                iconName.contains("clear") && !iconName.contains("night") -> R.string.icon_sun
                iconName.contains("night") -> R.string.icon_moon
                else -> R.string.icon_sun
            }
            withContext(Dispatchers.Main) {
                if (observation == null || instant == null) {
                    AlertDialog.Builder(this@MainActivity).setTitle("Error")
                        .setMessage("Failed to retrieve current weather conditions.").show()
                    return@withContext
                }
                findViewById<IconTextView>(R.id.weather_icon).text = getString(icon)
                findViewById<TextView>(R.id.secondary_text_1).text =
                    getString(R.string.dry_temperature, instant.airTemperature.toFahrenheit())
                findViewById<TextView>(R.id.secondary_text_2).text =
                    getString(R.string.humidity, instant.relativeHumidity)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            // Wait for both coroutines to complete before hiding the loading indicator
            uncJob.join()
            nwsJob.join()
            // DO THIS LAST
            withContext(Dispatchers.Main) {
                findViewById<SwipeRefreshLayout>(R.id.main_weather_refresh_layout)?.isRefreshing = false
                findViewById<ProgressBar>(R.id.loading_progress_bar).visibility = View.GONE
            }
        }
    }

    /**
     * Converts this Float from celsius to fahrenheit
     * and rounds to the nearest tenth of a degree.
     */
    private fun Float.toFahrenheit(): Float = round((this * 1.8f + 32f) * 10) / 10

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun getUserLocation(): Location? = runBlocking {
        val provider = LocationManager.FUSED_PROVIDER
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val nc = findNavController(R.id.nav_host_fragment_content_main)

        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("WetBulbWeather", "No permission granted.")
            withContext(Dispatchers.Main) {
                findViewById<ProgressBar>(R.id.loading_progress_bar).visibility = View.GONE
                if (nc.currentDestination?.id != R.id.NoLocationPermissionFragment)
                    nc.navigate(R.id.action_FirstFragment_to_SecondFragment)
            }
            this@MainActivity.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0
            )
            return@runBlocking null
        }

        if (nc.currentDestination?.id == R.id.main_weather_refresh_layout) {
            nc.navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        return@runBlocking lm.getLastKnownLocation(provider)
            ?: suspendCoroutine { continuation ->
                Looper.prepare()
                lm.requestLocationUpdates(provider, 2000L, 10f) { location ->
                    continuation.resumeWith(Result.success(location))
                }
            }
    }
}
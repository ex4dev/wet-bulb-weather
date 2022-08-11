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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ex4.wetbulbweather.databinding.ActivityMainBinding
import dev.ex4.wetbulbweather.api.API
import kotlinx.coroutines.*
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

        binding.fab.setOnClickListener { view ->
            refresh()
        }

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.main_weather_refresh_layout)
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener {
                refresh()
            }
            refresh()
        }
    }

    fun refresh() {
        findViewById<SwipeRefreshLayout>(R.id.main_weather_refresh_layout)?.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            val location = getUserLocation() ?: return@launch
            API.getNWSForecast(location.latitude, location.longitude)
            val response = API.getWBGTForecast(location.latitude, location.longitude, "06")
            if (response == null) {
                AlertDialog.Builder(this@MainActivity).setTitle("Error").setMessage("Failed to retrieve Wet Bulb Globe Temperature information.").show()
                return@launch
            }
            val closestHour = response.getClosestHour()
            val closestHourIndex = response.hours.indexOf(closestHour)
            val wbgtRanges = response.getWetBulbRanges()[closestHourIndex]
            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.response).text = response.toString()
                findViewById<TextView>(R.id.primary_text).text = "${round(wbgtRanges.max).toInt()}°"
                findViewById<TextView>(R.id.weather_explanation_text).text =
                    if (wbgtRanges.max < 80) "Wet bulb globe temperature is between ${wbgtRanges.min}° (shade) and ${wbgtRanges.max}° (full sun). Take a 5 minute break from intense activity every 30 minutes."
                    else if (wbgtRanges.max < 85) "Wet bulb globe temperature is between ${wbgtRanges.min}° (shade) and ${wbgtRanges.max}° (full sun). Take a 5 minute break from intense activity every 25 minutes."
                    else if (wbgtRanges.max < 88) "Wet bulb globe temperature is between ${wbgtRanges.min}° (shade) and ${wbgtRanges.max}° (full sun). New or unconditioned athletes should have reduced intensity practice and modifications in clothing. Well-conditioned athletes should have more frequent rest breaks and hydration as well as cautious monitoring for symptoms of heat illness. Take a 5 minute break from intense activity every 20 minutes."
                    else if (wbgtRanges.max < 90) "Wet bulb globe temperature is between ${wbgtRanges.min}° (shade) and ${wbgtRanges.max}° (full sun). All athletes must be under constant observation and supervision. Remove pads and equipment. Take a 5 minute break from intense activity every 15 minutes."
                    else "Wet bulb globe temperature is between ${wbgtRanges.min} (shade) and ${wbgtRanges.max}° (full sun). Avoid intense outdoor activity."
                findViewById<TextView>(R.id.weather_explanation_header).text =
                    if (wbgtRanges.max < 80) "Almost No Risk"
                    else if (wbgtRanges.max < 85) "Low Risk"
                    else if (wbgtRanges.max < 88) "Moderate Risk"
                    else if (wbgtRanges.max < 90) "High Risk"
                    else "Extreme Risk"
                findViewById<LinearLayout>(R.id.weather_explanation_card).backgroundTintList = ColorStateList.valueOf(resources.getColor(
                    if (wbgtRanges.max < 80) R.color.light_green
                    else if (wbgtRanges.max < 85) R.color.light_green
                    else if (wbgtRanges.max < 88) R.color.light_orange
                    else if (wbgtRanges.max < 90) R.color.light_red
                    else R.color.light_red
                , null))
                findViewById<TextView>(R.id.temperature_visualization_shade).text = "${wbgtRanges.min}°"
                findViewById<TextView>(R.id.temperature_visualization_sun).text = "${wbgtRanges.max}°"
            }
            val observation = API.getObservation(location.latitude, location.longitude)
            val instant = observation?.data?.instant?.details
            val iconName = observation?.nextHour?.summary?.symbolCode?.substringBeforeLast('_') ?: "sun"
            val icon = when {
                iconName.contains("snow") -> R.string.icon_snowflake
                iconName.contains("cloudy") -> R.string.icon_cloud
                iconName.contains("rain") -> R.string.icon_cloud_rain
                iconName.contains("thunder") -> R.string.icon_cloud_bolt
                iconName.contains("clear") -> R.string.icon_sun
                else -> R.string.icon_sun
            }
            if (observation == null || instant == null) {
                AlertDialog.Builder(this@MainActivity).setTitle("Error").setMessage("Failed to retrieve current weather conditions.").show()
                return@launch
            }
            withContext(Dispatchers.Main) {
                findViewById<IconTextView>(R.id.weather_icon).text = getString(icon)
                findViewById<TextView>(R.id.secondary_text_1).text = "Temperature: ${instant.airTemperature.toFahrenheit()}°"
                findViewById<TextView>(R.id.secondary_text_2).text = "Humidity: ${instant.relativeHumidity}%"
            }

            findViewById<SwipeRefreshLayout>(R.id.main_weather_refresh_layout)?.isRefreshing = false
        }
    }

    private fun Float.toFahrenheit(): Float = this * 1.8f + 32f

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
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
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ex4.wetbulbweather.api.NWS
import dev.ex4.wetbulbweather.api.NWSObservationResponse
import dev.ex4.wetbulbweather.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.text.DateFormat
import kotlin.coroutines.suspendCoroutine

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
            val result: NWSObservationResponse.Feature
            try {
                val station = NWS.getClosestStation(getUserLocation() ?: return@launch)!! //todo error handling for when this is null
                result = NWS.getStationReading(station).getMostRecentObservation()!! //todo error handling for when this is null
            } catch (e: Throwable) {
                e.printStackTrace()
                return@launch
            }
            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.response).text = result.toString()

                // LAST UPDATE
                // you are going to hate the way i do this :)
                findViewById<LinearLayout>(R.id.last_update_card).backgroundTintList =
                    when (result.properties.timestamp.time - System.currentTimeMillis()) {
                        in -1800000 .. 0 -> ColorStateList.valueOf(resources.getColor(R.color.light_green, null)) // in the last 30 minutes = green
                        in -3600000 .. 0 -> ColorStateList.valueOf(resources.getColor(R.color.light_orange, null)) // in the last 60 minutes = green
                        else -> ColorStateList.valueOf(resources.getColor(R.color.light_red, null)) // otherwise = red
                    }
                findViewById<TextView>(R.id.last_update_text).text = DateFormat.getDateTimeInstance().format(result.properties.timestamp)

                // WIND
                findViewById<LinearLayout>(R.id.wind_card).backgroundTintList = ColorStateList.valueOf(resources.getColor(
                    when (result.properties.windSpeed.value!!.toInt()) {
                        in 0 .. 40 -> R.color.light_green
                        in 0 .. 60 -> R.color.light_orange
                        else -> R.color.light_red
                    }
                , null))
                findViewById<TextView>(R.id.wind_text).text = "Wind Speed: ${result.properties.windSpeed}\nWind Gust: ${result.properties.windGust.value}\nWind Chill: ${result.properties.windChill.value}\nWind Direction: ${result.properties.windDirection.value}"

                findViewById<TextView>(R.id.secondary_text_1).text = "Temperature: ${result.properties.temperature.value?.toFahrenheit().toString()}°"
                findViewById<TextView>(R.id.secondary_text_2).text = "Humidity: ${result.properties.relativeHumidity.value?.toString()}%"
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
            nc.navigate(R.id.action_FirstFragment_to_SecondFragment)
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
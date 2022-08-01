package dev.ex4.wetbulbweather

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.os.Looper
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import dev.ex4.wetbulbweather.api.NWS
import dev.ex4.wetbulbweather.api.NWSObservationResponse
import dev.ex4.wetbulbweather.databinding.ActivityMainBinding
import kotlinx.coroutines.*
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

            CoroutineScope(Dispatchers.IO).launch {
                val result: NWSObservationResponse.Feature
                try {
                    val station = NWS.getClosestStation(getUserLocation()!!)!!
                    result = NWS.getStationReading(station).features.first()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@MainActivity).setMessage(
                        result.toString()
                    ).setTitle("W").setPositiveButton("WWWWW") { dialog, which -> dialog.dismiss() }
                        .show()
                }
            }
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()
        }
    }

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

        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            this@MainActivity.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0
            )
            return@runBlocking null
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
package dev.ex4.wetbulbweather

import android.app.Application

class WetBulbWeather : Application() {
    companion object {
        lateinit var instance: WetBulbWeather
    }
    override fun onCreate() {
        instance = this
        super.onCreate()
    }
}
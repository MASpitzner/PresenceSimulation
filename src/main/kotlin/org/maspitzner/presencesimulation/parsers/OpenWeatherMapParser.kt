package org.maspitzner.presencesimulation.parsers

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.maspitzner.presencesimulation.models.WeatherDataEntry
import java.io.File


/**
 * Implements the reading of an OpenWeatherMap Historical Data Bulk in JSON format.
 * No instantiation needed therefore only static functionality.
 * */

class OpenWeatherMapParser private constructor() {
    companion object {

        /**
         * OpenWeatherMap data in JSON format, uses Gson.
         * @param path String path to weather data.
         * @return List of WeatherDataEntries representing every entry in the weather data.
         *
         * @see WeatherDataEntry
         */

        fun parse(path: String): List<WeatherDataEntry> {

            val plainText = File(path).readText().trim()
            val listType = object : TypeToken<List<WeatherDataEntry>>() {}.type
            val json = Gson().fromJson<List<WeatherDataEntry>>(
                plainText,
                listType
            )
            json.forEach { it.transform() }
            return json.orEmpty()

        }
    }
}

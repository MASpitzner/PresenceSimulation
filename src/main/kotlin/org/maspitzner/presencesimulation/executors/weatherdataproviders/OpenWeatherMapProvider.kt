package org.maspitzner.presencesimulation.executors.weatherdataproviders

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.maspitzner.presencesimulation.models.LogEntry
import org.maspitzner.presencesimulation.models.WeatherDataEntry
import org.maspitzner.presencesimulation.parsers.OpenWeatherMapParser
import org.maspitzner.presencesimulation.utils.configuration.Configuration
import java.io.File

/**
 * Class implementing necessary functionality as described in [WeatherProvider]
 * This class focuses on OpenWeather Data as Json Bulk
 * Implements [WeatherProvider]
 * @see [WeatherProvider]
 */
class OpenWeatherMapProvider(override val config: Configuration) : WeatherProvider {
    private var weatherData = mutableListOf<WeatherDataEntry>()
    private val lastModified = File(config.weatherDataPath).lastModified()
    private var lastRead: DateTime = DateTime.now().plusDays(1)

    /**
     * Checks whether a log entry has corresponding weather data
     * currently not used. Might be of interest in future extensions.
     * @param logEntry the LogEntry to check
     * @return Boolean indicating whether it has a corresponding weather data entry
     */
    override fun isInWeatherData(logEntry: LogEntry): Boolean {
        var isInData = false
        weatherData.windowed(2, 1) {
            if (logEntry.timeStamp.isBefore(DateTime(it[1].getJodaTimeFormat(), DateTimeZone.UTC)) &&
                logEntry.timeStamp.isAfter(DateTime(it[0].getJodaTimeFormat(), DateTimeZone.UTC))
            ) {
                isInData = true
            }
        }
        return isInData
    }

    /**
     * Assigns weather data to a log entry if providable.
     * currently not used. Might be of interest in future extensions.
     * @param logEntry the LogEntry to assign weather data to
     */
    override fun assignWeatherInformation(logEntry: LogEntry) {
        weatherData.windowed(2, 1) {
            if (logEntry.timeStamp.isBefore(DateTime(it[1].getJodaTimeFormat(), DateTimeZone.UTC)) &&
                logEntry.timeStamp.isAfter(DateTime(it[0].getJodaTimeFormat(), DateTimeZone.UTC))
            ) {
                logEntry.setWeather(it[0])
            }
        }
    }

    /**
     * Returns a List of weather data acquired by a static file or similar sources
     * @return the acquired weather data as List of WeatherDataEntries
     */
    override fun getStaticWeatherData(): List<WeatherDataEntry> {
        if (lastRead.isBeforeNow && DateTime(lastModified).isBefore(lastRead) && weatherData.isNotEmpty())
            return weatherData
        else {
            weatherData = OpenWeatherMapParser.parse(config.weatherDataPath).toMutableList()
            lastRead = DateTime.now()
        }
        return weatherData
    }

}
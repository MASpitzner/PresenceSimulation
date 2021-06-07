package org.maspitzner.presencesimulation.executors.weatherdataproviders

import org.maspitzner.presencesimulation.models.LogEntry
import org.maspitzner.presencesimulation.models.WeatherDataEntry
import org.maspitzner.presencesimulation.utils.configuration.Configuration

/**
 * Mock class implemented for debugging and integration purposes
 * Implements [WeatherProvider]
 * @see [WeatherProvider]
 */
class MockWeatherDataProvider(override val config: Configuration) : WeatherProvider {
    init {
        println("MockWeatherDataProvider instantiated")
    }

    /**
     * Checks whether a log entry has corresponding weather data
     * currently not used. Might be of interest in future extensions.
     * @param logEntry the LogEntry to check
     * @return since this is a mock class returns always true
     */
    override fun isInWeatherData(logEntry: LogEntry): Boolean {
        println("checking whether weather data is providable")
        return true
    }

    /**
     * Assigns weather data to a log entry if providable.
     * Since this is a Mock class, doesn't do anything useful
     * @param logEntry the LogEntry to assign weather data to
     */
    override fun assignWeatherInformation(logEntry: LogEntry) {
        println("assigning whether weather data to log entry")
    }

    /**
     * Returns a List of weather data acquired by a static file or similar sources
     * @return since this is a mock class returns always an empty list
     */
    override fun getStaticWeatherData(): List<WeatherDataEntry> {
        return mutableListOf()
    }
}
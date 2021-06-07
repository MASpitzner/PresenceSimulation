package org.maspitzner.presencesimulation.executors.weatherdataproviders

import org.maspitzner.presencesimulation.models.LogEntry
import org.maspitzner.presencesimulation.models.WeatherDataEntry
import org.maspitzner.presencesimulation.utils.configuration.Configuration

/**
 * Interface describing functionalities associated with WeatherData acquisition
 */
interface WeatherProvider {
    val config: Configuration

    /**
     * Checks whether a log entry has corresponding weather data
     * currently not used. Might be of interest in future extensions.
     * @param logEntry the LogEntry to check
     * @return Boolean indicating whether it has a corresponding weather data entry
     */
    fun isInWeatherData(logEntry: LogEntry): Boolean

    /**
     * Assigns weather data to a log entry if providable.
     * currently not used. Might be of interest in future extensions.
     * @param logEntry the LogEntry to assign weather data to
     */
    fun assignWeatherInformation(logEntry: LogEntry)

    /**
     * Returns a List of weather data acquired by a static file or similar sources
     * @return the acquired weather data as List of WeatherDataEntries
     */
    fun getStaticWeatherData(): List<WeatherDataEntry>
}
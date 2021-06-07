package org.maspitzner.presencesimulation.evaluation

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import org.maspitzner.presencesimulation.models.WeatherDataEntry
import org.maspitzner.presencesimulation.utils.datahandling.PowerLaw
import kotlin.random.Random

/**
 * Generates Random log data based on a Stochastic Distribution Model.
 * Needed for the synthetic evaluation data.
 */
class RandomLogGenerator {
    private val startOfDate = DateTime("2019-11-18T00:00:00.001+00:00").withZone(DateTimeZone.UTC)

    /**
     * Generates a Log as List Of Events.
     * The way the activations of the different Devices will be uniformly distributed.
     * @param numberOfUniqueDevices: The number of pairwise different Devices,
     * that shall be included in the generated log.
     * @param numberOfEvents: The number of events that shall occur in the generated log.
     * @param start: DateTime at which we want the random gen to start.
     * Defaults arbitrarily to "2019-11-18T00:00:00.001+00:00"
     * @param maxWaitingTime: An upper bound to the length of the waiting time between two events
     *  to avoid having no event at all for an unrealistic duration of time
     *
     * @return The list of events as Log
     */
    fun generateUniformLog(
        numberOfUniqueDevices: Int,
        numberOfEvents: Int,
        start: DateTime = startOfDate,
        maxWaitingTime: Int = 7200000,
        randomStart: Boolean = false,
        weatherData: List<WeatherDataEntry>
    ): Log {

        val result = mutableListOf<Pair<Int, DateTime>>()
        val generator = Random(0)

        var newTimeStamp = getStartTime(randomStart, weatherData, start)

        (0 until numberOfEvents).forEach { _ ->
            val device = (generator.nextDouble() * numberOfUniqueDevices).toInt()
            val waitingTime = (generator.nextDouble() * maxWaitingTime).toInt()
            newTimeStamp = newTimeStamp.plusMillis(waitingTime)
            result.add(Pair(device, newTimeStamp))

        }
        return buildLog(result, weatherData)
    }


    /**
     * Generates a Log as List Of Events.
     * The way the activations of the different Devices
     * will be distributed according to a power law distribution.
     *
     * @param numberOfUniqueDevices: The number of pairwise different Devices,
     * that shall be included in the generated log.
     * @param numberOfEvents: The number of events that shall occur in the generated log.
     *
     * @return The list of events as Log.
     */
    fun generateZipfianLog(
        numberOfUniqueDevices: Int,
        numberOfEvents: Int,
        start: DateTime = startOfDate,
        maxWaitingTime: Int = 7200000,
        randomStart: Boolean = false,
        weatherData: List<WeatherDataEntry>
    ): Log {
        val result = mutableListOf<Pair<Int, DateTime>>()
        val timeGenerator = Random(0)
        val labelGenerator = PowerLaw()

        var newTimeStamp = getStartTime(randomStart, weatherData, start)

        (0 until numberOfEvents).forEach { _ ->
            val device = labelGenerator.getZipfInt(numberOfUniqueDevices)
            val waitingTime = (timeGenerator.nextDouble() * maxWaitingTime).toInt()
            newTimeStamp = newTimeStamp.plusMillis(waitingTime)
            result.add(Pair(device, newTimeStamp))

        }

        return buildLog(result, weatherData)
    }

    /**
     * Calculates the base timestamp for synthetic evaluation logs.
     * @param randomStart indicates whether to use a random starting timestamp
     * @param weatherData the available weather data to select a viable timestamp from
     * @param start a possible base timestamp
     * @return start timestamp as DateTime object
     */
    private fun getStartTime(randomStart: Boolean, weatherData: List<WeatherDataEntry>, start: DateTime) =
        if (randomStart) {
            val randomTime = weatherData.random().dayTimeIso?.split(" ") ?: listOf("", "")
            try {
                DateTime("${randomTime[0]}T${randomTime[1]}").withZone(DateTimeZone.UTC)
            } catch (e: Exception) {
                when (e) {
                    is org.joda.time.IllegalInstantException -> DateTime("${randomTime[0]}T04:0:00").withZone(
                        DateTimeZone.UTC
                    )
                    else -> throw e
                }
            }
        } else {
            start
        }

    /**
     * Builds a log from the generated (Label,Timestamp)-Pairs and attributes them with weather data
     * @param result the list of (Label,Timestamp)-Pairs
     * @param weatherData the weather data to choose data from
     * @return A converted Log
     */
    private fun buildLog(result: MutableList<Pair<Int, DateTime>>, weatherData: List<WeatherDataEntry>): Log {
        val rawLog = result.map { LogEntry(it.second, it.first) } as ArrayList
        val log = Log(rawLog)

        log.setWeatherInformation(weatherData)
        log.setWeatherLabels()
        log.scaleLog()

        return log
    }


}




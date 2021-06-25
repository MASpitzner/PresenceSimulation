package org.maspitzner.presencesimulation.executors.simulators

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.executors.logproviders.LogProvider
import org.maspitzner.presencesimulation.executors.logproviders.MockLogProvider
import org.maspitzner.presencesimulation.executors.logproviders.OpenHABLogProvider
import org.maspitzner.presencesimulation.executors.weatherdataproviders.MockWeatherDataProvider
import org.maspitzner.presencesimulation.executors.weatherdataproviders.OpenWeatherMapProvider
import org.maspitzner.presencesimulation.executors.weatherdataproviders.WeatherProvider
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import org.maspitzner.presencesimulation.models.WeatherDataEntry
import org.maspitzner.presencesimulation.simulation.events.*
import org.maspitzner.presencesimulation.simulation.timestamps.*
import org.maspitzner.presencesimulation.utils.configuration.*
import org.maspitzner.presencesimulation.utils.filehandling.createFileIfNotExists
import java.io.File

/**
 * Class implementing functionalities needed to run a simulation based on OpenHab data
 * @param config a [Configuration] object specifying the needed parameters
 */
class OpenHABSimulator(override val config: Configuration) : Simulator {

    // the object providing the log data
    private val logProvider: LogProvider

    // the object providing weather data
    private val weatherProvider: WeatherProvider

    // the type of model used for timestamp simulation
    private val timestampModel: TimestampGenerator

    // the type of model used for label simulation
    private val eventModel: Model

    // the log representing the training data
    private val log: Log

    //the weather data representing the features
    private val weatherData: List<WeatherDataEntry>

    /*
     * Initializing the described objects
     */
    init {
        println("initializing OpenHABSimulator...")
        println("Starting with Log ...")
        logProvider = determineLogProvider()
        log = logProvider.getStaticLog()
        println("Done with Log!")
        println("Starting with WeatherData ...")
        weatherProvider = determineWeatherDataProvider()
        weatherData = weatherProvider.getStaticWeatherData()
        log.setWeatherInformation(weatherData)
        log.scaleLog()
        println("Done with WeatherData!")
        println("Training TimestampModel ...")
        timestampModel = determineTimestampModel()
        when (timestampModel) {
            is FitableTimestampGenerator -> timestampModel.fit(log)
        }
        println("Training done!")
        println("Training eventModel ...")
        eventModel = determineEventModel()
        eventModel.fit(log)
        println("Training done!")
    }

    /**
     * Runs a Simulation and returns the created result as a List of (Label,Timestamp)-Pairs
     * @param numberOfEntries the number of events to generate
     * @param untilTime a timestamp until which the simulation generates events
     * @return the generated (Label,Timestamp)-Pairs
     */
    private fun runSimulation(numberOfEntries: Int, untilTime: DateTime?): ArrayList<Pair<Int, DateTime>> {
        val testSet = log.getEntries().subList((log.size * 0.8).toInt(), log.size)
        val generatedLog = ArrayList<Pair<Int, DateTime>>()
        var deviceLabel = eventModel.predict(log)
        var baseTime = timestampModel.generateTimestamp(deviceLabel)
        val entry = LogEntry(baseTime, deviceLabel)
        val entries = ArrayList<LogEntry>()
        entries.add(entry)
        generatedLog.add(Pair(deviceLabel, baseTime))
        if (untilTime != null) {
            while (baseTime.isBefore(untilTime)) {
                deviceLabel = eventModel.predict(entries)
                baseTime = timestampModel.generateTimestamp(deviceLabel)
                generatedLog.add(Pair(deviceLabel, baseTime))
                entries.add(LogEntry(baseTime, deviceLabel))

            }
        } else {
            var i = 1
            testSet.windowed(10, 1).forEach {
                if (i < numberOfEntries) {
                    val input = ArrayList(it)
                    deviceLabel = eventModel.predict(input)
                    baseTime = timestampModel.generateTimestamp(deviceLabel)
                    generatedLog.add(Pair(deviceLabel, baseTime))
                    i++
                } else {
                    return@forEach
                }
            }
        }

        return generatedLog

    }

    /**
     * Runs a console simulation, simulates the events and prints them to the console
     */
    override fun runConsoleSimulation() {
        val log = runSimulation(config.numOfEvents, config.untilTime)
        log.forEach {
            println("${it.second}: ${it.first} (${this.log.getOutMapping(it.first)})")
        }
    }

    /**
     * Runs a Simulation with specified parameters by the [Configuration] object
     * Results are printed a filed specified in the [Configuration] object
     */
    override fun runLogSimulation() {
        val log = runSimulation(config.numOfEvents, config.untilTime)
        val outFile = File(config.outputPath)
        createFileIfNotExists(config.outputPath)
        val stringRep = StringBuilder()
        log.forEach {
            stringRep.append("${it.second}: ${it.first} (${this.log.getOutMapping(it.first)})\n")
        }
        outFile.writeText(stringRep.toString())
    }

    /**
     * Used to determine the [LogProvider] to use based on the [Configuration] object
     * @return the [LogProvider] to use
     */
    private fun determineLogProvider(): LogProvider {
        return when (config.logProvider) {
            LogProviderTypes.MOCK_PROVIDER -> MockLogProvider(config)
            LogProviderTypes.OPENHAB_PROVIDER -> OpenHABLogProvider(config)
        }
    }

    /**
     * Used to determine the [WeatherProvider] to use based on the [Configuration] object
     * @return the [WeatherProvider] to use
     */
    private fun determineWeatherDataProvider(): WeatherProvider {
        return when (config.weatherDataProvider) {
            WeatherDataProviderTypes.MOCK_PROVIDER -> MockWeatherDataProvider(config)
            WeatherDataProviderTypes.OPEN_WEATHER_MAP_PROVIDER -> OpenWeatherMapProvider(config)
        }
    }

    /**
     * Used to determine the [Model] to use for label simulation based on the [Configuration] object
     * @return the [Model] to use
     */
    private fun determineEventModel(): Model {
        return when (config.eventModelType) {
            EventModelTypes.MOCK_EVENT_MODEL -> MockEventModel()
            EventModelTypes.MKM -> MarkovModel()
            EventModelTypes.CMKM -> ClusteredBasedMarkovModel()
            EventModelTypes.MLPM -> MLPModel()
            EventModelTypes.LSTMM -> LSTMModel()
            else -> MockEventModel()
        }
    }

    /**
     * Used to determine the [TimestampGenerator] to use for timestamp simulation based on the [Configuration] object
     * @return the [TimestampGenerator] to use
     */
    private fun determineTimestampModel(): TimestampGenerator {
        return when (config.timestampModelType) {
            TimestampModelTypes.MOCK_TIMESTAMP_MODEL -> MockTimestampModel(DateTime.now())
            TimestampModelTypes.UTSM -> UniformTimestampGenerator(DateTime.now())
            TimestampModelTypes.PTSM -> PoissonTimestampGenerator(DateTime.now())
            TimestampModelTypes.CPTSM -> ClassBasedPoissonTimestampGenerator(DateTime.now())
            TimestampModelTypes.TFTSM -> TimeFrameTimestampGenerator(DateTime.now())
            TimestampModelTypes.LSTMTSM -> LSTMTimestampGenerator(DateTime.now())
            else -> MockTimestampModel(DateTime.now())
        }
    }
}
package org.maspitzner.presencesimulation.parsers

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.utils.configuration.*
import org.maspitzner.presencesimulation.utils.exceptions.MissingParameterException
import org.maspitzner.presencesimulation.utils.exceptions.NoConfigurationException
import org.maspitzner.presencesimulation.utils.exceptions.WrongParameterChoiceException
import java.io.File

/**
 * Class which provides the functionality to extract CommandlineArguments
 * @param args the commandline arguments
 */
class CommandLineConfigParser(private val args: Array<String>) {
    private val baseOutPath = "${File("").absolutePath}${File.separatorChar}simout${File.separatorChar}"
    private val extractedArgs = extractParameter()
    val config: Configuration

    /**
     * reads the commandline arguments on construction
     */
    init {

        val evaluationRun = extractBooleanParameter("-eval")
        val evaluationSingle = if (evaluationRun) extractBooleanParameter("-evalsingle") else false
        val fileOutputRun = extractBooleanParameter("-output")


        val eventModelType =
            if (evaluationRun && !evalWithSpecificModel("")) EventModelTypes.EVAL else extractEventModelType()
        val timestampModelType =
            if (evaluationRun && !evalWithSpecificModel("")) TimestampModelTypes.EVAL else extractTimestampModelType()
        val logProviderType =
            if (evaluationRun && !evalWithSpecificModel("")) LogProviderTypes.MOCK_PROVIDER else extractLogProviderType()
        val weatherDataProviderType = extractWeatherDataProviderType()
        val logPath = if (evaluationRun && !evalWithSpecificModel("")) "" else extractPath("-log")
        val weatherDataPath = extractPath("-weatherdata")
        val outputPath =
            extractStringParameter("-outputpath").ifEmpty { "${baseOutPath}logOutput${File.separatorChar}generatedLog.log" }
        val evalPath =
            extractStringParameter("-evalpath").ifEmpty { "${baseOutPath}evaluation${File.separatorChar}" }
        val evalIterations = parseValue("-evaliterations")
        val evalReal = extractBooleanParameter("-real")
        val timeEval = extractBooleanParameter("-time")
        val numOfEvents = parseValue("-events")
        val duration = parseValue("-until")
        val untilTime = DateTime.now().plusHours(duration)
        config = Configuration(
            logProviderType,
            weatherDataProviderType,
            eventModelType,
            timestampModelType,
            logPath,
            weatherDataPath,
            evaluationRun,
            evaluationSingle,
            evalPath,
            fileOutputRun,
            outputPath,
            evalIterations,
            timeEval,
            evalReal,
            if (numOfEvents > 0) numOfEvents else 100,
            if (untilTime.isAfterNow && duration > 0) untilTime else null

        )

    }

    /**
     * extracts all commandline parameters as hashmap
     */
    private fun extractParameter(): HashMap<String, String> {
        if (args.isNullOrEmpty()) {
            throw NoConfigurationException()
        }

        val parameters = HashMap<String, String>()
        val lastIndex = args.indexOfLast { it.contains("-") } + 1


        (0 until lastIndex).forEach {
            val param = args[it]
            val value = if (it + 1 < args.size && !args[it + 1].contains("-")) args[it + 1] else "true"
            if (param.contains("-")) {
                parameters[param.toLowerCase()] = value
            }
        }

        return parameters
    }

    /**
     * determines the type of eventmodel to use
     * @return the eventmodel type
     */
    private fun extractEventModelType(): EventModelTypes {
        if (!extractedArgs.containsKey("-eventmodel")) {
            throw MissingParameterException("Event Model Type (-eventModel/-eventmodel)")
        } else {
            return when (extractedArgs["-eventmodel"]!!.toLowerCase()) {
                "mock" -> EventModelTypes.MOCK_EVENT_MODEL
                "mkm" -> EventModelTypes.MKM
                "cmkm" -> EventModelTypes.CMKM
                "mlp" -> EventModelTypes.MLPM
                "lstm" -> EventModelTypes.LSTMM
                else -> {
                    throw WrongParameterChoiceException(
                        "-eventmodel/-eventModel",
                        ""
                    )
                }

            }
        }
    }

    /**
     * determines the type of timestamp model to use
     * @return the timestamp model type
     */
    private fun extractTimestampModelType(): TimestampModelTypes {
        if (!extractedArgs.containsKey("-timestampmodel")) {
            throw MissingParameterException("Event Model Type (-timestampmodel/-timestampModel)")
        } else {
            return when (extractedArgs["-timestampmodel"]!!.toLowerCase()) {
                "mock" -> TimestampModelTypes.MOCK_TIMESTAMP_MODEL
                "utsm" -> TimestampModelTypes.UTSM
                "ptsm" -> TimestampModelTypes.PTSM
                "cptsm" -> TimestampModelTypes.CPTSM
                "tftsm" -> TimestampModelTypes.TFTSM
                "lstmtsm" -> TimestampModelTypes.LSTMTSM
                else -> {
                    throw WrongParameterChoiceException(
                        "-timestampModel/-timestampmodel",
                        ""
                    )
                }

            }
        }
    }

    /**
     * determines the type of log provider to use
     * mainly for later extensions
     * @return the log provider type
     */
    private fun extractLogProviderType(): LogProviderTypes {
        if (!extractedArgs.containsKey("-logprovider")) {
            throw MissingParameterException("Log Provider Type (-logprovider/-logProvider)")
        } else {
            return when (extractedArgs["-logprovider"]!!.toLowerCase()) {
                "mock" -> LogProviderTypes.MOCK_PROVIDER
                "openhab" -> LogProviderTypes.OPENHAB_PROVIDER
                else -> {
                    throw WrongParameterChoiceException(
                        "-logProvider/-logprovider",
                        ""
                    )
                }

            }
        }
    }

    /**
     * determines the type of weather data provider to use
     * mainly for later extensions
     * @return the weather data provider type
     */
    private fun extractWeatherDataProviderType(): WeatherDataProviderTypes {
        if (!extractedArgs.containsKey("-weatherdataprovider")) {
            throw MissingParameterException("Weather Data Provider Type (-weatherdataprovider/-weatherDataProvider)")
        } else {
            return when (extractedArgs["-weatherdataprovider"]!!.toLowerCase()) {
                "mock" -> WeatherDataProviderTypes.MOCK_PROVIDER
                "openweather" -> WeatherDataProviderTypes.OPEN_WEATHER_MAP_PROVIDER
                else -> {
                    throw WrongParameterChoiceException(
                        "-weatherdataprovider/-weatherDataProvider",
                        ""
                    )
                }

            }
        }
    }

    /**
     * extracts a boolean parameter for a given key
     * @param key the key for the parameter
     * @return  whether the paramater is set or not
     */
    private fun extractBooleanParameter(key: String): Boolean {
        return extractedArgs.containsKey(key)
    }

    /**
     * extracts a string parameter for a given key
     * @param key the key for the parameter
     * @return the value of the string parameter
     */
    private fun extractStringParameter(key: String): String {
        return if (extractedArgs.containsKey(key)) extractedArgs[key].toString() else ""
    }

    /**
     * extracts a path parameter for a given key
     * checks the availability of the path (existence and readability)
     * @param key the key for the parameter
     * @return the value of the path parameter
     */
    private fun extractPath(key: String): String {
        if (extractedArgs.containsKey(key) && isAllReadableAndExists(extractedArgs[key].toString())
        ) {
            extractedArgs[key].toString()
        } else {
            throw MissingParameterException("$key [might be not readable] ${extractedArgs[key].toString()}")
        }
        return extractedArgs[key].toString()
    }


    /**
     * checks the existence and readability of a given path as sting
     * @param path the path to check
     * @return whether the file at the path os readable and existing
     */
    private fun isAllReadableAndExists(path: String): Boolean {

        return File(path).exists() &&
                (((File(path).isDirectory) && File(path).listFiles()?.all { it.canRead() } ?: false)
                        || File(path).canRead())

    }

    /**
     * returns whether it is a an evaluation run for a single model
     */
    private fun evalWithSpecificModel(additionalKey: String): Boolean {

        return extractedArgs.containsKey("-eventmodel") && extractedArgs.containsKey("-timestampmodel") && if (additionalKey.isNotEmpty()) extractedArgs.containsKey(
            additionalKey
        ) else true

    }

    /**
     * extracts an integer parameter
     * @param key the key for the integer parameter
     * @return the value of the integer parameter
     */
    private fun parseValue(key: String): Int {
        return extractedArgs[key]?.trim()?.toInt() ?: 0
    }

}
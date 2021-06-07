package org.maspitzner.presencesimulation.models

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.maspitzner.presencesimulation.utils.datahandling.MinMaxNormalizer
import org.maspitzner.presencesimulation.utils.datahandling.Standardizer
import org.maspitzner.presencesimulation.utils.filehandling.createFileIfNotExists
import org.maspitzner.presencesimulation.utils.filehandling.getIdleTimes
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * This class encapsulates the whole given log information for a specific home.
 * Which also keeps track of statistics like event per device distribution.
 * @param logEntries
 *         (optional) Pre-made list of LogEntries consisting at least of a device and a timestamp.
 */

class Log(private val logEntries: ArrayList<LogEntry> = ArrayList(), originalLabels: Int = -1) {
    var size = logEntries.size
    private val inMapping = HashMap<String, Int>()
    private val outMapping = HashMap<Int, String>()
    var deviceCount = if (originalLabels < 0) {
        if (logEntries.size <= 0) 0 else (logEntries.distinctBy { it.selectedDevice }
            .maxByOrNull { it.selectedDevice }?.selectedDevice ?: 0) + 1
    } else {
        originalLabels
    }
    private var uniqueDevices = HashSet<String>()
    private var minMaxValues = HashMap<Feature, Pair<Double, Double>>()


    /**
     * Inserts a new log entry into the log given at least the timestamp and the device name.
     *  @param timestamp The timestamp at which the respective event occurred.
     *  @param deviceName The name of the device which had an action in this log entry.
     */
    fun insertEntry(timestamp: DateTime, deviceName: String) {
        uniqueDevices.add(deviceName)
        if (!inMapping.contains(deviceName)) {
            inMapping[deviceName] = inMapping[deviceName] ?: deviceCount
            outMapping[deviceCount] = deviceName
            deviceCount++
        }

        logEntries.add(LogEntry(timestamp, inMapping[deviceName] ?: -1))
        size++
    }

    /**
     * Gets the i.th entry from the log.
     * @param i The index which indicates the entry to retrieve.
     * @return The entry in the log at index i.
     */
    operator fun get(i: Int) = logEntries[i]


    /**
     * one hot encodes the weather labels provided by a open weather map JSON object
     */
    fun setWeatherLabels() {
        val encoding = logEntries
            .distinctBy { it.weather }
            .mapIndexed { i, it -> it.weather to i }
            .toMap()
        val excludedIndex: Int = encoding.size - 1
        logEntries.forEach {
            val labelEncoding = Array(encoding.size - 1) { 0.0 }
            val weatherIndex = encoding[it.weather] ?: 0
            if (weatherIndex != excludedIndex) {
                labelEncoding[weatherIndex] = 1.0
            }
            it.weatherEncoding = labelEncoding
        }
    }


    /**
     * Sets the weather information based on the extracted weather data.
     * @param unfilteredWeatherData The list extracted from the OpenWeatherMap Data.
     */
    fun setWeatherInformation(unfilteredWeatherData: List<WeatherDataEntry>) {

        val weatherData = filterWeatherByTimeFrame(unfilteredWeatherData)
        var currentLogEntry = 0
        var currentWeatherEntry = 0
        /*
        sets weather information of every logEntry with respect to its timestamp
         */
        while (currentLogEntry < logEntries.size) {

            if (currentWeatherEntry + 1 < weatherData.size && logEntries[currentLogEntry].timeStamp.isAfter(
                    weatherData[currentWeatherEntry + 1].first
                )
            ) {
                while (currentWeatherEntry + 1 < weatherData.size && !((logEntries[currentLogEntry].timeStamp.isAfter(
                        weatherData[currentWeatherEntry].first
                    )
                            || logEntries[currentLogEntry].timeStamp == weatherData[currentWeatherEntry].first)
                            && (logEntries[currentLogEntry].timeStamp.isBefore(
                        weatherData[currentWeatherEntry + 1].first
                    )))
                ) {
                    currentWeatherEntry++
                }
            }
            logEntries[currentLogEntry].setWeather(weatherData[currentWeatherEntry].second)
            currentLogEntry++
        }
    }

    /**
     * Returns a string representation of the receiver log.
     * @return String representing the given Log.
     */
    override fun toString(): String {
        val stringRepresentation = StringBuilder()
        logEntries.forEach { stringRepresentation.append("$it\n") }
            .also {
                stringRepresentation.append("deviceCount=$deviceCount\n")
                uniqueDevices.forEach { stringRepresentation.append("$it\n") }
            }
        return stringRepresentation.toString()
    }

    /**
     * Sorts the log entries by their timestamps.
     */
    fun sortByTimestamp() {
        logEntries.sortBy { it.timeStamp }
    }

    /**
     * calculates the minimum and maximum for idle times in a given log
     */
    fun getTimestampMinMax(): Pair<Long, Long> {
        val idleTimes = ArrayList<Long>()
        logEntries.forEachIndexed { i, _ ->
            if (i + 1 < logEntries.size) {

                idleTimes.add(getIdleTimes(logEntries[i], logEntries[i + 1]))
            }
        }
        return Pair(idleTimes.minOrNull() ?: Long.MIN_VALUE, idleTimes.maxOrNull() ?: Long.MAX_VALUE)
    }


    /**
     * scales every scalable feature in the log to the interval [0,1]
     */
    fun scaleLog() {
        val normalizer = MinMaxNormalizer()
        getMinMax()
        logEntries.map { logEntry ->
            val currentFeature = getScalableFeatures(logEntry)
            currentFeature.forEach { features ->
                if (features.key != Feature.CLOUDINESS && features.key != Feature.HUMIDITY) {
                    val min = minMaxValues[features.key]?.first ?: 0.0
                    val max = minMaxValues[features.key]?.second ?: 0.0
                    val scaledValue = normalizer.scale(min, max, features.value)
                    currentFeature[features.key] = scaledValue
                }
            }
            logEntry.setScaledFeatures(currentFeature)
        }
    }

    /**
     * standardizes the whole log to a random variable with expectation value 0 and variance 1
     */
    fun standardizeLog() {
        val standardizer = Standardizer()
        val means = getMean()
        val standardDeviations = getStandardDeviation(means)
        logEntries.map { logEntry ->
            val currentFeature = getScalableFeatures(logEntry)
            currentFeature.forEach { features ->
                if (features.key != Feature.CLOUDINESS && features.key != Feature.HUMIDITY) {
                    val mean = means[features.key] ?: 0.0
                    val standardDeviation = standardDeviations[features.key] ?: 0.0
                    val scaledValue = standardizer.scale(mean, standardDeviation, currentFeature[features.key] ?: 0.0)
                    currentFeature[features.key] = scaledValue
                }
            }


        }
    }

    /**
     * applies a given code block to each log entry
     * @param block the code block
     */
    fun forEach(block: (LogEntry) -> Unit) {
        logEntries.forEach {
            block(it)
        }
    }

    /**
     * returns the LogEntries as an ArrayList<LogEntry>
     * @return all log entries as ArrayList<LogEntry>
     */
    fun getEntries(): ArrayList<LogEntry> = logEntries


    /**
     * writes a log as csv file
     * @param path the path where the csv file should be written
     * @param separator separator char for the csv format default=','
     */
    fun writeCSV(path: String, separator: Char = ',') {
        val csvFile = File(path)
        createFileIfNotExists(path)
        logEntries.forEach {
            val entryCSVString = it.getCSVString(separator)
            csvFile.appendText("$entryCSVString\n")
        }
    }


    /**
     * returns the name of a device instead of its integer representation
     * @param device the device as integer
     * @return the name of the device as stated in the original log
     */
    fun getOutMapping(device: Int) = outMapping[device] ?: "Unknown Device"

    /**
     * Calculates statistics about the given log on demand.
     * @return Statistics object encapsulating some relevant information for the evaluation of the Thesis.
     */
    @Suppress("unused")
    fun calculateStatistics(): Statistics {
        val deviceDistribution = HashMap<Int, Int>()
        logEntries.forEach {
            deviceDistribution[it.selectedDevice] = deviceDistribution[it.selectedDevice]?.plus(1) ?: 1
        }
        return Statistics(
            deviceDistribution.toList().sortedByDescending { it.second }.toMap()
        )

    }

    /**
     * returns a train split of the current log as 2d dataset
     * @param length window size of the train data
     * @param splitRatio the proportion of the log which is used as train data (default=0.8)
     * @return the train data as 2d Dataset
     */
    fun get2DTrainData(length: Int, splitRatio: Double = 0.8): DataSet {
        val trainLog = Log(ArrayList(logEntries.subList(0, (logEntries.size * splitRatio).toInt())), deviceCount)
        return trainLog.get2DNDArray(length)
    }


    /**
     * returns a test split of the current log as 2d dataset
     * @param length window size of the test data (must be equal to train data)
     * @param splitRatio the proportion of the log which is used as test data (default=1-0.8=0.2)
     * @return the test data as 2d Dataset
     */
    fun get2DTestData(length: Int, splitRatio: Double = 0.8): DataSet {

        val testLog =
            Log(ArrayList(logEntries.subList((logEntries.size * splitRatio).toInt(), logEntries.size)), deviceCount)
        return testLog.get2DNDArray(length)
    }

    /**
     * returns the complete Log as a 3d Dataset. The label depends on whether it is for label or timestamp synthesis
     * @param timestampModel boolean indicating whether the model to train is a timestamp model or not
     * @return a 3d dataset representation of the log
     */
    fun get3DTrainCompleteLog(timestampModel: Boolean = false): DataSet {
        val minMaxNormalizer = MinMaxNormalizer()
        val (min, max) = getTimestampMinMax()
        val featureShape = intArrayOf(1, logEntries[0].getFeatures().size, logEntries.size)
        val labelShape = intArrayOf(1, deviceCount, logEntries.size)
        val timestampShape = intArrayOf(1, 1, logEntries.size)
        val featuresNDArray = Nd4j.zeros(*featureShape)
        val labelsNDArray = Nd4j.zeros(*labelShape)
        val idleTimesNDArray = Nd4j.zeros(*timestampShape)
        logEntries.forEachIndexed { i, logEntry ->


            if (i + 1 < logEntries.size) {
                logEntry.getFeatures().toDoubleArray().forEachIndexed { k, featureFalue ->
                    featuresNDArray.putScalar(arrayOf(1, k, i).toIntArray(), featureFalue)
                }
                labelsNDArray.putScalar(arrayOf(1, logEntries[i + 1].selectedDevice, i).toIntArray(), 1)
                idleTimesNDArray.putScalar(
                    arrayOf(1, 1, i).toIntArray(),
                    minMaxNormalizer.scale(min, max, getIdleTimes(logEntries[i], logEntries[i + 1]))
                )

            }
        }
        val targetData = if (timestampModel) idleTimesNDArray else labelsNDArray
        return DataSet(featuresNDArray, targetData)
    }


    /**
     * returns a train split of the current log as 3d dataset
     * @param length window size of the train data
     * @param splitRatio the proportion of the log which is used as train data (default=0.8)
     * @param timestampModel boolean indicating whether the model to train is a timestamp model or not
     * @return the test data as 3d Dataset
     */
    fun get3DTrainData(length: Int, splitRatio: Double = 0.8, timestampModel: Boolean = false): DataSet {
        val trainLog = Log(ArrayList(logEntries.subList(0, (logEntries.size * splitRatio).toInt())), deviceCount)
        return trainLog.get3DNDArray(length, timestampModel)
    }

    /**
     * returns a test split of the current log as 3d dataset
     * @param length window size of the test data (must be equal to train data)
     * @param splitRatio the proportion of the log which is used as test data (default=1-0.8=0.2)
     * @param timestampModel boolean indicating whether the model to train is a timestamp model or not
     * @return the test data as 3d Dataset
     */
    fun get3DTestData(length: Int, splitRatio: Double = 0.8, timestampModel: Boolean = false): DataSet {
        val testLog =
            Log(ArrayList(logEntries.subList((logEntries.size * splitRatio).toInt(), logEntries.size)), deviceCount)
        return testLog.get3DNDArray(length, timestampModel)
    }


    /**
     * Filters the weather data the way,
     * that only the needed time frame will be retrieved.
     * @param weatherData unfiltered weather data as list.
     * @return List<Pair<DateTime, WeatherDataEntry>> where the weather data
     * is only the data within the time frame represented by the log.
     */
    private fun filterWeatherByTimeFrame(weatherData: List<WeatherDataEntry>): List<Pair<DateTime, WeatherDataEntry>> {
        val firstEvent = logEntries[0].timeStamp
        val lastEvent = logEntries[logEntries.size - 1].timeStamp
        return weatherData
            .map { dataEntry ->
                DateTime(
                    dataEntry.getJodaTimeFormat(), DateTimeZone.UTC
                ) to dataEntry
            }.filter {
                it.first.isAfter(firstEvent.hourOfDay().roundFloorCopy().minusHours(1)) &&
                        it.first.isBefore(lastEvent.hourOfDay().roundCeilingCopy().plusHours(1))
            }
    }


    /**
     * returns the scaleable features by their type for a given LogEntry
     *  @param entry the given LogEntry
     *  @return am mapping of feature type to feature value
     *
     */
    private fun getScalableFeatures(entry: LogEntry): HashMap<Feature, Double> {

        val currentValues = HashMap<Feature, Double>()

        currentValues[Feature.TIME_OF_DAY] = entry.timeOfDay
        currentValues[Feature.DAY_OF_YEAR] = entry.dayOfYear
        currentValues[Feature.REAL_TEMPERATURE] = entry.realTemperature
        currentValues[Feature.FELT_TEMPERATURE] = entry.feltTemperature
        currentValues[Feature.CLOUDINESS] = entry.cloudiness
        currentValues[Feature.HUMIDITY] = entry.humidity
        currentValues[Feature.PRESSURE] = entry.pressure.toDouble()
        currentValues[Feature.WIND_SPEED] = entry.windSpeed
        return currentValues
    }

    /**
     * calculates each mean of all scalable features for a given log
     * @return a map of feature type to its mean
     */
    private fun getMean(): HashMap<Feature, Double> {
        val means = HashMap<Feature, Double>()
        logEntries.map { getScalableFeatures(it) }.forEach { features ->
            features.forEach { feature ->
                means[feature.key] = (means[feature.key] ?: 0.0) + (feature.value / logEntries.size)
            }
        }

        return means
    }

    /**
     * calculates each standard deviation of all scalable features for a given log
     * @return a map of feature type to its stdDeviation
     */

    private fun getStandardDeviation(means: HashMap<Feature, Double>): HashMap<Feature, Double> {
        val standardDeviations = HashMap<Feature, Double>()
        logEntries.forEach { logEntry ->
            val currentFeatures = getScalableFeatures(logEntry)
            Feature.values().forEach { feature ->
                val featureMean = means[feature] ?: 0.0
                val featureValue = currentFeatures[feature] ?: 0.0
                var currentSum = standardDeviations[feature] ?: 0.0
                currentSum += (featureValue - featureMean).pow(2.0)
                standardDeviations[feature] = currentSum
            }

        }
        standardDeviations.forEach { standardDeviationSum ->
            standardDeviations[standardDeviationSum.key] = sqrt(standardDeviationSum.value / logEntries.size)
        }


        return standardDeviations
    }

    /**
     * calculates whether the current value is part of the min max pair with respect to a given feature type
     * @param currentValue the value of a feature
     * @param feature the feature type
     * @return the new min max pair
     */
    private fun getNewMinMaxPair(currentValue: Double, feature: Feature): Pair<Double, Double> {

        if (minMaxValues[feature] == null || minMaxValues[feature]?.first == null || minMaxValues[feature]?.second == null) {
            minMaxValues[feature] = Pair(Double.MAX_VALUE, Double.MIN_VALUE)
        }
        return when {
            currentValue < minMaxValues[feature]?.first!! -> {
                Pair(currentValue, minMaxValues[feature]?.second!!)
            }
            currentValue > minMaxValues[feature]?.second!! -> {
                Pair(minMaxValues[feature]?.first!!, currentValue)
            }
            else -> {
                minMaxValues[feature] ?: Pair(Double.MAX_VALUE, Double.MIN_VALUE)
            }
        }

    }

    /**
     * calculates the minimum and maximum for each feature for the given log
     */
    private fun getMinMax() {
        logEntries.forEach { logEntry ->
            val currentFeatures = getScalableFeatures(logEntry)
            Feature.values().forEach { feature ->
                val newMinMaxPair = getNewMinMaxPair(currentFeatures[feature] ?: 0.0, feature)
                minMaxValues[feature] = newMinMaxPair
            }
        }
    }


    /**
     * returns the log as a 2d Dataset simple transformation to simplify the usage of (DL4J)
     * @param length the window length of entries for a given training example
     * @return the log as a Dataset of the given window size to train DL4J-models with
     */
    private fun get2DNDArray(length: Int): DataSet {
        val features = Array<Array<Double>>(logEntries.size - length) { emptyArray() }
        val labels = Array(logEntries.size - length) { Array(deviceCount) { 0.0 } }
        val tempFeature = ArrayList<Double>()
        logEntries.forEachIndexed { i, _ ->
            if (i + length < logEntries.size) {
                (i until i + length).forEach { j ->

                    tempFeature.addAll(logEntries[j].getFeatures().toTypedArray())

                }

                features[i] = tempFeature.toTypedArray()
                labels[i][logEntries[i + length].selectedDevice] = 1.0
                tempFeature.clear()
            }
        }

        return DataSet(Nd4j.createFromArray(features), Nd4j.createFromArray(labels))
    }


    /**
     * returns the log as a 3d Dataset simple transformation to simplify the usage of (DL4J)
     * @param length the window length of entries for a given training example
     * @param timestampModel boolean indicating whether the model to train is a timestamp model or not
     * @return the log as a 3d Dataset of the given window size to train DL4J-models with
     */
    private fun get3DNDArray(length: Int, timestampModel: Boolean = false): DataSet {
        val minMaxNormalizer = MinMaxNormalizer()
        val (min, max) = getTimestampMinMax()
        val featureShape = intArrayOf(logEntries.size - length, logEntries[0].getFeatures().size, length)
        val labelShape = intArrayOf(logEntries.size - length, deviceCount, length)
        val timestampShape = intArrayOf(logEntries.size - length, 1, length)
        val featuresNDArray = Nd4j.zeros(*featureShape)
        val labelsNDArray = Nd4j.zeros(*labelShape)
        val idleTimesNDArray = Nd4j.zeros(*timestampShape)
        logEntries.forEachIndexed { i, _ ->
            if (i + length < logEntries.size) {
                (0 until length).forEach { j ->

                    logEntries[j + i].getFeatures().toDoubleArray().forEachIndexed { k, it ->
                        featuresNDArray.putScalar(arrayOf(i, k, j).toIntArray(), it)
                    }
                    labelsNDArray.putScalar(arrayOf(i, logEntries[i + j + 1].selectedDevice, j).toIntArray(), 1)
                    idleTimesNDArray.putScalar(
                        arrayOf(i, 1, j).toIntArray(),
                        minMaxNormalizer.scale(
                            min,
                            max,
                            getIdleTimes(logEntries[i + j], logEntries[i + j + 1]).toDouble()
                        )
                    )
                }
            }
        }

        val targetData = if (timestampModel) idleTimesNDArray else labelsNDArray
        return DataSet(featuresNDArray, targetData)
    }

}

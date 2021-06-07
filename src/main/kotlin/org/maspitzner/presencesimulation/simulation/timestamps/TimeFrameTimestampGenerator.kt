package org.maspitzner.presencesimulation.simulation.timestamps

import org.apache.commons.math3.distribution.PoissonDistribution
import org.joda.time.DateTime
import org.joda.time.IllegalInstantException
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import kotlin.random.Random

/**
 * Class abstraction to implement a Timeframe-based TimestampGenerator.
 * This implements [FitableTimestampGenerator] and [StatefulTimestampGenerator]
 * @see FitableTimestampGenerator
 * @see StatefulTimestampGenerator
 */
class TimeFrameTimestampGenerator(override var baseTimestamp: DateTime) : FitableTimestampGenerator,
    StatefulTimestampGenerator {
    /*
     * Necessary variables
     * deviceStates - indicating whether a device is currently on or off,
     *                assuming a simulation starts with all devices off
     * timeFrameLength - the partition length of the hours of a day in hours
     * durations - a Array of Lists of idleTimes
     * generators - a Set of poisson generators
     * threshold - a number which a generated idle time can't exceed
     * mostCommonDevice - the device most often activated for fall back
     * coreActivityTimes - the timeframes in which the most activity is seen, split by the splitRatio
     */
    private lateinit var deviceStates: Array<Boolean>
    private var timeFrameLength = 2
    private val idleTimesPerTimeframe = HashMap<Int, Array<ArrayList<Long>>>()
    private val generators = HashMap<Int, Array<PoissonDistribution?>>()
    private var threshold = 1.5 * 60 * 60 * 1000.0
    private var mostCommonDevice = -1
    private lateinit var coreActivityTimes: HashSet<Int>
    private var splitRatio = 0.50

    /**
     * Optional function for later usage
     * This sets the hyperparameters of this model
     * One might later add
     * @param splitRatio the split ratio to set
     * @param timeFrameLength the partition length of the hours of a day. Shouldn't exceed 24
     */
    fun setHyperParameters(splitRatio: Double = 0.5, timeFrameLength: Int = 2) {
        this.splitRatio = splitRatio
        if (timeFrameLength <= 24)
            this.timeFrameLength = timeFrameLength
    }

    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param logEntries the data to train the model on
     * @param uniqueDevices the number of unique devices in the data
     */
    override fun fit(logEntries: ArrayList<LogEntry>, uniqueDevices: Int) {


        //first sets all devices to off
        deviceStates = Array(uniqueDevices) { false }
        // calculates the most commonly used device
        mostCommonDevice = logEntries.groupBy { it.selectedDevice }.maxByOrNull { it.value.size }?.key ?: 0
        //calculates the device distribution for the partitions of the hours of a day
        val hourList = logEntries.groupBy { it.timeStamp.hourOfDay / timeFrameLength }
        //initializes the counts per devices for later mean calculations and for the core activity time calculation
        val occurrences = Array(24 / timeFrameLength) { 0.0 }


        //counts the device occurrences and initializes the idle times lookup
        countOccurrences(hourList, occurrences, logEntries, uniqueDevices)
        // calculates the core activity times by device activation per timeframe and splitting w.r.t. the splitratio
        extractCoreActivityTimes(occurrences)


        //calculates the idleTimes per  timeframe
        calculateIdleTimesPerTimeframe(hourList)
        //calculates the respective mean for each timeFrame and generates a PoissonGenerator
        createGenerators(uniqueDevices)
    }

    /**
     * Counts the device occurrences and initializes the idle times lookup
     * @param hourList logentries per timeFrame
     * @param occurrences number of occurrences in the respective timeframe
     * @param logEntries the given training data
     * @param uniqueDevices the number of unique devices in the training data
     */
    private fun countOccurrences(
        hourList: Map<Int, List<LogEntry>>,
        occurrences: Array<Double>,
        logEntries: ArrayList<LogEntry>,
        uniqueDevices: Int
    ) {
        hourList.toSortedMap().forEach {
            occurrences[it.key] = it.value.size.toDouble() / logEntries.size
            idleTimesPerTimeframe[it.key] = Array(uniqueDevices) { ArrayList() }
        }
    }

    /**
     * calculates for each timeframe and device one dedicated poisson generator
     * @param uniqueDevices the number of devices in the training data
     */
    private fun createGenerators(uniqueDevices: Int) {
        idleTimesPerTimeframe.toSortedMap()
            .forEach { timeFrameEntries ->

                val currentMeans = Array(uniqueDevices) { 0.0 }
                timeFrameEntries.value.forEachIndexed { device, durations ->
                    val durationSum = durations.sum()
                    val mean = durationSum.toDouble() / durations.size
                    currentMeans[device] = mean
                }
                generators[timeFrameEntries.key] = Array(uniqueDevices) { device ->
                    if (currentMeans[device].isNaN()) {
                        null
                    } else {
                        PoissonDistribution(currentMeans[device])
                    }
                }
            }
    }

    /**
     * Calculates the IdleTimes per Timeframe
     * @param hourList logentries per timeframe
     */
    private fun calculateIdleTimesPerTimeframe(hourList: Map<Int, List<LogEntry>>) {
        hourList.forEach { (i, list) ->
            list.windowed(2).forEach {
                if (it[1].timeStamp.dayOfYear == it[0].timeStamp.dayOfYear) {
                    val idleTime = it[1].timeStamp.millis - it[0].timeStamp.millis
                    idleTimesPerTimeframe[i]?.get(it[0].selectedDevice)?.add(idleTime)
                }
            }
        }
    }

    /**
     * Calculates the core activity timeframes
     * @param occurrences the ratio of activities per timeframe
     */
    private fun extractCoreActivityTimes(occurrences: Array<Double>) {
        coreActivityTimes = occurrences
            .mapIndexed { i, it ->
                i to it
            }
            .sortedByDescending { it.second }
            .subList(0, (occurrences.size * splitRatio).toInt())
            .map { it.first }
            .toHashSet()
    }

    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param log the data to train the model on
     */
    override fun fit(log: Log) {
        fit(log.getEntries(), log.deviceCount)
    }

    /**
     * generates a idle time for each device in the given label list
     * @param labelList a list of labels in need of idle times
     * @return a list of idle times
     */
    override fun generateLengthSequence(labelList: ArrayList<Int>): ArrayList<Long> {
        return labelList.map { generateTimestampLength(it) } as ArrayList<Long>
    }

    /**
     * generates a idle time for each LogEntry in the list of LogEntries
     * @param logEntries a list of LogEntries in need of idle times
     * @return a list of idle times
     */
    override fun generateLengthSequence(logEntries: List<LogEntry>): ArrayList<Long> {
        return logEntries.map { generateTimestampLength(it.selectedDevice) } as ArrayList<Long>
    }

    /**
     * generates a idle time for each LogEntry in the Log
     * @param log a given Log in need of idle times
     * @return a list of idle times
     */
    override fun generateLengthSequence(log: Log): ArrayList<Long> {
        return generateLengthSequence(log.getEntries())
    }

    /**
     * generates a timestamp for each device in the given label list
     * @param labelList a list of labels in need of timestamps
     * @return a list of timestamps
     */
    override fun generateTimestampSequence(labelList: ArrayList<Int>): ArrayList<DateTime> {
        return labelList.map { generateTimestamp(it) } as ArrayList<DateTime>
    }

    /**
     * generates a timestamp for each LogEntry in the list of LogEntries
     * @param labelList a list of LogEntries in need of timestamps
     * @return a list of idle times
     */
    override fun generateTimestampSequence(labelList: List<LogEntry>): ArrayList<DateTime> {
        return labelList.map { generateTimestamp(it.selectedDevice) } as ArrayList<DateTime>
    }

    /**
     * generates a timestamp for each LogEntry in the Log
     * @param log a given Log in need of timestamps
     * @return a list of timestamps
     */
    override fun generateTimestampSequence(log: Log): ArrayList<DateTime> {
        return generateTimestampSequence(log.getEntries())
    }

    /**
     * generates a idle time based on a LogEntry
     * @param logEntry the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntry: LogEntry): Long {
        return generateTimestampLength(logEntry.selectedDevice)
    }

    /**
     * generates a idle time based on a device label
     * @param deviceLabel the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(deviceLabel: Int): Long {
        //calculating the current time frame
        val timeframe = baseTimestamp.hourOfDay / timeFrameLength
        //checks whether the current timeframe is a core activity one
        val isInMainActivityFrame = this.coreActivityTimes.contains(timeframe)
        // initializes the current device state
        val deviceIsOn = deviceStates[deviceLabel]

        //calculates the idle time and returns it
        return if (isInMainActivityFrame || deviceIsOn) {
            // if the device is on or the current timeframe is a core activity time frame
            // change the state and generate the respective idle time by generating it via the respective
            // poisson generator
            deviceStates[deviceLabel] = !deviceStates[deviceLabel]
            if (generators[timeframe]?.get(deviceLabel) != null) {
                var waitingTime = generators[timeframe]?.get(deviceLabel)?.sample()?.toLong() ?: -1
                if (waitingTime > threshold || waitingTime == -1L) {
                    waitingTime = threshold.toLong()
                }
                waitingTime
            } else {
                threshold.toLong()
            }

        } else {
            // if this is not the case,
            // change the state
            deviceStates[deviceLabel] = !deviceStates[deviceLabel]
            //shift the idle time to the next timeframe
            generateIdleTimeNextTimeframe(timeframe)
        }
    }

    /**
     * Generates the next idle time w.r.t. core activity timeframes
     * @param timeframe the current timeframe
     * @return the calculated idle time
     */
    private fun generateIdleTimeNextTimeframe(timeframe: Int): Long {
        // determination of the next core activity timeframe
        val nextActiveTimeFrame =
            coreActivityTimes.toTypedArray()
                .map { it to (it - timeframe).toDouble() }
                .filter { it.second > 0 }
                .minByOrNull { it.second }?.first
                ?: coreActivityTimes.random()
        val currentTime = baseTimestamp

        // generate the target timestamp
        var targetTime: DateTime = generateTargetTime(nextActiveTimeFrame)

        // if the target timestamp is before the current timestamp (due to summer and winter time calculation)
        // increase the timestamp by a day
        if (targetTime.isBefore(currentTime)) {
            targetTime = targetTime.plusDays(1)
        }

        // return the idle time
        return targetTime.millis - currentTime.millis
    }


    /**
     * Generates the wanted target time in case the current timeframe isn't a core activity timeframe
     * @param nextActiveTimeFrame the next core activity timeframe
     * @return the target time as timeframe
     */
    private fun generateTargetTime(nextActiveTimeFrame: Int): DateTime {
        var targetTime: DateTime
        // calculating the starting hour of the next timeframe
        val hour = nextActiveTimeFrame * timeFrameLength
        // handling of time gaps due to leap years and summer and winter time
        try {
            targetTime =
                DateTime(
                    baseTimestamp.year,
                    baseTimestamp.monthOfYear,
                    baseTimestamp.dayOfMonth,
                    hour,
                    Random.nextInt(0, 60),
                    Random.nextInt(0, 60)
                )
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException,
                is IllegalInstantException -> {
                    targetTime = DateTime(
                        baseTimestamp.year,
                        baseTimestamp.monthOfYear,
                        baseTimestamp.dayOfMonth,
                        hour + 1,
                        Random.nextInt(0, 60),
                        Random.nextInt(0, 60)
                    )
                }
                else -> throw e
            }
        }
        return targetTime
    }

    /**
     * generates a idle time based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntries: ArrayList<LogEntry>): Long {
        return try {
            generateTimestampLength(logEntries.last())
        } catch (e: NoSuchElementException) {
            generateTimestampLength(mostCommonDevice)
        }
    }

    /**
     * generates a idle time based on a Log
     * @param log the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(log: Log): Long {
        return try {
            generateTimestampLength(log.getEntries().last())
        } catch (e: NoSuchElementException) {
            generateTimestampLength(mostCommonDevice)
        }
    }

    /**
     * generates a timestamp based on a LogEntry
     * @param logEntry the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(logEntry: LogEntry): DateTime {
        return generateTimestamp(logEntry.selectedDevice)
    }

    /**
     * generates a timestamp based on a device label
     * @param deviceLabel the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(deviceLabel: Int): DateTime {
        return transformDurationToTimestamp(generateTimestampLength(deviceLabel))
    }

    /**
     * generates a timestamp based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(logEntries: ArrayList<LogEntry>): DateTime {
        return try {
            generateTimestamp(logEntries.last())
        } catch (e: NoSuchElementException) {
            generateTimestamp(mostCommonDevice)
        }
    }

    /**
     * generates a timestamp based on a log
     * @param log the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(log: Log): DateTime {
        return try {
            generateTimestamp(log.getEntries().last())
        } catch (e: NoSuchElementException) {
            generateTimestamp(mostCommonDevice)
        }
    }
}
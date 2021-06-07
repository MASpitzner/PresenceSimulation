package org.maspitzner.presencesimulation.simulation.timestamps

import org.apache.commons.math3.distribution.PoissonDistribution
import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * class implementing a Classbased poisson timestamp generator
 * implements [FitableTimestampGenerator] and [StatefulTimestampGenerator]
 * @see [FitableTimestampGenerator]
 * @see [StatefulTimestampGenerator]
 */
class ClassBasedPoissonTimestampGenerator(override var baseTimestamp: DateTime) : FitableTimestampGenerator,
    StatefulTimestampGenerator {
    private val waitingTimeClasses = ArrayList<ArrayList<Double>>()
    private val generators = ArrayList<ArrayList<PoissonDistribution>>()
    private var threshold = 1 * 60 * 60 * 1000.0

    /**
     * Determines the classes used for generate the Poisson idle time generators
     * @param idleTimes list of idle times
     * @return list of idle times means
     */
    private fun determineClasses(idleTimes: ArrayList<Double>): ArrayList<Double> {

        // Initialization of classes, borders and buckets to the idle times into the classes while building
        val numberOfClasses = sqrt(idleTimes.size.toDouble()).roundToInt()

        val intervalBorders = Array(numberOfClasses) { idleTimes.maxOrNull()!! / (it + 1) }.reversedArray()
        val buckets = Array<ArrayList<Double>>(numberOfClasses) { ArrayList() }
        var currentBucket = 0

        // Calculates the class population
        idleTimes.sorted().forEach {
            if (it > intervalBorders[currentBucket]) {
                currentBucket++
            }
            buckets[currentBucket].add(it)
        }

        // calculates the mean per representative
        val newList = ArrayList<Double>()
        buckets.forEach {
            if (it.isNotEmpty()) {
                var mean = it.sum()
                mean /= it.size
                if (mean <= threshold) {
                    newList.add(mean)
                }
            }
        }
        if (newList.isEmpty()) {
            newList.add(threshold)
        }
        return newList
    }

    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param logEntries the data to train the model on
     * @param uniqueDevices the number of unique devices in the data
     */
    override fun fit(logEntries: ArrayList<LogEntry>, uniqueDevices: Int) {
        val durations = Array(uniqueDevices) { ArrayList<Double>() }

        /*
        Generates idle times
         */
        logEntries.windowed(2) {
            val idleTimes = it[1].timeStamp.millis - it[0].timeStamp.millis
            durations[it[0].selectedDevice].add(idleTimes.toDouble())
        }
        /*
         * organizes them into classes
         */
        durations.forEach {
            this.waitingTimeClasses.add(determineClasses(it))
        }

        /*
         * Creates a poisson generator for every mean
         */
        this.waitingTimeClasses.forEachIndexed { i, deviceMeans ->
            this.generators.add(ArrayList())
            deviceMeans.forEach {
                this.generators[i].add(PoissonDistribution(it))
            }
        }

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
        return generateLengthSequence(logEntries.map { it.selectedDevice } as ArrayList<Int>)
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
        return labelList.map { transformDurationToTimestamp(generateTimestampLength(it)) } as ArrayList<DateTime>
    }

    /**
     * generates a timestamp for each LogEntry in the list of LogEntries
     * @param labelList a list of LogEntries in need of timestamps
     * @return a list of idle times
     */
    override fun generateTimestampSequence(labelList: List<LogEntry>): ArrayList<DateTime> {
        return generateTimestampSequence(labelList.map { it.selectedDevice } as ArrayList<Int>)
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
        return try {
            generators[deviceLabel].random().sample().toLong()
        } catch (e: NoSuchElementException) {
            Random.nextLong(0, threshold.roundToLong())
        }
    }

    /**
     * generates a idle time based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntries: ArrayList<LogEntry>): Long {
        return try {
            generateTimestampLength(logEntries.last().selectedDevice)
        } catch (e: NoSuchElementException) {
            generateTimestampLength(0)
        }

    }

    /**
     * generates a idle time based on a Log
     * @param log the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(log: Log): Long {
        return generateTimestampLength(log.getEntries())

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
     * generates a timestamp based on a LogEntry
     * @param logEntry the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(logEntry: LogEntry): DateTime {
        return generateTimestamp(logEntry.selectedDevice)
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
            generateTimestamp(0)
        }
    }

    /**
     * generates a timestamp based on a log
     * @param log the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(log: Log): DateTime {
        return generateTimestamp(log.getEntries())
    }
}
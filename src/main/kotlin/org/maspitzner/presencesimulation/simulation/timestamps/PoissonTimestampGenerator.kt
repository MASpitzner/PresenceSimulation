package org.maspitzner.presencesimulation.simulation.timestamps

import org.apache.commons.math3.distribution.PoissonDistribution
import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry


/**
 * Implements needed functionality for generating a timestamp based on a poisson distribution model.
 * implements [FitableTimestampGenerator] and [StatefulTimestampGenerator]
 */
class PoissonTimestampGenerator(override var baseTimestamp: DateTime) :
    FitableTimestampGenerator,
    StatefulTimestampGenerator {
    private val epsilon = 0.0000000000001
    private var numberGenerator = Array(0) { PoissonDistribution(1.0) }

    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param logEntries the data to train the model on
     * @param uniqueDevices the number of unique devices in the data
     */
    override fun fit(logEntries: ArrayList<LogEntry>, uniqueDevices: Int) {
        var means = Array(uniqueDevices) { 0.0 }
        val groupedEntries = logEntries.groupBy { it.selectedDevice }
        val occurrences = Array(uniqueDevices) { n -> groupedEntries[n]?.size ?: 1 }
        logEntries.windowed(2) {
            val duration = it[1].timeStamp.millis - it[0].timeStamp.millis
            means[it[0].selectedDevice] += duration.toDouble()
        }

        means = means.mapIndexed { i, it ->
            if (it > 0 && occurrences[i] > 0) {
                it / occurrences[i]
            } else {
                epsilon
            }
        }.toTypedArray()

        this.numberGenerator = Array(uniqueDevices) { n ->
            PoissonDistribution(means[n])
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
     * generates a idle time based on a LogEntry
     * @param logEntry the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntry: LogEntry): Long {
        return numberGenerator[logEntry.selectedDevice].sample().toLong()
    }

    /**
     * generates a idle time based on a device label
     * @param deviceLabel the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(deviceLabel: Int): Long {
        return numberGenerator[deviceLabel].sample().toLong()
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
     * generates a timestamp based on a LogEntry
     * @param logEntry the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(logEntry: LogEntry): DateTime {
        return transformDurationToTimestamp(generateTimestampLength(logEntry))
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
        return transformDurationToTimestamp(generateTimestampLength(logEntries))
    }

    /**
     * generates a timestamp based on a log
     * @param log the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(log: Log): DateTime {
        return transformDurationToTimestamp(generateTimestampLength(log))
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
        return logEntries.map { generateTimestampLength(it) } as ArrayList<Long>
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
        return generateLengthSequence(labelList).map { transformDurationToTimestamp(it) } as ArrayList<DateTime>
    }

    /**
     * generates a timestamp for each LogEntry in the list of LogEntries
     * @param labelList a list of LogEntries in need of timestamps
     * @return a list of idle times
     */
    override fun generateTimestampSequence(labelList: List<LogEntry>): ArrayList<DateTime> {
        return generateLengthSequence(labelList).map { transformDurationToTimestamp(it) } as ArrayList<DateTime>
    }

    /**
     * generates a timestamp for each LogEntry in the Log
     * @param log a given Log in need of timestamps
     * @return a list of timestamps
     */
    override fun generateTimestampSequence(log: Log): ArrayList<DateTime> {
        return generateLengthSequence(log).map { transformDurationToTimestamp(it) } as ArrayList<DateTime>
    }

}
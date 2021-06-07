package org.maspitzner.presencesimulation.simulation.timestamps

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import kotlin.random.Random


/**
 * Implements needed functionality for generating a timestamp based on a uniform distribution model.
 */
class UniformTimestampGenerator(override var baseTimestamp: DateTime) :

    StatefulTimestampGenerator {

    private var seed = 0L
    private var numberGenerator = Random(DateTime.now().millis)
    private var upperBorder = 7200000L

    /**
     * functionality to set parameters different form the static default ones
     */
    fun setRandomParameter(seed: Long = -1, random: Random? = null, upperBorder: Long = Long.MAX_VALUE) {
        if (seed > 0) {
            this.seed = seed
            this.numberGenerator = Random(seed)
        }
        if (random != null) {
            this.numberGenerator = random
        }
        if (upperBorder != this.upperBorder) {
            this.upperBorder = upperBorder
        }
    }


    /**
     * generates a idle time based on a LogEntry
     * @param logEntry the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntry: LogEntry): Long {
        return numberGenerator.nextLong(0, upperBorder)
    }

    /**
     * generates a idle time based on a device label
     * @param deviceLabel the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(deviceLabel: Int): Long {
        return numberGenerator.nextLong(0, upperBorder)
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
        return transformDurationToTimestamp(generateTimestampLength(log.getEntries()))
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
        val durationList = generateLengthSequence(labelList)
        return durationList
            .map { transformDurationToTimestamp(it) }
                as ArrayList<DateTime>
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
        return generateTimestampSequence(log.getEntries())
    }


}
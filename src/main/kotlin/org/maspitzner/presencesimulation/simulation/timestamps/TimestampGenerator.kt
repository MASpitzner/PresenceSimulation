package org.maspitzner.presencesimulation.simulation.timestamps

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry


/**
 * abstraction for all Timestamp Models
 */
interface TimestampGenerator {

    // base timestamp to simulate based on this
    var baseTimestamp: DateTime

    /**
     * generates a idle time based on a LogEntry
     * @param logEntry the base for the synthesis of the next idle time
     * @return the idle time
     */
    fun generateTimestampLength(logEntry: LogEntry): Long

    /**
     * generates a idle time based on a device label
     * @param deviceLabel the base for the synthesis of the next idle time
     * @return the idle time
     */
    fun generateTimestampLength(deviceLabel: Int): Long

    /**
     * generates a idle time based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next idle time
     * @return the idle time
     */
    fun generateTimestampLength(logEntries: ArrayList<LogEntry>): Long

    /**
     * generates a idle time based on a Log
     * @param log the base for the synthesis of the next idle time
     * @return the idle time
     */
    fun generateTimestampLength(log: Log): Long

    /**
     * generates a timestamp based on a LogEntry
     * @param logEntry the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    fun generateTimestamp(logEntry: LogEntry): DateTime

    /**
     * generates a timestamp based on a device label
     * @param deviceLabel the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    fun generateTimestamp(deviceLabel: Int): DateTime

    /**
     * generates a timestamp based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    fun generateTimestamp(logEntries: ArrayList<LogEntry>): DateTime

    /**
     * generates a timestamp based on a log
     * @param log the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    fun generateTimestamp(log: Log): DateTime


    /**
     * transforms a idle time based on the current base timestamp to a follow up timestamp
     * @param millis the idle time generated
     * @return the next timestamp
     */
    fun transformDurationToTimestamp(millis: Long): DateTime {
        baseTimestamp = baseTimestamp.plus(millis)
        return baseTimestamp

    }
}
package org.maspitzner.presencesimulation.simulation.timestamps

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry

/**
 * Interface abstraction for models that are able to generate sequences of timestamps
 * implements also [TimestampGenerator]
 * @see [TimestampGenerator]
 */
interface StatefulTimestampGenerator : TimestampGenerator {

    /**
     * generates a idle time for each device in the given label list
     * @param labelList a list of labels in need of idle times
     * @return a list of idle times
     */
    fun generateLengthSequence(labelList: ArrayList<Int>): ArrayList<Long>

    /**
     * generates a idle time for each LogEntry in the list of LogEntries
     * @param logEntries a list of LogEntries in need of idle times
     * @return a list of idle times
     */
    fun generateLengthSequence(logEntries: List<LogEntry>): ArrayList<Long>

    /**
     * generates a idle time for each LogEntry in the Log
     * @param log a given Log in need of idle times
     * @return a list of idle times
     */
    fun generateLengthSequence(log: Log): ArrayList<Long>

    /**
     * generates a timestamp for each device in the given label list
     * @param labelList a list of labels in need of timestamps
     * @return a list of timestamps
     */
    fun generateTimestampSequence(labelList: ArrayList<Int>): ArrayList<DateTime>

    /**
     * generates a timestamp for each LogEntry in the list of LogEntries
     * @param labelList a list of LogEntries in need of timestamps
     * @return a list of idle times
     */
    fun generateTimestampSequence(labelList: List<LogEntry>): ArrayList<DateTime>

    /**
     * generates a timestamp for each LogEntry in the Log
     * @param log a given Log in need of timestamps
     * @return a list of timestamps
     */
    fun generateTimestampSequence(log: Log): ArrayList<DateTime>

}
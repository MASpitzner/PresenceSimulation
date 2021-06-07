package org.maspitzner.presencesimulation.simulation.timestamps

import org.joda.time.DateTime
import org.joda.time.Duration
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry

/**
 * Mock Timestamp Model for debugging purposes

 */
class MockTimestampModel(override var baseTimestamp: DateTime) : TimestampGenerator {
    init {
        println("MockTimestampModel instantiated")
    }

    /**
     * generates a idle time based on a LogEntry
     * @param logEntry the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntry: LogEntry): Long {
        //This is a mock type therefore
        return 0
    }


    /**
     * generates a idle time based on a device label
     * @param deviceLabel the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(deviceLabel: Int): Long {
        //This is a mock type therefore
        return 0
    }

    /**
     * generates a idle time based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntries: ArrayList<LogEntry>): Long {
        //This is a mock type therefore
        return 0
    }

    /**
     * generates a idle time based on a Log
     * @param log the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(log: Log): Long {
        //This is a mock type therefore
        return 0
    }

    /**
     * generates a timestamp based on a LogEntry
     * @param logEntry the base for the synthesis of the next timestamp
     * @return the next timestamp
     */

    override fun generateTimestamp(logEntry: LogEntry): DateTime {
        //This is a mock type therefore
        return baseTimestamp + Duration(1000)
    }

    /**
     * generates a timestamp based on a device label
     * @param deviceLabel the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(deviceLabel: Int): DateTime {
        //This is a mock type therefore
        return baseTimestamp + Duration(1000)
    }

    /**
     * generates a timestamp based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(logEntries: ArrayList<LogEntry>): DateTime {
        //This is a mock type therefore
        return baseTimestamp + Duration(1000)
    }

    /**
     * generates a timestamp based on a log
     * @param log the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(log: Log): DateTime {
        //This is a mock type therefore
        return baseTimestamp + Duration(1000)
    }
}
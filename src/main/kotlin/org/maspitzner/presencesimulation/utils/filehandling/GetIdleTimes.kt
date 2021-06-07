package org.maspitzner.presencesimulation.utils.filehandling

import org.maspitzner.presencesimulation.models.LogEntry

/**
 * returns the idle time between two log entries
 */
fun getIdleTimes(currentLogEntry: LogEntry, nextLogEntry: LogEntry): Long {
    return nextLogEntry.timeStamp.millis - currentLogEntry.timeStamp.millis
}
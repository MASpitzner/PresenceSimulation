package org.maspitzner.presencesimulation.simulation.timestamps

import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry

/**
 * Interface abstraction for models that are able train on a given dataset
 * implements also [TimestampGenerator]
 * @see [TimestampGenerator]
 */
interface FitableTimestampGenerator : TimestampGenerator {
    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param logEntries the data to train the model on
     * @param uniqueDevices the number of unique devices in the data
     */
    fun fit(logEntries: ArrayList<LogEntry>, uniqueDevices: Int = 0)

    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param log the data to train the model on
     */
    fun fit(log: Log)
}
package org.maspitzner.presencesimulation.simulation.events

import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry

/**
 * Interface abstracting the common functionalities of all Models for label prediction.
 * Contains methods for fitting given data and to predict a label based on the learned patterns.
 */
interface Model {

    /**
     * Fits the given data as log entries and unique devices.
     * @param logEntries a arraylist of LogEntries to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * @param uniqueDevices the number of distinct device labels in the given dataset
     */
    fun fit(logEntries: ArrayList<LogEntry>, acyclic: Boolean, uniqueDevices: Int = 0)

    /**
     * Fits the given data as log entries and unique devices.
     * @param log a Log to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * (default is false)
     */
    fun fit(log: Log, acyclic: Boolean = false)

    /**
     * Predicts a successor label based on a given LogEntry
     * @param logEntry the LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    fun predict(logEntry: LogEntry): Int

    /**
     * Predicts a successor label based on a set of given LogEntry
     * @param logEntries the set of given LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    fun predict(logEntries: ArrayList<LogEntry>): Int

    /**
     * Predicts a successor label based on a given log
     * @param log the log whose successor label is to predict
     * @return the successor label calculated
     */
    fun predict(log: Log): Int
}
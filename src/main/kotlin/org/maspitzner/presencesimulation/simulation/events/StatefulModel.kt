package org.maspitzner.presencesimulation.simulation.events

import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry

/**
 * Interface abstracting functionalities needed  to predict sequences of
 * Implements [Model]
 * @see [Model]
 *
 */
interface StatefulModel:Model {
    /**
     * Predicts a sequence of successor labels (numberOfWantedEvents many) based on a single LogEntry
     * @param logEntry the LogEntry on whose basis the sequence should be predicted
     * @param numberOfWantedEvents the amount of successor labels
     * @return a list of successor labels
     */
    fun predictSequence(logEntry: LogEntry, numberOfWantedEvents: Int): ArrayList<Int>

    /**
     * Predicts a sequence of successor labels (numberOfWantedEvents many) based on a List of LogEntries
     * @param logEntries the List of LogEntries on whose basis the sequence should be predicted
     * @param numberOfWantedEvents the amount of successor labels
     * @return a list of successor labels
     */
    fun predictSequence(logEntries: ArrayList<LogEntry>, numberOfWantedEvents: Int): ArrayList<Int>

    /**
     * Predicts a sequence of successor labels (numberOfWantedEvents many) based on Log
     * @param log the Log on whose basis the sequence should be predicted
     * @param numberOfWantedEvents the amount of successor labels
     * @return a list of successor labels
     */
    fun predictSequence(log: Log, numberOfWantedEvents: Int): ArrayList<Int>

    /**
     * Resets the previous predictions
     */
    fun resetPredictions()
}
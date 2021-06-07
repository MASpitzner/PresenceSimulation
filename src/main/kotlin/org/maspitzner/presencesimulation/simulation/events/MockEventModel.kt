package org.maspitzner.presencesimulation.simulation.events

import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry

/**
 * Debugging Class essentially does nothing relevant used to check for integration
 */
class MockEventModel : Model {

    init {
        println("MockEventModel instantiated")
    }

    /**
     * Since a mock class, does nothing
     * @param logEntries a arraylist of LogEntries to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * @param uniqueDevices the number of distinct device labels in the given dataset
     */
    override fun fit(logEntries: ArrayList<LogEntry>, acyclic: Boolean, uniqueDevices: Int) {
        println("MockEventModel fitting")
    }

    /**
     * Since a mock class, does nothing
     * @param log a Log to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * (default is false)
     */
    override fun fit(log: Log, acyclic: Boolean) {
        println("MockEventModel fitting")
    }

    /**
     * Since it is a mock class returns always 0
     * @param logEntry the LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntry: LogEntry): Int {
        //This is a mock type therefore
        return 0
    }

    /**
     * Since it is a mock class returns always 0
     * @param logEntries the set of given LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntries: ArrayList<LogEntry>): Int {
        //This is a mock type therefore
        return 0
    }

    /**
     * Since it is a mock class returns always 0
     * @param log the log whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(log: Log): Int {
        //This is a mock type therefore
        return 0
    }
}
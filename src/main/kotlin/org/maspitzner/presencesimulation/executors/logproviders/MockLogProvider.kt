package org.maspitzner.presencesimulation.executors.logproviders

import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.utils.configuration.Configuration

/**
 * Mock Log-Provider implemented for debug usage and integration
 * Implements [LogProvider]
 * @see [LogProvider]
 */
class MockLogProvider(override val config: Configuration) : LogProvider {
    init {
        println("MockLogProvider instantiated")
    }

    /**
     * Since this is a mock class it returns a empty log
     * @return empty log
     */
    override fun getStaticLog(): Log {
        println("returning MockLog")
        return Log()
    }
}
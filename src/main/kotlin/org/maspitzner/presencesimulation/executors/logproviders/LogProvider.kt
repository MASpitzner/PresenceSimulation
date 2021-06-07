package org.maspitzner.presencesimulation.executors.logproviders

import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.utils.configuration.Configuration

/**
 * Interface abstracting common functionalities among log providing classes
 * For future improvements one could implement a live update of a log
 * This interface should be implemented by alternative log providing classes
 */
interface LogProvider {
    val config:Configuration
    /**
     * Provides static log information gained by some Log File or similar
     * @return the extracted and parsed Log
     */
    fun getStaticLog(): Log
}
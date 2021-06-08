package org.maspitzner.presencesimulation.executors.logproviders

import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.parsers.OpenHabLogParser
import org.maspitzner.presencesimulation.utils.configuration.Configuration

/**
 * Class implementing the functionalities needed to provide log information from openhab log data
 * @param config the [Configuration] provided to run the simulation
 * Implements [LogProvider]
 * @see [LogProvider]
 */
class OpenHABLogProvider(override val config: Configuration) : LogProvider {
    /**
     * Returns a [Log]-Object read the paths provided in the [Configuration]-Object
     * @return the parsed log-information as Log
     */
    override fun getStaticLog(): Log {
        return OpenHabLogParser.parse(config.logPath, config.tokenList)
    }
}
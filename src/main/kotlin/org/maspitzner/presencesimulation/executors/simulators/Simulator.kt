package org.maspitzner.presencesimulation.executors.simulators

import org.maspitzner.presencesimulation.utils.configuration.Configuration

/**
 * Interface describing the functionalities needed to run a simulation either for console or a file
 */
interface Simulator {
    /**
     * [Configuration] object used to determine the type of simulation and other parameters
     */
    val config: Configuration

    /**
     * Runs a Simulation with specified parameters by the [Configuration] object
     * Results are printed to the console
     */
    fun runConsoleSimulation()

    /**
     * Runs a Simulation with specified parameters by the [Configuration] object
     * Results are printed a filed specified in the [Configuration] object
     */
    fun runLogSimulation()

}
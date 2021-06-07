package org.maspitzner.presencesimulation.utils.exceptions


/**
 * Custom exception thrown if there are less than one cluster requested
 */
class IllegalNumberOfClustersException : Throwable() {
    override val message: String
        get() = "At least one Cluster needs to be calculated!"
}

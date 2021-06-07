package org.maspitzner.presencesimulation.utils.datahandling

/**
 * simple helper class which only provides the functionality to apply standardization
 */
class Standardizer {
    fun <T : Number> scale(mean: T, standardDeviation: T, x: T) =
        (x.toDouble() - mean.toDouble()) / (standardDeviation.toDouble())
}

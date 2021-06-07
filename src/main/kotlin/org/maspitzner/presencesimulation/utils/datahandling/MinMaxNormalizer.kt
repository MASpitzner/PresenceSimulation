package org.maspitzner.presencesimulation.utils.datahandling

/**
 * simple helper class to apply and revert normalization to the interval [0,1]
 */
class MinMaxNormalizer {
    /**
     * scales a number to the interval [0,1]
     * @param min the minimum number of the given set of data
     * @param max the maximum number of the given set of data
     * @param x the number to scale
     * @return the scaled number
     */
    fun <T : Number> scale(min: T, max: T, x: T) =
        (x.toDouble() - min.toDouble()) / (max.toDouble() - min.toDouble())


    /**
     * descales a number from the interval [0,1]
     * @param min the minimum number of the given set of data
     * @param max the maximum number of the given set of data
     * @param x the number to descale
     * @return the descaled number
     */
    fun <T : Number> revert(min: T, max: T, x: T) =
        x.toDouble() * (max.toDouble() - min.toDouble()) + min.toDouble()

}

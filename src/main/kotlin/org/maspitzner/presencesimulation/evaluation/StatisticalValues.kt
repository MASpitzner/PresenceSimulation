package org.maspitzner.presencesimulation.evaluation

import java.math.BigDecimal

/**
 * Data class to represent the statistical results of a timestamp evaluation run
 * @param mean the arithmetic mean
 * @param variance the statistical variance
 * @param stdDev the standard deviation
 * @param stdError the standard error
 * @param relativeStdError the relative standard error
 */
data class StatisticalValues(
    val mean: BigDecimal,
    val variance: BigDecimal,
    val stdDev: BigDecimal,
    val stdError: BigDecimal,
    val relativeStdError: BigDecimal,
)
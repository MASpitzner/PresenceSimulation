package org.maspitzner.presencesimulation.evaluation

/**
 * Class implementing weight matrix functionalities for the [NeedlemanWunschAlgorithm]
 * Gap cost is chosen to be 0.5 since we want to allow shifts
 * if the overall score benefits more from it than from a mismatch
 */
data class WeightMatrix(
    val gapCost: Double = 0.5
) {
    /**
     * Returns the cost to replace one string against another one.
     * Chosen to be 1 if they mismatch else 0 since there is no real distance between two devices
     * (With more information this could be adapted to other costs e.g. lesser cost for devices in the same room)
     * @param first the first string to compare
     * @param second the other string to compare
     * @return 0 if [first] and [second] match else 1
     */
    fun getCost(first: String, second: String) = if (first == second) 0 else 1
}


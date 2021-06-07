package org.maspitzner.presencesimulation.evaluation

import java.lang.Double.min

/**
 * Class abstraction to implement the needleman wunsch algorithm.
 * This is used to calculate a global alignment between two label sequences
 * (at least in this context).
 *
 */
class NeedlemanWunschAlgorithm {
    //Since this is a dynamic programming algorithm it calculates the final result
    // via this table
    private lateinit var scoringMatrix: Array<Array<Double>>

    /**The [WeightMatrix] object which is used to calculate the score between two labels
     */
    private val weightMatrix = WeightMatrix()


    /**
     * Computes the scoring Matrix or DP-Table between two label lists.
     * The alignment score between the to label sequences can be found in the [scoringMatrix] at
     * position scoringMatrix[scoringMatrix.size - 1][scoringMatrix[0].size - 1]
     * This executes the needleman wunsch algorithm
     * @param firstSequence the first label sequence
     * @param secondSequence the other label sequence
     */
    private fun computeScoringMatrix(firstSequence: List<String>, secondSequence: List<String>) {


        scoringMatrix =
            Array((firstSequence.size + 1)) {
                Array(secondSequence.size + 1) { 0.0 }
            }


        for (i in scoringMatrix.indices) {
            for (j in 0 until scoringMatrix[i].size) {
                if (i == 0 || j == 0) {

                    scoringMatrix[i][j] = i.or(j) * weightMatrix.gapCost
                    continue
                }

                val cost = weightMatrix.getCost(firstSequence[i - 1], secondSequence[j - 1])
                val diagonalPredecessorCost = cost + scoringMatrix[i - 1][j - 1]
                val leftPredecessorCost = scoringMatrix[i][j - 1] + weightMatrix.gapCost
                val topPredecessorCost = scoringMatrix[i - 1][j] + weightMatrix.gapCost

                scoringMatrix[i][j] = min(
                    diagonalPredecessorCost,
                    min(
                        topPredecessorCost,
                        leftPredecessorCost
                    )
                )
            }
        }

    }

    /**
     * Calculates and returns the alignment quality between two label sequences.
     * @param firstSequence the first label sequence
     * @param secondSequence the other label sequence
     * @return the alignment quality as Double
     */
    fun align(firstSequence: List<String>, secondSequence: List<String>): Double {
        computeScoringMatrix(firstSequence, secondSequence)
        return scoringMatrix[scoringMatrix.size - 1][scoringMatrix[0].size - 1]
    }
}
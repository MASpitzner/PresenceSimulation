package org.maspitzner.presencesimulation.simulation.events

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Cluster
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.utils.exceptions.IllegalNumberOfClustersException
import kotlin.random.Random

/**
 * Implements functionality to cluster either single events or time series
 * based on their weather and time information.
 * @param numOfCluster the number of clusters to calculate
 * @param iterations the number of iterations for early stopping the k-means algorithm
 * @param log the training data as Log
 * @param seed a seed for randomization (default fixed for evaluation purposes)
 */
class KMeansClusterBuilder(
    private val numOfCluster: Int = 8,
    private val iterations: Int = 1000,
    val log: Log,
    seed: Long = 150
) {
    private var randomGenerator: Random
    private var bestClustering: Pair<Double, Array<Cluster>>

    init {
        if (numOfCluster < 1) {
            throw IllegalNumberOfClustersException()
        }
        randomGenerator = if (seed > 0) {
            Random(seed)
        } else {
            Random(DateTime.now().millis)
        }
        bestClustering = Pair(
            Double.MAX_VALUE,
            Array(numOfCluster) { Cluster(log[randomGenerator.nextInt(0, log.size)]) })
    }


    /**
     * Build a clustering with [numOfCluster] many clusters.
     * implements a k-means-algorithm.
     * @return the set of clusters generated
     */
    private fun buildClustering(): Array<Cluster> {

        if (numOfCluster < 1) {
            throw IllegalNumberOfClustersException()
        }
        val clusters = Array(numOfCluster) { Cluster(log[randomGenerator.nextInt(0, log.size)]) }
        var hasChanged = true
        var currentIteration = 0
        val distances = Array(numOfCluster) { 0.0 }
        val currentCluster = Array(log.size) { -1 }
        var logIndex = 0
        while (hasChanged && currentIteration < iterations) {
            hasChanged = false
            log.forEach { logEntry ->
                clusters.forEachIndexed { index, cluster ->
                    distances[index] = cluster.distanceToCentroid(logEntry)
                }
                var min = Double.MAX_VALUE
                var minIndex = 0
                distances.forEachIndexed { index, value ->
                    if (value < min) {
                        min = value
                        minIndex = index
                    }
                }

                if (currentCluster[logIndex] != minIndex) {
                    if (currentCluster[logIndex] != -1) {
                        clusters[currentCluster[logIndex]].removeEntryFromCluster(logEntry)
                    }
                    currentCluster[logIndex] = minIndex
                    hasChanged = true
                    clusters[minIndex].logEntries.add(logEntry)
                }

                logIndex++
            }
            clusters.forEach { it.calculateNewCentroid() }
            currentIteration++
            logIndex = 0
        }
        return clusters
    }

    /**
     * Implements a possible option to assess the cluster quality
     * @param clusters the clustering of which the quality should be determined
     * @return a numerical value indicating the quality
     */
    fun calculateVariance(clusters: Array<Cluster>): Double {
        var interClusterVariances = 0.0
        var intraClusterVariance = 0.0
        val mean = getMeanVector()

        clusters.forEach {
            interClusterVariances += it.calculateInterClusterVariance()
            intraClusterVariance += (it.distanceToCentroid(mean))

        }
        return intraClusterVariance + interClusterVariances
    }

    /**
     * Implements a possible extension to improve the model quality by assessing different ks
     * In this version just assessing the given k
     * @param assessedClusters the number of checked clusterings
     * @return a cluster quality and the respective cluster
     */
    fun calculateBestClustering(assessedClusters: Int = 1): Pair<Double, Array<Cluster>> {
        (0 until assessedClusters).forEach { _ ->

            val clustering = buildClustering()
            val clusterQuality = calculateWeightedVariance(clustering)
            if (clusterQuality < bestClustering.first) {
                bestClustering = Pair(clusterQuality, clustering)
            }
        }
        return bestClustering
    }

    /**
     * Calculates the weighted cluster variance for a given clustering
     * @param clusters the clustering to evaluate
     * @return a numerical value indicating the quality (lesser is better)
     */
    private fun calculateWeightedVariance(clusters: Array<Cluster>): Double {
        var interClusterVariances = 0.0
        var intraClusterVariance = 0.0
        val mean = getMeanVector()

        clusters.forEach {
            interClusterVariances += it.calculateInterClusterVariance()
            intraClusterVariance += (1 / numOfCluster * it.distanceToCentroid(mean))

        }
        return intraClusterVariance + (interClusterVariances * 1 / numOfCluster)
    }

    /**
     * Calculates a vector containing the means of all features
     * @return the feature mean vector
     */
    private fun getMeanVector(): ArrayList<Double> {
        val means = ArrayList<Double>()
        log[0].getFeatures().forEach { _ -> means.add(0.0) }
        log.forEach {
            it.getFeatures().forEachIndexed { index, feature ->
                means[index] += (feature / log.size)
            }
        }
        return means

    }
}
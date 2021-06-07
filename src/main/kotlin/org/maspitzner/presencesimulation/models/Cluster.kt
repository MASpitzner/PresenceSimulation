package org.maspitzner.presencesimulation.models

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * class to represent a cluster of LogEntries
 * @param startingEntry the LogEntry which is the first centroid
 */
data class Cluster(var startingEntry: LogEntry) {
    private var centroid = startingEntry.getFeatures()
    val logEntries = ArrayList<LogEntry>()


    /**
     * pads features to have equal feature lengths
     * @param centroidFeatures the features of the centroid of this cluster
     * @param otherEntryFeatures the features of the compared LogEntry
     */
    private fun padFeatures(centroidFeatures: ArrayList<Double>, otherEntryFeatures: ArrayList<Double>) {
        if (centroidFeatures.size > otherEntryFeatures.size) {
            while (centroidFeatures.size != otherEntryFeatures.size) {
                otherEntryFeatures.add(0.0)
            }
        } else if (centroidFeatures.size < otherEntryFeatures.size) {
            while (centroidFeatures.size != otherEntryFeatures.size) {
                centroidFeatures.add(0.0)
            }
        }
    }

    /**
     * removes the given LogEntry from the cluster during reassignment
     * @param logEntry the LogEntry to remove
     */
    fun removeEntryFromCluster(logEntry: LogEntry) {
        logEntries.remove(logEntry)
    }

    /**
     * calculates the euclidean distance between the given LogEntry and the current centroid
     * @param otherEntry the LogEntry whose distance should be computed
     * @return the euclidean distance
     */
    fun distanceToCentroid(otherEntry: LogEntry): Double {
        val centroidFeatures = centroid
        val otherEntryFeatures = otherEntry.getFeatures()
        padFeatures(centroidFeatures, otherEntryFeatures)
        var distance = centroidFeatures.foldIndexed(0.0) { index, sum, element ->
            sum + (element - otherEntryFeatures[index]).pow(2)
        }
        distance = sqrt(distance)
        return distance
    }

    /**
     * calculates the euclidean distance between the given list of features and the current centroid
     * @param otherEntry the features whose distance should be computed
     * @return the euclidean distance
     */
    fun distanceToCentroid(otherEntry: ArrayList<Double>): Double {
        val centroidFeatures = centroid
        padFeatures(centroidFeatures, otherEntry)
        var distance = centroidFeatures.foldIndexed(0.0) { index, sum, element ->
            sum + (element - otherEntry[index]).pow(2)
        }
        distance = sqrt(distance)
        return distance
    }

    /**
     * calculates the new centroid based on the LogEntries in this cluster
     */
    fun calculateNewCentroid() {
        val means = ArrayList<Double>()
        centroid.forEach { _ -> means.add(0.0) }
        logEntries.forEach {
            it.getFeatures().forEachIndexed { index, feature ->
                means[index] += (feature / logEntries.size)
            }
        }
        centroid = means
    }

    /**
     * calculates the inter cluster variance
     * @return the inter cluster variance
     */
    fun calculateInterClusterVariance(): Double {
        var variance = 0.0
        logEntries.forEach {
            variance += distanceToCentroid(it).pow(2)
        }
        return variance
    }

    /**
     * override of the toString() method for pretty printing
     */
    override fun toString(): String {
        return "$centroid\n${logEntries.size}"
    }

}
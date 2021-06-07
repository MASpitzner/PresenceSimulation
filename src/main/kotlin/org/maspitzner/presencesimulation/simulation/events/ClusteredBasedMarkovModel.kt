package org.maspitzner.presencesimulation.simulation.events

import org.maspitzner.presencesimulation.models.Cluster
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import kotlin.math.abs

/**
 * Class implementing the functionality needed for training and usage of a cluster-based markov chain model
 * @param kMin minimal k to use for k-means
 * @param kMax maximal k to use for k-means
 * Implements [Model] and [StatefulModel]
 * @see [Model]
 * @see [StatefulModel]
 */
class ClusteredBasedMarkovModel(private val kMin: Int = 8, private val kMax: Int = 8) :
    StatefulModel {
    private var acyclic = false
    private lateinit var clusters: Array<Cluster>
    private var k = 0
    private lateinit var models: Array<MarkovModel>
    var uniqueDevices = 0


    /**
     * Calculates the cluster membership for a given log entry to select the correct markov-modell
     * @param logEntry the LogEntry whose cluster membership is to calculate
     * @return the index of the cluster
     */
    private fun getClusterMembership(logEntry: LogEntry): Int {
        val clusterMembership = Array(clusters.size) { n -> clusters[n].distanceToCentroid(logEntry) }
        val minDistance = clusterMembership.minByOrNull { it }
        return clusterMembership.indexOf(minDistance)
    }

    /**
     * Calculates the cluster membership of a list of logEntries
     * @param logEntries the list of LogEntries to cluster
     * @return the index of the calculated cluster
     */
    private fun getClusterMembership(logEntries: ArrayList<LogEntry>): Int {
        val clusterMembership = Array(clusters.size) { n ->
            logEntries.sumOf { clusters[n].distanceToCentroid(it) } / logEntries.size
        }
        val minDistance = clusterMembership.minOrNull()
        return clusterMembership.indexOf(minDistance)
    }

    /**
     * Calculates the best k within the range of [kMin] and [kMax] (inclusive)
     * Implemented for experimental usage
     * Calculates for each possible k a clustering and determines the inter and intra cluster variance
     */
    private fun getBestK(logEntries: ArrayList<LogEntry>): Pair<Int, Array<Cluster>> {

        val bestK = ArrayList<Pair<Double, Double>>()
        val log = Log(logEntries)
        log.sortByTimestamp()
        val bestCluster = ArrayList<Array<Cluster>>()

        (kMin until kMax + 1).forEach { k ->
            val clusterBuilder = KMeansClusterBuilder(numOfCluster = k, log = log)
            val clustering = clusterBuilder.calculateBestClustering()
            val clusterQuality = clustering.first
            bestK.add(Pair(clusterQuality, 0.0))
            bestCluster.add(clustering.second)
        }
        bestK.forEachIndexed { i, _ ->
            if (i - 1 >= 0 && i + 1 < bestK.size) {
                val secondDerivative = abs(bestK[i + 1].first + bestK[i - 1].first - (2 * bestK[i].first))
                bestK[i] = Pair(bestK[i].first, secondDerivative)
            }
        }
        val finalK = bestK.indexOf(bestK.minByOrNull { it.first }) + kMin
        return Pair(finalK, bestCluster[finalK - kMin])
    }

    /**
     * Trains the Model on a given set of training data.
     * Calculates the clustering and builds for each cluster a markov-chain modell
     * @param logEntries the training data as ArrayList of LogEntries
     */
    private fun build(logEntries: ArrayList<LogEntry>) {
        val clustering = getBestK(logEntries)
        this.k = clustering.first
        this.clusters = clustering.second
        this.models = Array(this.clusters.size) { MarkovModel() }
        models.forEachIndexed { index, markovModel ->
            markovModel.fit(
                logEntries = clusters[index].logEntries, acyclic = this.acyclic,
                uniqueDevices = this.uniqueDevices
            )
        }
    }

    /**
     * Fits the given data as log entries and unique devices.
     * @param logEntries a arraylist of LogEntries to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * @param uniqueDevices the number of distinct device labels in the given dataset
     */
    override fun fit(logEntries: ArrayList<LogEntry>, acyclic: Boolean, uniqueDevices: Int) {
        if (uniqueDevices > 0) {
            this.uniqueDevices = uniqueDevices
        } else {
            this.uniqueDevices = logEntries.distinctBy { it.selectedDevice }.size
        }

        this.acyclic = acyclic
        build(logEntries)
    }

    /**
     * Fits the given data as log entries and unique devices.
     * @param log a Log to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * (default is false)
     */
    override fun fit(log: Log, acyclic: Boolean) {
        this.acyclic = acyclic
        uniqueDevices = log.deviceCount
        log.sortByTimestamp()
        this.uniqueDevices = log.deviceCount
        build(log.getEntries())
    }

    /**
     * Predicts a successor label based on a given LogEntry
     * @param logEntry the LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntry: LogEntry): Int {
        return models[getClusterMembership(logEntry)].predict(logEntry)
    }

    /**
     * Predicts a successor label based on a set of given LogEntry
     * @param logEntries the set of given LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntries: ArrayList<LogEntry>): Int {
        var deviceLabel = 0
        logEntries.forEach { deviceLabel = predict(it) }
        return deviceLabel
    }

    /**
     * Predicts a successor label based on a given log
     * @param log the log whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(log: Log): Int {
        return predict(log.getEntries())
    }

    /**
     * Predicts a sequence of successor labels (numberOfWantedEvents many) based on a single LogEntry
     * @param logEntry the LogEntry on whose basis the sequence should be predicted
     * @param numberOfWantedEvents the amount of successor labels
     * @return a list of successor labels
     */
    override fun predictSequence(logEntry: LogEntry, numberOfWantedEvents: Int): ArrayList<Int> {
        return models[getClusterMembership(logEntry)].predictSequence(logEntry, numberOfWantedEvents)
    }

    /**
     * Predicts a sequence of successor labels (numberOfWantedEvents many) based on a List of LogEntries
     * @param logEntries the List of LogEntries on whose basis the sequence should be predicted
     * @param numberOfWantedEvents the amount of successor labels
     * @return a list of successor labels
     */
    override fun predictSequence(logEntries: ArrayList<LogEntry>, numberOfWantedEvents: Int): ArrayList<Int> {
        return models[getClusterMembership(logEntries)].predictSequence(logEntries, numberOfWantedEvents)
    }

    /**
     * Predicts a sequence of successor labels (numberOfWantedEvents many) based on Log
     * @param log the Log on whose basis the sequence should be predicted
     * @param numberOfWantedEvents the amount of successor labels
     * @return a list of successor labels
     */
    override fun predictSequence(log: Log, numberOfWantedEvents: Int): ArrayList<Int> {
        return predictSequence(log.getEntries(), numberOfWantedEvents)
    }

    /**
     * Resets the previous predictions
     */
    override fun resetPredictions() {
        models.forEach { it.resetPredictions() }
    }

    /**
     * Overwrites toString for pretty printing of the hyperparameters
     */
    override fun toString(): String {
        val stringRepresentation = with(StringBuilder()) {
            append("kMin=${kMin}, kMax=${kMin},selectedK=${k}\n")
            append("acyclic=$acyclic\n")
            append("simulating $uniqueDevices devices")

        }
        models.forEach { stringRepresentation.append("$it\n") }

        return stringRepresentation.toString()
    }


}
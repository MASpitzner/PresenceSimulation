package org.maspitzner.presencesimulation.simulation.events

import org.joda.time.DateTime
import org.joda.time.Duration
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import kotlin.random.Random

/**
 * Class abstracting the functionality needed to build and use a Label-Model based on Markov-Chains.
 *
 * Implements [Model] and [StatefulModel]
 */
class MarkovModel : StatefulModel {

    /* initializes needed variables
            uniqueDevices - number of distinct labels inside the train data
            acyclic - boolean indicating whether backwards edges are allowed
            lastPredictedNode - set during prediction to keep track of the last node
                                inside the markov chain
            startNodes - a list of nodes which are the starting nodes in a given subsequence of the log
            seed and randomGenerator used to determine edges in the markov chains
     */
    var uniqueDevices: Int = 0
    private var acyclic = false
    private var lastPredictedNode: Node? = null
    private val startNodes = HashMap<Int, Node>()
    private var seed = 0L
    private var randomGenerator = Random(seed)


    /**
     * Splits a given log into subsequences of the given duration.
     * Used to determine starting nodes.
     * @param logEntries the given training data
     * @param duration the time window length
     * @return a collection of collections of LogEntries w.r.t. to their time frame membership
     */
    private fun splitIntoSubsequences(
        logEntries: ArrayList<LogEntry>,
        duration: Duration = Duration((2 * 60 * 60 * 1000))
    ): ArrayList<ArrayList<LogEntry>> {
        val subsequences = ArrayList<ArrayList<LogEntry>>()
        if (logEntries.isNotEmpty()) {
            var baseTimeStamp = logEntries[0].timeStamp
            var currentSubsequence = ArrayList<LogEntry>()
            logEntries.forEach {
                if (it.timeStamp > baseTimeStamp + duration) {
                    subsequences.add(currentSubsequence)
                    currentSubsequence = ArrayList()
                    baseTimeStamp = it.timeStamp
                }
                currentSubsequence.add(it)
            }.also { subsequences.add(currentSubsequence) }
        }

        return subsequences
    }


    /**
     * Trains a model based on the given LogEntries.
     * Splits first into subsequences based on a given duration.
     * Secondly builds a markov-chain for each subsequence and remembers the start nodes
     * @param logEntries the given train data
     * @param seed a given seed for random number generation (default is 150 might be replaced with 0 for real usage).
     */
    private fun build(logEntries: ArrayList<LogEntry>, seed: Long = 150) {
        this.seed = if (seed > 0) seed else DateTime.now().millis
        this.randomGenerator = Random(this.seed)
        //splits into subsequences
        val subsequences = splitIntoSubsequences(logEntries)
        //generates a markov chain for each subsequence
        subsequences.forEach { subsequence ->
            var currentNode = Node(uniqueDevices)
            var predecessor = -1

            subsequence.forEachIndexed { index, logEntry ->
                if (!acyclic || logEntry.selectedDevice != predecessor) {
                    if (index == 0) {
                        if (!startNodes.containsKey(logEntry.selectedDevice)) {
                            startNodes[logEntry.selectedDevice] = Node(uniqueDevices)
                        }
                        currentNode = startNodes[logEntry.selectedDevice]!!
                    } else {
                        if (!currentNode.transitions.containsKey(logEntry.selectedDevice)) {
                            currentNode.transitions[logEntry.selectedDevice] = Node(uniqueDevices)
                        }
                        currentNode.incoming++
                        currentNode.probabilities[logEntry.selectedDevice]++
                        currentNode = currentNode.transitions[logEntry.selectedDevice]!!
                    }

                }
                predecessor = logEntry.selectedDevice
            }
        }
    }

    /**
     * Fits the given data as log entries and unique devices.
     * @param logEntries a arraylist of LogEntries to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * @param uniqueDevices the number of distinct device labels in the given dataset
     */
    override fun fit(logEntries: ArrayList<LogEntry>, acyclic: Boolean, uniqueDevices: Int) {
        this.acyclic = acyclic
        if (uniqueDevices > 0) {
            this.uniqueDevices = uniqueDevices
        } else {
            this.uniqueDevices = logEntries.distinctBy { it.selectedDevice }.size
        }
        logEntries.sortBy { it.timeStamp }

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
        build(log.getEntries())
    }

    /**
     * Selects a node to proceed or start with a prediction based on the last device.
     * If the prediction is about to start and the given device isn't a start node observed
     * in the fitted train data a random start node is selected.
     * @param lastDevice the last device on whose basis the start node should be selected
     * @return the current prediction node
     */
    private fun getPredictionNode(lastDevice: Int) =
        if (lastPredictedNode == null) {
            val fromStartNode = if (startNodes.containsKey(lastDevice)) {
                startNodes[lastDevice]
            } else {
                val key = startNodes.keys.random(randomGenerator)
                startNodes[key]
            }
            fromStartNode
        } else {
            lastPredictedNode
        }

    /**
     * Predicts a successor label based on a given LogEntry
     * @param previousDevice the LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    private fun predict(previousDevice: Int): Int {
        //first selecting the current prediction node
        val predictionNode = getPredictionNode(previousDevice)
        //get the edge probabilities
        val currentTransitionProbabilities = predictionNode!!.getProbabilities()
        // create a map of probability to device label and sort it by probability
        val probabilityMap = currentTransitionProbabilities.mapIndexed { index, it ->
            it to index

        }.toList().sortedBy { it.first }


        // select the device based on a coin toss
        var toss = randomGenerator.nextDouble()
        var i = 0
        while (i + 1 < probabilityMap.size && toss > 0) {
            toss -= probabilityMap[i].first
            i++
        }
        lastPredictedNode = predictionNode.transitions[probabilityMap[i].second]
        return probabilityMap[i].second
    }

    /**
     * Predicts a successor label based on a given LogEntry
     * @param logEntry the LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntry: LogEntry): Int {

        return predict(logEntry.selectedDevice)
    }

    /**
     * Predicts a successor label based on a set of given LogEntry
     * @param logEntries the set of given LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntries: ArrayList<LogEntry>): Int {
        var nextDevice = -1
        splitIntoSubsequences(logEntries).last().forEach { nextDevice = predict(it) }
        return nextDevice

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
        val nextDevices = ArrayList<Int>()
        var lastDevice = logEntry.selectedDevice
        (0 until numberOfWantedEvents).forEach { _ ->
            val predictedDevice = predict(lastDevice)
            nextDevices.add(predictedDevice)
            lastDevice = predictedDevice
        }
        return nextDevices
    }

    /**
     * Predicts a sequence of successor labels (numberOfWantedEvents many) based on a List of LogEntries
     * @param logEntries the List of LogEntries on whose basis the sequence should be predicted
     * @param numberOfWantedEvents the amount of successor labels
     * @return a list of successor labels
     */
    override fun predictSequence(logEntries: ArrayList<LogEntry>, numberOfWantedEvents: Int): ArrayList<Int> {
        val nextDevices = ArrayList<Int>()
        var lastDevice = -1
        splitIntoSubsequences(logEntries).last().forEach { _ -> lastDevice = predict(lastDevice) }
        nextDevices.add(lastDevice)
        (0 until (numberOfWantedEvents - 1)).forEach { _ ->
            val predictedDevice = predict(lastDevice)
            nextDevices.add(predictedDevice)
            lastDevice = predictedDevice
        }
        return nextDevices
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
        lastPredictedNode = null
    }

    /**
     * Internal class to represent a node in the markov-chain
     * contains transitions and probabilities as well as the associated device label
     */
    private class Node(devices: Int) {
        val transitions = HashMap<Int, Node>()
        val probabilities = Array(devices) { 0 }
        var incoming = 0

        /**
         * returns the edge probabilities
         * @return the edge probabilities as Array where the index of the array corresponds to the device label
         */
        fun getProbabilities(): Array<Double> {
            return probabilities.map { it.toDouble() / incoming }.toTypedArray()
        }

        override fun toString(): String {
            val text = StringBuilder()
            transitions.forEach {
                text.append("${it.key}:${probabilities[it.key]} ")
            }
            text.append("incoming: $incoming\n")
            return text.toString()
        }

    }

}
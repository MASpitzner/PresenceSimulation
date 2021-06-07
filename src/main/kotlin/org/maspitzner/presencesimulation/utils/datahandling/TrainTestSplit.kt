package org.maspitzner.presencesimulation.utils.datahandling

import org.maspitzner.presencesimulation.models.Log

/**
 * data class to represent an abstraction of a split of a log into a test and a train proportion
 * @param trainData a log which represents the train proportion of the log
 * @param testData a log which represents the test proportion of the log
 */
data class TrainTestSplit(val trainData: Log, val testData: Log)


/**
 * data class to represent an abstraction of a split of a log into a test and a train proportion
 * @param log the log to split
 * @param splitRatio the ratio in to which degree the log will be split
 * @return a TrainTestSplit, which encapsulates the train and the test proportion
 */
fun getTestTrainSplit(log: Log, splitRatio: Double): TrainTestSplit {
    val entries = log.getEntries()
    val trainData = Log(arrayListOf(*entries.subList(0, (entries.size * splitRatio).toInt()).toTypedArray()))
    val testData =
        Log(arrayListOf(*entries.subList((entries.size * splitRatio).toInt(), entries.size).toTypedArray()))
    return TrainTestSplit(trainData, testData)
}
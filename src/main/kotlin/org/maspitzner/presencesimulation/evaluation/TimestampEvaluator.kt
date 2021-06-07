package org.maspitzner.presencesimulation.evaluation

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.utils.configuration.TimestampModelTypes
import org.maspitzner.presencesimulation.utils.filehandling.createFileIfNotExists
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/**
 * Class implementing functionalities to assess the quality of a timestamp simulation.
 */
class TimestampEvaluator {
    /**
     * Lambda to convert ms into s
     */
    val millisToSec = { it: Long -> it.toDouble() / 1000 }

    /**
     * lambda to extract the file path w.r.t. the model type
     */
    val getModelDirectorySubPath = { type: TimestampModelTypes ->
        val subPath = when (type) {
            TimestampModelTypes.UTSM -> "UniformGenerator"
            TimestampModelTypes.PTSM -> "PoissonGenerator"
            TimestampModelTypes.CPTSM -> "ClassBasedPoissonGenerator"
            TimestampModelTypes.TFTSM -> "TimeFrameGenerator"
            TimestampModelTypes.LSTMTSM -> "LSTMGenerator"
            else -> ""

        }

        subPath
    }

    /**
     * Evaluates the quality of al model types
     * @param modelPath the directory where to find the results of a generation run performed
     * by a [EvaluationDataGenerator]
     * @param groundTruthPath the directory where to find the ground truth of a generation run performed
     * by a [EvaluationDataGenerator]
     *
     */
    fun evaluateAllModelTypes(
        modelPath: String,
        groundTruthPath: String
    ) {
        evaluateModelType(TimestampModelTypes.UTSM, modelPath, groundTruthPath)
        evaluateModelType(TimestampModelTypes.PTSM, modelPath, groundTruthPath)
        evaluateModelType(TimestampModelTypes.CPTSM, modelPath, groundTruthPath)
        evaluateModelType(TimestampModelTypes.TFTSM, modelPath, groundTruthPath)
        evaluateModelType(TimestampModelTypes.LSTMTSM, modelPath, groundTruthPath)

    }


    /**
     * Evaluates a single model by iterating over all it's result and calculating relevant data
     * @param type the model type to evaluate
     * @param modelPath the path where to find its results
     * @param groundTruthPath the path to the ground truth
     */
    fun evaluateModelType(
        type: TimestampModelTypes,
        modelPath: String,
        groundTruthPath: String
    ) {
        val subPath = getModelDirectorySubPath(type)
        val logTypeResults = HashMap<String, ArrayList<String>>()
        val groundTruthResults = HashMap<String, HashMap<String, String>>()

        val stripResultNumber: (String, String, String) -> String = { it, logType, suffix ->
            it.replace(suffix, "").replace("$logType${File.separatorChar}", "")
        }


        File("$modelPath${File.separatorChar}$subPath${File.separatorChar}").listFiles()?.forEach { typeDir ->
            val logType =
                typeDir.absolutePath.replace("$modelPath${File.separatorChar}$subPath${File.separatorChar}", "")
            val resultPaths = ArrayList<String>()
            if (typeDir.isDirectory) {
                typeDir.listFiles()?.forEach { resultFile ->
                    resultPaths.add(resultFile.absolutePath)
                    stripResultNumber(resultFile.absolutePath, typeDir.absolutePath, ".result")
                }

                if (!logTypeResults.containsKey(logType)) {
                    logTypeResults[logType] = resultPaths
                }
            }

        }
        File("$groundTruthPath${File.separatorChar}").listFiles()?.forEach { typeDir ->
            val logType = typeDir.absolutePath.replace("$groundTruthPath${File.separatorChar}", "")
            val groundTruthPaths = HashMap<String, String>()
            typeDir.listFiles()?.forEach { resultFile ->
                groundTruthPaths[stripResultNumber(resultFile.absolutePath, typeDir.absolutePath, ".log")] =
                    resultFile.absolutePath
            }

            if (!groundTruthResults.containsKey(logType)) {
                groundTruthResults[logType] = groundTruthPaths
            }
        }
        val minMaxResults = HashMap<String, ArrayList<StatisticalValues>>()
        for ((logType, pathList) in logTypeResults) {
            val similarity = ArrayList<StatisticalValues>()
            pathList.forEachIndexed { _, it ->
                val logNumber = stripResultNumber(it.substring(it.indexOf(logType)), logType, ".result")
                val gtPath = groundTruthResults[logType]?.get(logNumber) ?: ""
                val result = evaluateTimestampSeries(it, gtPath)
                similarity.add(result)
            }
            minMaxResults[logType] = similarity
        }

        writeResult(minMaxResults, "$modelPath${File.separatorChar}$subPath")

    }


    /**
     * Calculates the difference between two timestamps for all timestamp in a given list.
     * @param synthetic the synthetic timestamps
     * @param groundTruth the ground truth to evaluate against
     * @return the differences as absolute positive values as list
     */
    private fun getDifferenceFromArrays(synthetic: List<DateTime>, groundTruth: List<DateTime>): List<Double> {

        return synthetic.mapIndexed { i, it ->
            abs(millisToSec(it.millis - groundTruth[i].millis))
        }

    }

    /**
     * Calculates the difference between two timestamps for all timestamp in two given files.
     * @param syntheticPath the path to the synthetic timestamps
     * @param groundTruthPath the path to the ground truth to evaluate against
     * @return the differences as absolute positive values as list
     */
    private fun getDifferenceFromFiles(syntheticPath: String, groundTruthPath: String): List<Double> {

        val syntheticFileContent = File(syntheticPath).readLines()

        val syntheticTimestampSeries = getTimestampSeriesFromFileContent(syntheticFileContent)
        val groundTruthFileContent = File(groundTruthPath).readLines().map { it.split(",")[0] }
        val groundTruthTimestampSeries = getTimestampSeriesFromFileContent(groundTruthFileContent)
        return getDifferenceFromArrays(syntheticTimestampSeries, groundTruthTimestampSeries)
    }

    /**
     * Extracts a sequence of timestamps from a list of string.
     * @param stringTimestampSeries the timestamp sequence as strings
     * @return the list of timestamps as timestamps
     */
    private fun getTimestampSeriesFromFileContent(stringTimestampSeries: List<String>): List<DateTime> {
        return stringTimestampSeries.map { DateTime(it) }
    }

    /**
     * Given two paths, extracts the timestamp sequences and calculates relevant statistical information to
     * assess the quality of the timestamp simulation.
     * @param syntheticPath the path to the synthetic data
     * @param groundTruthPath the path to the ground truth to evaluate against
     *
     */
    private fun evaluateTimestampSeries(
        syntheticPath: String,
        groundTruthPath: String
    ): StatisticalValues {
        val differences = getDifferenceFromFiles(syntheticPath, groundTruthPath)

        val precision = MathContext(20)
        val n = BigDecimal(differences.size)
        val nMinusOne = BigDecimal(differences.size - 1)

        var mean = BigDecimal.ZERO
        differences.forEach { mean += BigDecimal(it) }
        mean /= n

        val squareMeanError = differences.map { (BigDecimal(it) - mean).pow(2) }
        var variance = BigDecimal.ZERO
        squareMeanError.forEach { variance += it }
        variance /= nMinusOne

        val stdDeviation = variance.sqrt(precision)
        val stdError = stdDeviation / (n.sqrt(precision))
        return StatisticalValues(mean, variance, stdDeviation, stdError, BigDecimal(100) * (stdError / n))

    }

    /**
     * Writes the minimal and maximal values of the statistical interesting values to a given file
     * Writes null if no data available
     * @param results the statistical values to write to a file
     * @param path the path to the file to write to
     */
    private fun writeResult(results: HashMap<String, ArrayList<StatisticalValues>>, path: String) {

        if (path.endsWith("Generator", true) || path.endsWith("Generator/", true)) {
            val resultString = StringBuilder()
            resultString.append("log_type,min_mean,max_mean,min_var,max_var,min_stdDev,max_stdDev,min_stdErr,max_stdErr,min_relStdErr,max_relStdErr\n")
            results.forEach { (logType, result) ->
                resultString.append("$logType,")
                resultString.append("${result.minOfOrNull { it.mean }},")
                resultString.append("${result.maxOfOrNull { it.mean }},")
                resultString.append("${result.minOfOrNull { it.variance }},")
                resultString.append("${result.maxOfOrNull { it.variance }},")
                resultString.append("${result.minOfOrNull { it.stdDev }},")
                resultString.append("${result.maxOfOrNull { it.stdDev }},")
                resultString.append("${result.minOfOrNull { it.stdError }},")
                resultString.append("${result.maxOfOrNull { it.stdError }},")
                resultString.append("${result.minOfOrNull { it.relativeStdError }},")
                resultString.append("${result.maxOfOrNull { it.relativeStdError }}\n")

            }
            File("$path${File.separatorChar}summary.csv")
                .apply { createFileIfNotExists(this.absolutePath) }
                .writeText(resultString.toString())
        }

    }
}
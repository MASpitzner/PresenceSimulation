package org.maspitzner.presencesimulation.evaluation

import org.maspitzner.presencesimulation.utils.configuration.Configuration
import org.maspitzner.presencesimulation.utils.filehandling.createFileIfNotExists
import java.io.File

/**
 * Class implementing the needed functionality to evaluate a given set of generated synthetic log information
 * w.r.t. the Label quality
 * @param config a [Configuration] object containing necessary information to run a evaluation
 */
class LabelEvaluator(private val config: Configuration) {
    // The evaluation algorithm in this case the needleman wunsch algorithm
    private val algorithm = NeedlemanWunschAlgorithm()

    // predefined model sub paths
    private val modelPaths = listOf("BasicMarkov", "LSTM", "MLP", "ClusteredMarkov")

    // predefined path to ground truth directory
    private val groundTruthSubPath = "${File.separatorChar}groundTruth${File.separatorChar}"

    // extracts the log number from a path string
    val stripResultNumber: (String, String, String) -> String = { it, type, suffix ->
        it.replace(suffix, "").replace("$type${File.separatorChar}", "")
    }

    /**
     * Function to trigger a evaluation
     */
    fun run() {
        val logSubtypes = listOf(
            "fewZipfianLog",
            "largeFewUniformLog",
            "largeManyUniformLog",
            "realLog",
            "smallFewUniformLog",
            "smallManyUniformLog",
            "manyZipfianLog"
        )
        logSubtypes.forEach { logSubtype ->
            evalLogSubPath(logSubtype)
        }
    }

    /**
     * Extracts the sequence of labels from a given file
     * @param path the path to the generated log file
     */
    private fun getSequenceFrom(path: String): List<String> {


        val content = File(path).readLines()
        return if (path.contains("groundTruth")) {
            content.map { it.split(",")[1] }
        } else {
            content
        }
    }

    /**
     * Evaluates the results of a model w.r.t. the appropriate log type
     * @param modelType the string representing the model type
     * @param logSubtype the string representing the model type
     */
    private fun evalModelType(modelType: String, logSubtype: String) {
        val similarities = ArrayList<Double>()
        // assembles the ground truth directory
        val groundTruthDir = if (config.evaluationPath.endsWith(File.separatorChar)) "${
            config.evaluationPath.substring(
                0,
                config.evaluationPath.length - 1
            )
        }${groundTruthSubPath}" else "${config.evaluationPath}$groundTruthSubPath"
        // assembles the result directory
        val resultDir = "${config.evaluationPath}models${File.separatorChar}$modelType${File.separatorChar}$logSubtype"

        /* iterates over all generated synthetic log information and evaluates them with the needleman wunsch algorithm
            calculates the appropriate relative similarity
         */
        File(resultDir).listFiles()
            ?.forEach {
                if (!it.absolutePath.contains("summary")) {
                    val resultNumber = stripResultNumber(
                        it.absolutePath,
                        resultDir,
                        ".result"
                    )
                    val groundTruth =
                        getSequenceFrom("$groundTruthDir$logSubtype${File.separatorChar}$resultNumber.log")
                    val result = getSequenceFrom(it.absolutePath)
                    val score = algorithm.align(result, groundTruth) / result.size
                    similarities.add(1 - score)
                }
            }
        //writes the summarized results
        val summaryPath = "${resultDir}${File.separatorChar}summary.txt"
        createFileIfNotExists(summaryPath)
        writeSummary(summaryPath, similarities)
    }

    //triggers evaluation of a log type
    private fun evalLogSubPath(logSubtype: String) {
        modelPaths.forEach { modelType ->
            evalModelType(modelType, logSubtype)

        }
    }



    /**
     * function to write a summary of an evaluation run to a file
     * @param path to the file where the results should be written to
     * @param similarities list of similarity results
     */
    private fun writeSummary(path: String, similarities: ArrayList<Double>) {
        val stringRep = StringBuilder()
        similarities.forEach {
            stringRep.append("$it\n")
        }
        stringRep.append("Average: ${similarities.sum() / similarities.size}")

        File(path).writeText(stringRep.toString())
    }
}
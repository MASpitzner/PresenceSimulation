package org.maspitzner.presencesimulation.evaluation

import org.maspitzner.presencesimulation.models.WeatherDataEntry
import org.maspitzner.presencesimulation.parsers.OpenWeatherMapParser
import org.maspitzner.presencesimulation.simulation.events.*
import org.maspitzner.presencesimulation.simulation.timestamps.*
import org.maspitzner.presencesimulation.utils.configuration.Configuration
import org.maspitzner.presencesimulation.utils.configuration.EventModelTypes
import org.maspitzner.presencesimulation.utils.configuration.TimestampModelTypes
import org.maspitzner.presencesimulation.utils.datahandling.getTestTrainSplit
import org.maspitzner.presencesimulation.utils.exceptions.NotEvaluableModelException
import org.maspitzner.presencesimulation.utils.filehandling.createDirIfNotExists
import org.maspitzner.presencesimulation.utils.filehandling.createFileIfNotExists
import java.io.File
import kotlin.system.measureNanoTime

/**
 * Class to evaluate the training runtime of a given model or all models
 * @param config [Configuration] object to define the behaviour of the evaluator
 */
class RuntimeEvaluator(private val config: Configuration) {

    /**
     * The generated results
     */
    private val result = ArrayList<Long>()

    /**
     * The weather data to attribute a generated evaluation log
     */
    private val weatherData: List<WeatherDataEntry> = OpenWeatherMapParser.parse(config.weatherDataPath)
    private val basePath = config.evaluationPath.replace("evaluation/", "runtime/")

    /**
     * size parameters of the evaluation logs
     */
    private val numberMany = 150
    private val numberLarge = 15000

    /**
     * Evaluates the training runtime of all implemented model types
     * @param numberOfTrainings the amount of evaluation iterations
     */
    private fun runAll(numberOfTrainings: Int) {
        val timestampModels = TimestampModelTypes.values()
            .filter { it != TimestampModelTypes.MOCK_TIMESTAMP_MODEL && it != TimestampModelTypes.EVAL }
        val eventModelTypes = EventModelTypes.values()
            .filter { it != EventModelTypes.MOCK_EVENT_MODEL && it != EventModelTypes.EVAL }

        eventModelTypes.forEach {
            evaluateLabelModelRuntime(numberOfTrainings, it)
            writeResult("$basePath${it}.runtime")
            result.clear()
        }

        timestampModels.forEach {
            evaluateTimestampModelRuntime(numberOfTrainings, it)

            writeResult("$basePath${it}.runtime")
            result.clear()
        }
    }

    /**
     * Writes the result of the runtime evaluation to a file
     */
    private fun writeResult(resultPath: String) {
        createFileIfNotExists(resultPath)
        var averageRuntime = 0.0
        result.forEach { averageRuntime += it / result.size }
        val textResult = result.joinToString(";")

        File(resultPath).writeText("$textResult\n$averageRuntime")
    }

    /**
     * Evaluates the runtime of the training of one ore all models.
     * Writes the results to a file.
     * @param numberOfTrainings the amount of evaluation iterations
     */
    fun evaluateModelRuntime(numberOfTrainings: Int) {
        val eventModelNotEvaluable =
            (config.eventModelType == EventModelTypes.MOCK_EVENT_MODEL || config.eventModelType == EventModelTypes.EVAL)
        val timestampModelNotEvaluable =
            (config.timestampModelType == TimestampModelTypes.EVAL || config.timestampModelType == TimestampModelTypes.MOCK_TIMESTAMP_MODEL)

        createDirIfNotExists(basePath)

        if (config.evaluationSingle) {
            val resultPath: String = if (!eventModelNotEvaluable) {
                evaluateLabelModelRuntime(numberOfTrainings, config.eventModelType)
                "$basePath${config.eventModelType}.runtime"
            } else if (!timestampModelNotEvaluable) {
                evaluateTimestampModelRuntime(numberOfTrainings, config.timestampModelType)
                "$basePath${config.timestampModelType}.runtime"

            } else {
                throw NotEvaluableModelException()
            }
            writeResult(resultPath)
            result.clear()
        } else {
            runAll(numberOfTrainings)
        }

    }


    /**
     * Generates the training log and
     * evaluates the training runtime of a label model.
     * @param numberOfTrainings the amount of training iterations
     * @param eventModelTypes the model type to evaluate
     */
    private fun evaluateLabelModelRuntime(numberOfTrainings: Int, eventModelTypes: EventModelTypes) {
        (0 until numberOfTrainings).forEach {
            val log = RandomLogGenerator().generateZipfianLog(
                numberMany,
                numberLarge,
                weatherData = weatherData,
                randomStart = true
            )
            val (trainData, _) = getTestTrainSplit(log, 0.8)


            val model: Model = when (eventModelTypes) {
                EventModelTypes.MKM -> {
                    MarkovModel()
                }
                EventModelTypes.CMKM -> {
                    ClusteredBasedMarkovModel()
                }
                EventModelTypes.LSTMM -> {
                    val tempModel = LSTMModel()
                    tempModel
                }
                EventModelTypes.MLPM -> {
                    val tempModel = MLPModel()
                    tempModel
                }

                else -> {
                    throw NotEvaluableModelException()
                }
            }
            val runtime = measureNanoTime {
                model.fit(trainData)
            }

            result.add(runtime)
            println("Evaluating Runtime of: $eventModelTypes ${it + 1}/$numberOfTrainings runs")
        }
    }

    /**
     * Generates the training log and
     * evaluates the training runtime of a timestamp model.
     * @param numberOfTrainings the amount of training iterations
     * @param timestampModelTypes the model type to evaluate
     */
    private fun evaluateTimestampModelRuntime(numberOfTrainings: Int, timestampModelTypes: TimestampModelTypes) {
        (0 until numberOfTrainings).forEach {

            val log = RandomLogGenerator().generateZipfianLog(
                numberMany,
                numberLarge,
                weatherData = weatherData,
                randomStart = true
            )
            val (trainData, testData) = getTestTrainSplit(log, 0.8)
            val baseTime = testData[0].timeStamp


            val timestampGenerator: TimestampGenerator = when (timestampModelTypes) {


                TimestampModelTypes.UTSM -> UniformTimestampGenerator(baseTime)
                TimestampModelTypes.PTSM -> PoissonTimestampGenerator(baseTime)
                TimestampModelTypes.CPTSM -> ClassBasedPoissonTimestampGenerator(baseTime)
                TimestampModelTypes.TFTSM -> TimeFrameTimestampGenerator(baseTime)
                TimestampModelTypes.LSTMTSM -> LSTMTimestampGenerator(baseTime)
                else -> throw NotEvaluableModelException()
            }

            val runtime = measureNanoTime {
                if (timestampGenerator !is UniformTimestampGenerator) {
                    (timestampGenerator as FitableTimestampGenerator).fit(trainData)
                } else {
                    UniformTimestampGenerator(baseTime)
                }
            }

            println("Evaluating Runtime of: $timestampModelTypes ${it + 1}/$numberOfTrainings runs")

            result.add(runtime)
        }
    }

}
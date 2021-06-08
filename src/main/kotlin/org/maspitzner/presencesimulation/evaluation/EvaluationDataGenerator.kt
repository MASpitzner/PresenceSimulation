package org.maspitzner.presencesimulation.evaluation

import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.WeatherDataEntry
import org.maspitzner.presencesimulation.parsers.OpenHabLogParser
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

/**
 * Class implementing functionalities to run models against generated evaluation logs or a real log
 * and persist the results in files for later usage
 * @param config [Configuration] object specifying the behaviour of the Generator
 */
class EvaluationDataGenerator(private val config: Configuration) {
    private val fs = File.separator

    //predefine persistence paths
    private val basePath = config.evaluationPath.replace("evaluation/", "")
    private val evaluationPath = config.evaluationPath
    private val modelEvaluationPath = "${evaluationPath}models"
    private val timestampEvaluationPath = "${evaluationPath}timestampGenerators"
    private val groundTruthPath = "${evaluationPath}groundTruth"
    private val logTypeSubPaths = listOf(
        "smallFewUniformLog",
        "smallManyUniformLog",
        "largeFewUniformLog",
        "largeManyUniformLog",
        "fewZipfianLog",
        "manyZipfianLog",
        "realLog"
    )

    //weather data to attribute from
    private val weatherData: List<WeatherDataEntry> = OpenWeatherMapParser.parse(config.weatherDataPath)

    /**
     * Runs the generation of evaluation data for as specified in the [Configuration] object
     * @param numberOfLogs the number of evaluation iterations
     * @param start a index to start from (in case of abort while generating)
     */
    fun runGeneration(numberOfLogs: Int, start: Int = 0) {
        if (config.evalReal) {
            val realLog = OpenHabLogParser.parse(config.logPath, config.tokenList)
            realLog.setWeatherInformation(weatherData)
            realLog.setWeatherLabels()
            realLog.scaleLog()
            generate(realLog, 0, "realLog")
        } else {
            generate(numberOfLogs, start)
        }

    }

    /**
     * Retrieves a string representation of the label model name
     * @param model the label model used
     * @return the name as string
     */
    private fun getModelName(model: Model): String {
        return when (model) {
            is LSTMModel -> {
                "LSTM"
            }
            is MLPModel -> {
                "MLP"
            }
            is MarkovModel -> {
                "BasicMarkov"
            }
            is ClusteredBasedMarkovModel -> {
                "ClusteredMarkov"
            }
            else -> "noModel"
        }

    }

    /**
     * Retrieves a string representation of the timestamp model name
     * @param timestampGenerator the timestamp model used
     * @return the name of the model as string
     */
    private fun getTimestampGeneratorName(timestampGenerator: TimestampGenerator): String {
        return when (timestampGenerator) {
            is LSTMTimestampGenerator -> {
                "LSTMGenerator"
            }
            is PoissonTimestampGenerator -> {
                "PoissonGenerator"
            }
            is ClassBasedPoissonTimestampGenerator -> {
                "ClassBasedPoissonGenerator"
            }
            is TimeFrameTimestampGenerator -> {
                "TimeFrameGenerator"
            }
            is UniformTimestampGenerator -> {
                "UniformGenerator"
            }
            else -> "noGenerator"
        }
    }

    /**
     * Generates necessary file structure if not available (requires write permission)
     * @param models the list of models to generate data for
     * @param generators the list of timestamp models to generate data for
     */
    private fun generateEvaluationDirectories(models: List<Model>, generators: List<TimestampGenerator>) {
        createDirIfNotExists(basePath)
        createDirIfNotExists(evaluationPath)

        if (!File(groundTruthPath).exists()) {
            createDirIfNotExists(groundTruthPath)
            logTypeSubPaths.forEach {
                createDirIfNotExists("$groundTruthPath${fs}$it")
            }
        }

        createDirIfNotExists(modelEvaluationPath)
        createDirIfNotExists(timestampEvaluationPath)

        models.forEach { model ->
            createDirIfNotExists("$modelEvaluationPath${fs}${getModelName(model)}")
            logTypeSubPaths.forEach {
                createDirIfNotExists("$modelEvaluationPath${fs}${getModelName(model)}${fs}$it")
            }
        }

        generators.forEach { generator ->
            createDirIfNotExists("$timestampEvaluationPath${fs}${getTimestampGeneratorName(generator)}")
            logTypeSubPaths.forEach {
                createDirIfNotExists("$timestampEvaluationPath${fs}${getTimestampGeneratorName(generator)}${fs}$it")
            }
        }
    }

    /**
     * Generates the specified number of logs w.r.t. the type and size as specified in the thesis
     * @param numberOfLogs the number of logs as specified in the [Configuration] object
     * @param start a starting index (in case of failure during generation)
     */
    private fun generate(numberOfLogs: Int, start: Int = 0) {

        val numberFew = 30
        val numberMany = 150
        val numberSmall = 3000
        val numberLarge = 15000

        (start until numberOfLogs).forEach { currentLog ->
            val fewZipfianLog = RandomLogGenerator().generateZipfianLog(
                numberFew,
                numberLarge,
                weatherData = weatherData,
                randomStart = true
            )
            fewZipfianLog.scaleLog()

            val manyZipfianLog = RandomLogGenerator().generateZipfianLog(
                numberMany,
                numberLarge,
                weatherData = weatherData,
                randomStart = true
            )
            manyZipfianLog.scaleLog()

            val smallFewUniformLog =
                RandomLogGenerator().generateUniformLog(
                    numberFew,
                    numberSmall,
                    weatherData = weatherData,
                    randomStart = true
                )
            smallFewUniformLog.scaleLog()

            val smallManyUniformLog =
                RandomLogGenerator().generateUniformLog(
                    numberMany,
                    numberSmall,
                    weatherData = weatherData,
                    randomStart = true
                )
            smallManyUniformLog.scaleLog()

            val largeFewUniformLog =
                RandomLogGenerator().generateUniformLog(
                    numberFew,
                    numberLarge,
                    weatherData = weatherData,
                    randomStart = true
                )
            largeFewUniformLog.scaleLog()

            val largeManyUniformLog =
                RandomLogGenerator().generateUniformLog(
                    numberMany,
                    numberLarge,
                    weatherData = weatherData,
                    randomStart = true
                )
            largeManyUniformLog.scaleLog()

            if (!config.evaluationSingle) {
                generate(smallFewUniformLog, currentLog, "smallFewUniformLog")
                generate(smallManyUniformLog, currentLog, "smallManyUniformLog")
                generate(largeFewUniformLog, currentLog, "largeFewUniformLog")
                generate(largeManyUniformLog, currentLog, "largeManyUniformLog")
                generate(fewZipfianLog, currentLog, "fewZipfianLog")
                generate(manyZipfianLog, currentLog, "manyZipfianLog")
            } else {
                val eventModelNotEvaluable =
                    (config.eventModelType == EventModelTypes.MOCK_EVENT_MODEL || config.eventModelType == EventModelTypes.EVAL)
                val timestampModelNotEvaluable =
                    (config.timestampModelType == TimestampModelTypes.EVAL || config.timestampModelType == TimestampModelTypes.MOCK_TIMESTAMP_MODEL)


                if (eventModelNotEvaluable && timestampModelNotEvaluable) {
                    throw NotEvaluableModelException()
                }
                if (!timestampModelNotEvaluable) {
                    generateForSingleTimestampModel(smallFewUniformLog, currentLog, "smallFewUniformLog")
                    generateForSingleTimestampModel(smallManyUniformLog, currentLog, "smallManyUniformLog")
                    generateForSingleTimestampModel(largeFewUniformLog, currentLog, "largeFewUniformLog")
                    generateForSingleTimestampModel(largeManyUniformLog, currentLog, "largeManyUniformLog")
                    generateForSingleTimestampModel(fewZipfianLog, currentLog, "fewZipfianLog")
                    generateForSingleTimestampModel(manyZipfianLog, currentLog, "manyZipfianLog")
                }
                if (!eventModelNotEvaluable) {
                    generateForSingleLabelModel(smallFewUniformLog, currentLog, "smallFewUniformLog")
                    generateForSingleLabelModel(smallManyUniformLog, currentLog, "smallManyUniformLog")
                    generateForSingleLabelModel(largeFewUniformLog, currentLog, "largeFewUniformLog")
                    generateForSingleLabelModel(largeManyUniformLog, currentLog, "largeManyUniformLog")
                    generateForSingleLabelModel(fewZipfianLog, currentLog, "fewZipfianLog")
                    generateForSingleLabelModel(manyZipfianLog, currentLog, "manyZipfianLog")
                }

            }
            println("Progress: ${currentLog + 1}/$numberOfLogs Logs evaluated")

        }
    }

    /**
     * Runs the data generation for a specific log and writes the result to a file
     * @param log the log to generate a synthetic extension for
     * @param currentIndex the current iteration index as file name
     * @param logTypeSubPath the type of log as directory postfix as string
     */
    private fun generate(log: Log, currentIndex: Int, logTypeSubPath: String = "") {


        //retrieve the test and train data from the given log
        val (trainData, testData) = getTestTrainSplit(log, 0.8)
        val baseTime = testData[0].timeStamp

        // initialize all models
        val modelList = listOf(
            ClusteredBasedMarkovModel(),
            MLPModel(),
            LSTMModel(),
            MarkovModel()
        )
        // initialize all timestamp generators
        val timestampGeneratorList = listOf(
            LSTMTimestampGenerator(baseTime),
            PoissonTimestampGenerator(baseTime),
            ClassBasedPoissonTimestampGenerator(baseTime),
            TimeFrameTimestampGenerator(baseTime),
            UniformTimestampGenerator(baseTime)
        )

        // generate the directory structure
        generateEvaluationDirectories(modelList, timestampGeneratorList)

        // persist the ground truth for later evaluation
        val groundTruth = testData.getEntries().map {
            Pair(it.timeStamp, it.selectedDevice)
        }
        persistGroundTruth(
            groundTruth,
            "$groundTruthPath${fs}$logTypeSubPath${fs}${currentIndex}.log"
        )

        //train the label models
        modelList.forEach { model ->
            when (model) {
                is LSTMModel -> {
                    model.fit(trainData)
                }
                is MLPModel -> {
                    model.fit(trainData)
                }
                is MarkovModel -> {
                    model.fit(trainData)
                }
                is ClusteredBasedMarkovModel -> {
                    model.fit(trainData)
                }
            }

        }
        //train the timestamp models
        timestampGeneratorList.forEach { timeStampGenerator ->
            when (timeStampGenerator) {
                is LSTMTimestampGenerator -> {
                    timeStampGenerator.fit(trainData)
                }
                is PoissonTimestampGenerator -> {
                    timeStampGenerator.fit(trainData)
                }
                is ClassBasedPoissonTimestampGenerator -> {
                    timeStampGenerator.fit(trainData)
                }
                is TimeFrameTimestampGenerator -> {
                    timeStampGenerator.fit(trainData)
                }
            }

        }

        // generate the synthetic log data
        modelList.forEach { model ->
            generateForLabelModel(testData, model, logTypeSubPath, currentIndex)
        }
        timestampGeneratorList.forEach { timestampGenerator ->
            generateForTimestampGenerator(testData, timestampGenerator, logTypeSubPath, currentIndex)
        }
    }


    /**
     * Generates and persists evaluation data for a single timestamp model type
     * @param log the log to generate a synthetic extension for
     * @param currentIndex the current iteration index as file name
     * @param logTypeSubPath the type of log as directory postfix as string
     */
    private fun generateForSingleTimestampModel(log: Log, currentIndex: Int, logTypeSubPath: String = "") {
        val (trainData, testData) = getTestTrainSplit(log, 0.8)
        val baseTime = testData[0].timeStamp

        val timestampGenerator: TimestampGenerator = when (config.timestampModelType) {


            TimestampModelTypes.UTSM -> UniformTimestampGenerator(baseTime)
            TimestampModelTypes.PTSM -> PoissonTimestampGenerator(baseTime)
            TimestampModelTypes.CPTSM -> ClassBasedPoissonTimestampGenerator(baseTime)
            TimestampModelTypes.TFTSM -> TimeFrameTimestampGenerator(baseTime)
            TimestampModelTypes.LSTMTSM -> LSTMTimestampGenerator(baseTime)


            else -> throw NotEvaluableModelException()
        }

        if (timestampGenerator !is UniformTimestampGenerator) {
            (timestampGenerator as FitableTimestampGenerator).fit(trainData)
        }
        generateForTimestampGenerator(testData, timestampGenerator, logTypeSubPath, currentIndex)

        val groundTruth = testData.getEntries().map {
            Pair(it.timeStamp, it.selectedDevice)
        }
        persistGroundTruth(
            groundTruth,
            "$groundTruthPath${fs}$logTypeSubPath${fs}${currentIndex}.log"
        )

    }

    /**
     * Generates and persists evaluation data for a single label model type
     * @param log the log to generate a synthetic extension for
     * @param currentIndex the current iteration index as file name
     * @param logTypSubPath the type of log as directory postfix as string
     */
    private fun generateForSingleLabelModel(log: Log, currentIndex: Int, logTypSubPath: String = "") {
        val (trainData, testData) = getTestTrainSplit(log, 0.8)


        val model: Model = when (config.eventModelType) {
            EventModelTypes.MKM -> {
                MarkovModel()
            }
            EventModelTypes.CMKM -> {
                ClusteredBasedMarkovModel()
            }
            EventModelTypes.LSTMM -> {
                LSTMModel()
            }
            EventModelTypes.MLPM -> {
                val tempModel = MLPModel()
                tempModel
            }

            else -> {
                throw NotEvaluableModelException()
            }
        }
        model.fit(trainData)
        generateForLabelModel(testData, model, logTypSubPath, currentIndex)

        val groundTruth = testData.getEntries().map {
            Pair(it.timeStamp, it.selectedDevice)
        }
        persistGroundTruth(
            groundTruth,
            "$groundTruthPath${fs}$logTypSubPath${fs}${currentIndex}.log"
        )


    }

    /**
     * Generates and persists evaluation data for a single label model type
     * @param testData the log to generate a synthetic extension for
     * @param model the model to generate data with
     * @param currentIndex the current iteration index as file name
     * @param logTypSubPath the type of log as directory postfix as string
     */
    private fun generateForLabelModel(
        testData: Log,
        model: Model,
        logTypSubPath: String,
        currentIndex: Int
    ) {

        val labelSequence = ArrayList<Int>()

        testData.forEach {
            labelSequence.add(model.predict(it))
        }

        createDirIfNotExists("$modelEvaluationPath${fs}${getModelName(model)}${fs}$logTypSubPath${fs}")
        val persistPath =
            "$modelEvaluationPath${fs}${getModelName(model)}${fs}$logTypSubPath${fs}$currentIndex.result"
        persistResult(
            labelSequence, persistPath
        )
    }

    /**
     * Generates and persists evaluation data for a single label model type
     * @param testData the log to generate a synthetic extension for
     * @param timestampGenerator the model to generate data with
     * @param currentIndex the current iteration index as file name
     * @param logTypSubPath the type of log as directory postfix as string
     */
    private fun generateForTimestampGenerator(
        testData: Log,
        timestampGenerator: TimestampGenerator,
        logTypSubPath: String,
        currentIndex: Int
    ) {
        val timestampSequence = ArrayList<DateTime>()
        testData.forEach {
            timestampSequence.add(timestampGenerator.generateTimestamp(it))
        }
        createDirIfNotExists("$timestampEvaluationPath${fs}${getTimestampGeneratorName(timestampGenerator)}${fs}$logTypSubPath${fs}")
        val persistPath =
            "$timestampEvaluationPath${fs}${getTimestampGeneratorName(timestampGenerator)}${fs}$logTypSubPath${fs}$currentIndex.result"
        persistResult(
            timestampSequence, persistPath
        )
    }

    /**
     * Persists the result of a generation run in a file
     * @param generated the data generated either labels or timestamps
     * @param persistPath the path to write the data to
     */
    private fun persistResult(generated: ArrayList<*>, persistPath: String) {
        createFileIfNotExists(persistPath)
        val persistFile = File(persistPath)
        val stringRepresentation = StringBuilder()
        generated.forEachIndexed { i, it ->
            if (i < generated.size - 1) {
                stringRepresentation.append("$it\n")
            }
        }
        persistFile.writeText(stringRepresentation.toString())
    }

    /**
     * Persists the ground truth of a generation run in a file
     * @param generated the data generated both labels and timestamps
     * @param persistPath the path to write the data to
     */
    private fun persistGroundTruth(generated: List<Pair<DateTime, Int>>, persistPath: String) {
        createFileIfNotExists(persistPath)
        val persistFile = File(persistPath)
        val stringRepresentation = StringBuilder()
        generated.forEachIndexed { i, it ->
            if (i > 0) {
                stringRepresentation.append("${it.first},${it.second}\n")
            }
        }
        persistFile.writeText(stringRepresentation.toString())
    }
}
package org.maspitzner.presencesimulation.simulation.timestamps

import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.LSTM
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import org.maspitzner.presencesimulation.utils.datahandling.MinMaxNormalizer
import org.nd4j.evaluation.regression.RegressionEvaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import kotlin.math.abs
import kotlin.math.roundToLong


/**
 * Class to abstract the functionalities of an LSTM regression model
 * to calculate the idle times between two events.
 * Implements [FitableTimestampGenerator] and [StatefulTimestampGenerator]
 * @see [FitableTimestampGenerator]
 * @see [StatefulTimestampGenerator]
 */
class LSTMTimestampGenerator(override var baseTimestamp: DateTime) :
    FitableTimestampGenerator, StatefulTimestampGenerator {
    private val verbosity: Boolean = false
    private lateinit var model: MultiLayerNetwork
    private var maxDuration = -1L
    private var minDuration = Long.MAX_VALUE

    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param logEntries the data to train the model on
     * @param uniqueDevices the number of unique devices in the data
     */
    override fun fit(logEntries: ArrayList<LogEntry>, uniqueDevices: Int) {
        // determine the window size of entries in to partition the train data
        val windowSize = 10
        //configure the LSTM initializing the hyperparameters
        val inNodes = logEntries[0].getFeatures().size
        val epochs = 1000
        val seed = 123456L
        val learningRate = 0.005
        val layerSize = 256


        val logData = Log(logEntries, uniqueDevices)
        logData.scaleLog()
        // calculate min and max value to make normalization revertable
        val (min, max) = logData.getTimestampMinMax()
        minDuration = min
        maxDuration = max
        //calculate train and test data w.r.t. window size default split is 80:20
        val trainData = logData.get3DTrainData(windowSize, timestampModel = true)
        val testData = logData.get3DTestData(windowSize, timestampModel = true)
        var currentEpoch = 0

        trainData.shuffle(seed)

        initializeLSTM(seed, learningRate, inNodes, layerSize)

        trainLSTM(currentEpoch, epochs, trainData)

        // for debugging and if verbose output is needed an evaluation is performed
        evaluateIfVerbose(testData)

    }

    /**
     * If verbose Information is desired this evaluates the Model with standart metrics.
     * @param testData the data to evuate with
     */
    private fun evaluateIfVerbose(testData: DataSet) {
        if (verbosity) {
            println(this.model.summary())
            val evaluation = RegressionEvaluation()
            val modelOutput = model.output(testData.features)
            evaluation.eval(testData.labels, modelOutput)
            println(evaluation.stats())
        }
    }

    /**
     * Trains the LSTM for the given amound of epochs on the given training data
     * @param currentEpoch the epoche started with (in case of failure)
     * @param epochs the set amount of all epochs
     * @param trainData the dataset to train on
     * */
    private fun trainLSTM(currentEpoch: Int, epochs: Int, trainData: DataSet) {
        for (i in currentEpoch until epochs) {
            model.fit(trainData)
            if (verbosity && i > 0 && i % 10 == 0) {
                println("Training epoch $i/$epochs done!")
            }
        }
    }

    /**
     * initializes the LSTM network with set parameters
     * @param seed a fixed Seed for evaluation purposes
     * @param learningRate the learningrate of the LSTM
     * @param inNodes the input layers nodes
     * @param the size of the hidden LSTM layer in nodes
     */
    private fun initializeLSTM(seed: Long, learningRate: Double, inNodes: Int, layerSize: Int) {
        //configure the LSTM
        val conf = NeuralNetConfiguration.Builder()
            .seed(seed)    //Random number generator seed for improved repeatability. Optional.
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .updater(Adam(learningRate))
            .list()
            .layer(0, LSTM.Builder().activation(Activation.TANH).nIn(inNodes).nOut(layerSize).build())
            .layer(
                1, RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY).nIn(layerSize).nOut(1).build()
            )
            .build()

        // initialize the neural net and train it
        this.model = MultiLayerNetwork(conf)
        model.init()
    }

    /**
     * Trains the model on a list of LogEntries, needs the number of unique devices in the Data
     * @param log the data to train the model on
     */
    override fun fit(log: Log) {
        fit(log.getEntries(), log.deviceCount)

    }

    /**
     * generates a idle time based on a LogEntry
     * @param logEntry the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntry: LogEntry): Long {
        val features = logEntry.getFeatures().toDoubleArray()
        val rawInput = ArrayList<DoubleArray>()
        val inNodes = model.layerInputSize(0)
        //if the features gotten from a log entry don't have the correct length
        // padding or truncating is applied
        when {
            features.size < inNodes -> {
                val paddedFeatures = DoubleArray(inNodes) { 0.0 }
                var j = 0
                for (i in inNodes - features.size until inNodes) {

                    paddedFeatures[i] = features[j++]
                }
                rawInput.add(paddedFeatures)
            }
            features.size > inNodes -> {
                val truncatedArray = features.sliceArray(features.size - inNodes until features.size)
                rawInput.add(truncatedArray)
            }
            else -> {
                rawInput.add(features)
            }
        }
        // generating the model input as a nd4j array
        val modelInput = Nd4j.create(rawInput.toTypedArray())
        // perform a forward pass through the LSTM
        val rawResult = model.rnnTimeStep(modelInput)
        // denormalize the result and return it as IdleTime
        return abs(
            MinMaxNormalizer()
                .revert(minDuration, maxDuration, rawResult.getDouble(0))
                .roundToLong()
        )
    }

    /**
     * generates a idle time based on a device label
     * @param deviceLabel the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(deviceLabel: Int): Long {
        return generateTimestampLength(LogEntry(DateTime.now(), deviceLabel))
    }

    /**
     * generates a idle time based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(logEntries: ArrayList<LogEntry>): Long {
        return logEntries.map { generateTimestampLength(it) }.last()
    }

    /**
     * generates a idle time based on a Log
     * @param log the base for the synthesis of the next idle time
     * @return the idle time
     */
    override fun generateTimestampLength(log: Log): Long {
        return generateTimestampLength(log.getEntries())
    }

    /**
     * generates a timestamp based on a LogEntry
     * @param logEntry the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(logEntry: LogEntry): DateTime {
        return transformDurationToTimestamp(generateTimestampLength(logEntry))
    }

    /**
     * generates a timestamp based on a device label
     * @param deviceLabel the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(deviceLabel: Int): DateTime {
        return transformDurationToTimestamp(generateTimestampLength(deviceLabel))
    }

    /**
     * generates a timestamp based on a list of LogEntries
     * @param logEntries the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(logEntries: ArrayList<LogEntry>): DateTime {
        return transformDurationToTimestamp(generateTimestampLength(logEntries))
    }

    /**
     * generates a timestamp based on a log
     * @param log the base for the synthesis of the next timestamp
     * @return the next timestamp
     */
    override fun generateTimestamp(log: Log): DateTime {
        return generateTimestamp(log.getEntries())
    }

    /**
     * generates a idle time for each device in the given label list
     * @param labelList a list of labels in need of idle times
     * @return a list of idle times
     */
    override fun generateLengthSequence(labelList: ArrayList<Int>): ArrayList<Long> {
        return labelList.map { generateTimestampLength(it) } as ArrayList<Long>
    }

    /**
     * generates a idle time for each LogEntry in the list of LogEntries
     * @param logEntries a list of LogEntries in need of idle times
     * @return a list of idle times
     */
    override fun generateLengthSequence(logEntries: List<LogEntry>): ArrayList<Long> {
        return logEntries.map { generateTimestampLength(it) } as ArrayList<Long>

    }

    /**
     * generates a idle time for each LogEntry in the Log
     * @param log a given Log in need of idle times
     * @return a list of idle times
     */
    override fun generateLengthSequence(log: Log): ArrayList<Long> {
        return generateLengthSequence(log.getEntries())
    }

    /**
     * generates a timestamp for each device in the given label list
     * @param labelList a list of labels in need of timestamps
     * @return a list of timestamps
     */
    override fun generateTimestampSequence(labelList: ArrayList<Int>): ArrayList<DateTime> {
        return generateLengthSequence(labelList).map { transformDurationToTimestamp(it) } as ArrayList<DateTime>
    }

    /**
     * generates a timestamp for each LogEntry in the list of LogEntries
     * @param labelList a list of LogEntries in need of timestamps
     * @return a list of idle times
     */
    override fun generateTimestampSequence(labelList: List<LogEntry>): ArrayList<DateTime> {
        return generateLengthSequence(labelList).map { transformDurationToTimestamp(it) } as ArrayList<DateTime>
    }

    /**
     * generates a timestamp for each LogEntry in the Log
     * @param log a given Log in need of timestamps
     * @return a list of timestamps
     */
    override fun generateTimestampSequence(log: Log): ArrayList<DateTime> {
        return generateLengthSequence(log).map { transformDurationToTimestamp(it) } as ArrayList<DateTime>
    }

}


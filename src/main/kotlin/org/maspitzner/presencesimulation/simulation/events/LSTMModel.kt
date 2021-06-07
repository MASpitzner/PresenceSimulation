package org.maspitzner.presencesimulation.simulation.events

import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.LSTM
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions

/**
 * Class implementing the functionalities to train and use a LSTM Model
 * Implements [Model]
 * @see [Model]
 */
class LSTMModel : Model {
    /* initializing required parameters
       model - the later LSTM
       verbosity - indicating whether to evaluate the underlying model and print verbose information
       inNodes - the number of Nodes in the input Layer
       seed - a fixed seed for evaluation purposes
       learningRate - the learning rate of the underlying LSTM artificial neural net
       layerSize - the number of Nodes in the hidden LSTM-Layer
     */
    private lateinit var model: MultiLayerNetwork
    private var verbosity: Boolean = false
    private var inNodes = 0
    private var epochs = 1000
    private var seed = 123456L
    private var learningRate = 0.005
    private var layerSize = 256


    /**
     * Configures, builds and trains the LSTM artificial neural net
     * @param logEntries the given training data as ArrayList of LogEntries
     * @param uniqueDevices the number of distinct device labels in the given training data
     * @param windowSize the number of LogEntries considered in every training and test example
     */
    private fun build(logEntries: ArrayList<LogEntry>, uniqueDevices: Int, windowSize: Int = 10) {

        //creates the train and test data from the given log
        val trainData = Log(logEntries).get3DTrainData(windowSize)
        val testData = Log(logEntries).get3DTestData(windowSize)

        // sets the given hyperparameters of the lstm model
        setParameters(logEntries[0].getFeatures().size, 1000, 0.005, 256, false)
        trainData.shuffle(seed)

        // configures the artificial neural net
        configureLSTM(uniqueDevices)

        // trains the artificial neural net with the given training data
        trainLSTMModel(trainData)

        //if wanted prints verbose statistics
        evaluateIfVerbose(uniqueDevices, testData)


    }

    /**
     * Trains the LSTM-Model with the provided training data
     * If verbose output is desired it prints the current Epoch
     * @param trainData the provided train data
     */
    private fun trainLSTMModel(trainData: DataSet) {
        for (i in 0 until epochs) {
            model.fit(trainData)
            if (i > 0 && i % 10 == 0 && verbosity) {
                println("Trainingsepoche $i/$epochs done!")
            }
        }
    }

    /**
     * Evaluates the trained LSTM-Model with standart metrics if verbose information is desired.
     * @param uniqueDevices the number of devices in the dataset
     * @param testData the test data for the evaluation.
     */
    private fun evaluateIfVerbose(uniqueDevices: Int, testData: DataSet) {
        if (verbosity) {
            println(this.model.summary())
            val evaluation = Evaluation(uniqueDevices)
            val modelOutput = model.output(testData.features)
            evaluation.eval(testData.labels, modelOutput)
            println(evaluation.stats(false, true))
        }
    }

    /**
     * Configures and initializes the LSTM with specific Hyperparameters
     * @param uniqueDevices the number of unique devices in the trainings dataset
     */
    private fun configureLSTM(uniqueDevices: Int) {
        val conf = NeuralNetConfiguration.Builder()
            .seed(seed)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .updater(Adam(learningRate))
            .list()
            .layer(
                0,
                LSTM.Builder().activation(Activation.TANH).weightInit(WeightInit.XAVIER).nIn(inNodes).nOut(layerSize)
                    .build()
            )
            .layer(
                1, RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT).weightInit(WeightInit.XAVIER)
                    .activation(Activation.SOFTMAX).nIn(layerSize).nOut(uniqueDevices).build()
            )
            .build()

        // initializes the neural net
        this.model = MultiLayerNetwork(conf)
        model.init()
    }

    /**
     * Sets needed hyperparameters for the lstm model.
     * @param inNodes the number of nodes in the input layer
     * @param epochs the number of training iterations to perform
     * @param learningRate the learning rate of the underlying artificial neural net
     * @param layerSize the size of the hidden lstm layer
     * @param verbosity boolean indicating whether or not to evaluate the artificial neural net performance
     * and print verbose stats
     */
    fun setParameters(inNodes: Int, epochs: Int, learningRate: Double, layerSize: Int, verbosity: Boolean) {
        this.inNodes = inNodes
        this.epochs = epochs
        this.learningRate = learningRate
        this.layerSize = layerSize
        this.verbosity = verbosity
    }

    /**
     * Fits the given data as log entries and unique devices.
     * @param logEntries a arraylist of LogEntries to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * @param uniqueDevices the number of distinct device labels in the given dataset
     */
    override fun fit(logEntries: ArrayList<LogEntry>, acyclic: Boolean, uniqueDevices: Int) {
        build(logEntries, uniqueDevices)
    }

    /**
     * Fits the given data as log entries and unique devices.
     * @param log a Log to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * (default is false)
     */
    override fun fit(log: Log, acyclic: Boolean) {
        build(log.getEntries(), log.deviceCount)
    }

    /**
     * Predicts a successor label based on a given LogEntry
     * @param logEntry the LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntry: LogEntry): Int {
        val rawResult = model.rnnTimeStep(Log(arrayListOf(logEntry)).get3DTrainCompleteLog(false).features)
        return Nd4j.argMax(rawResult, 1).getInt(0)

    }

    /**
     * Predicts a successor label based on a set of given LogEntry
     * @param logEntries the set of given LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntries: ArrayList<LogEntry>): Int {
        val log = Log(logEntries).get3DTrainCompleteLog()
        val rawResult = this.model.rnnTimeStep(log.features)
        return Nd4j.argMax(rawResult, 1).getInt(0)
    }

    /**
     * Predicts a successor label based on a given log
     * @param log the log whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(log: Log): Int {
        val rawResult = this.model.rnnTimeStep(log.get3DTrainCompleteLog().features)
        return Nd4j.argMax(rawResult, 1).getInt(0)
    }

}
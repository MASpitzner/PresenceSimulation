package org.maspitzner.presencesimulation.simulation.events

import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.BackpropType
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.joda.time.DateTime
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.models.LogEntry
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Sgd
import org.nd4j.linalg.lossfunctions.LossFunctions
import kotlin.random.Random

/**
 * Class abstracting the training and prediction of a Multilayer Perceptron for label generation.
 * Prediction is done essentially by considering a set of previous log entries as a List of features.
 * Model can be randomized, but for evaluation purposes it's by default using a fixed seed
 * @param randomized boolean indicating whether to use a random number generator
 * @param seed possible seed for the random number generator
 * Implements [Model]
 * @see [Model]
 */

class MLPModel(randomized: Boolean = false, seed: Long = 123456) : Model {
    //initialize needed values
    private var inNodes: Int = 0
    private var outNodes: Int = 0
    private var layerNodes: ArrayList<Int> = ArrayList()
    private var epochs: Int = 0
    private var learningRate: Double = 0.0
    private var verbosity: Boolean = false
    private lateinit var model: MultiLayerNetwork

    // initializing a possible random number generator
    private val randomGenerator: Random? = if (randomized) {
        if (seed > 0) Random(seed) else Random(DateTime.now().millis)
    } else null


    /**
     * Builds and initializes a Multilayer Perceptron
     *
     */
    private fun buildModel(): MultiLayerNetwork {
        var conf = NeuralNetConfiguration.Builder()
            .seed(randomGenerator?.nextLong() ?: 123456)
            .weightInit(WeightInit.XAVIER)
            .activation(Activation.RELU)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(Sgd(learningRate))
            .list()
        conf = addLayers(conf)
        conf.backpropType(BackpropType.Standard)
        val modelConfig = conf.build()
        val model = MultiLayerNetwork(modelConfig)
        model.init()
        return model
    }

    /**
     * Adds layers as given to a Multilayer Perceptron
     * @param builder a dl4j NeuralNetConfiguration.ListBuilder needed to configure a Multilayer Perceptron
     * @return NeuralNetConfiguration.ListBuilder with configured layers
     */
    private fun addLayers(builder: NeuralNetConfiguration.ListBuilder): NeuralNetConfiguration.ListBuilder {
        var previousLayerNodes = inNodes

        layerNodes.forEachIndexed { i, it ->
            builder.layer(
                i,
                DenseLayer.Builder().nIn(previousLayerNodes).nOut(it).weightInit(WeightInit.XAVIER)
                    .activation(Activation.RELU).build()
            )
            previousLayerNodes = it
        }
        builder.layer(
            layerNodes.size,
            OutputLayer.Builder().nIn(previousLayerNodes).nOut(this.outNodes).weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT)
                .build()
        )

        return builder
    }

    /**
     * Evaluation method for debugging and verbosity issues
     * Evaluates and prints the MLP performance
     * @param testDataSet the dataset to evaluate against
     * @param uniqueDevices
     */
    private fun evaluate(testDataSet: DataSet, uniqueDevices: Int) {
        val evaluation = Evaluation(uniqueDevices)
        val modelOutput = model.output(testDataSet.features)

        evaluation.eval(testDataSet.labels, modelOutput)
        println(evaluation.stats(false, true))
    }


    /**
     * Builds an trains the MLP on given LogEntry training data
     * @param logEntries the given training data as ArrayList of LogEntries
     * @param uniqueDevices the number of distinct labels in the training data
     */
    private fun build(logEntries: ArrayList<LogEntry>, uniqueDevices: Int) {
        val windowLength = 10
        val log = Log(logEntries, uniqueDevices)
        this.inNodes = windowLength * log[0].getFeatures().size

        val trainData = log.get2DTrainData(windowLength)
        val testData = log.get2DTestData(windowLength)
        trainData.shuffle(123456)

        val firstLayerNodes = 1024
        setParameter(
            arrayListOf(
                firstLayerNodes, firstLayerNodes / 2, firstLayerNodes / 4
            ), 1000, 0.005, false
        )

        this.model = buildModel()
        for (i in 0 until epochs) {
            model.fit(trainData)
            if (i > 0 && i % 10 == 0 && verbosity) {
                println("Training epoch $i/$epochs done!")
            }
        }
        if (verbosity) {
            println(this.model.summary())
            evaluate(testData, uniqueDevices)
        }

    }

    /**
     * Sets the parameters for the MLP
     * @param layerNodes A Arraylist of integer, describing how many hidden layers with how much nodes
     * e.g. {100,50,10} would build a MLP with 3 hidden layers with 100, 50 and 10 nodes
     * @param epochs the number of training iterations to perform
     * @param learningRate the learning rate for the neural net
     * @param evaluate whether to perform a evaluation of the underlying mlp and print verbose information
     *
     */
    fun setParameter(
        layerNodes: ArrayList<Int>,
        epochs: Int,
        learningRate: Double,
        evaluate: Boolean
    ) {
        this.layerNodes = layerNodes
        this.epochs = epochs
        this.learningRate = learningRate
        this.verbosity = evaluate
    }

    /**
     * Fits the given data as log entries and unique devices.
     * @param logEntries a arraylist of LogEntries to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * @param uniqueDevices the number of distinct device labels in the given dataset
     */
    override fun fit(logEntries: ArrayList<LogEntry>, acyclic: Boolean, uniqueDevices: Int) {
        this.outNodes = uniqueDevices
        build(logEntries, uniqueDevices)
    }

    /**
     * Fits the given data as log entries and unique devices.
     * @param log a Log to fit
     * @param acyclic boolean indicating whether backwards edges in Markov-Chain based models are allowed
     * (default is false)
     */
    override fun fit(log: Log, acyclic: Boolean) {
        fit(log.getEntries(), acyclic, log.deviceCount)
    }

    /**
     * Predicts a successor label based on a given LogEntry
     * @param logEntry the LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntry: LogEntry): Int {
        val features = logEntry.getFeatures().toDoubleArray()
        val rawInput = ArrayList<DoubleArray>()
        // padding or truncate features if needed
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
        val modelInput = Nd4j.create(rawInput.toTypedArray())
        val rawResult = model.predict(modelInput)
        return rawResult?.get(0) ?: 0

    }

    /**
     * Predicts a successor label based on a set of given LogEntry
     * @param logEntries the set of given LogEntry whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(logEntries: ArrayList<LogEntry>): Int {
        val rawInput = ArrayList<DoubleArray>()
        // flattening the array so that it can be used as input for the mlp
        when {
            logEntries.size * logEntries[0].getFeatures().size > inNodes -> {
                val sublist = logEntries.subList(
                    (logEntries.size - (inNodes / logEntries[0].getFeatures().size)), logEntries.size
                )
                val flatArray = sublist.flatMap { it.getFeatures() }.toDoubleArray()
                rawInput.add(flatArray)

            }
            // padding or truncate features if needed

            logEntries.size * logEntries[0].getFeatures().size < inNodes -> {
                val paddedInput = ArrayList<Double>()
                while (paddedInput.size < inNodes - (logEntries.size * logEntries[0].getFeatures().size)) {
                    repeat(logEntries[0].getFeatures().size) { paddedInput.add(0.0) }
                }
                logEntries.forEach { entry -> entry.getFeatures().forEach { paddedInput.add(it) } }
                rawInput.add(paddedInput.toDoubleArray())
            }

            else -> {
                val input = logEntries.flatMap { it.getFeatures() }
                rawInput.add(input.toDoubleArray())
            }

        }
        val modelInput = Nd4j.create(rawInput.toTypedArray())
        val rawResult = model.predict(modelInput)
        return rawResult?.get(0) ?: 0
    }

    /**
     * Predicts a successor label based on a given log
     * @param log the log whose successor label is to predict
     * @return the successor label calculated
     */
    override fun predict(log: Log): Int {
        return predict(log.getEntries())
    }

}
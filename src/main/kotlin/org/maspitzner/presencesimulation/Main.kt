package org.maspitzner.presencesimulation

import org.maspitzner.presencesimulation.evaluation.EvaluationDataGenerator
import org.maspitzner.presencesimulation.evaluation.LabelEvaluator
import org.maspitzner.presencesimulation.evaluation.RuntimeEvaluator
import org.maspitzner.presencesimulation.evaluation.TimestampEvaluator
import org.maspitzner.presencesimulation.executors.simulators.OpenHABSimulator
import org.maspitzner.presencesimulation.parsers.CommandLineConfigParser
import kotlin.system.measureTimeMillis

/**
 * Driver-Class to run the presence simulation.
 */

class Main {

    companion object {


        /**
         * Main method:
         * -extracts the configuration parameters from the command line
         * -invokes a simulator and calls its run functionalities
         * -alternatively invokes a evaluator and calls its evaluation functionalities
         * -measures the time and resource usage
         */
        @JvmStatic
        fun main(args: Array<String>) {

            val totalTime = measureTimeMillis {
                //extracts configuration parameters from the command line
                val config = CommandLineConfigParser(args).config


                //checks whether the current run is an evaluation run or a simulation run
                if (!config.evaluation) {
                    /*case simulation run:
                     * invokes a simulator and calls its run functionality with regard to console or file output
                     * measures the simulation time and computes the time per simulated entry
                     */
                    val simulator = OpenHABSimulator(config)
                    if (!config.fileOutput) {
                        val simTime = measureTimeMillis {
                            simulator.runConsoleSimulation()
                        }
                        println(
                            "Simulation took ${simTime}ms average of ${simTime / 100}ms/entry"
                        )
                    } else {
                        simulator.runLogSimulation()
                    }
                } else {
                    when {
                        /*case evaluation run:
                         * invokes a evaluator and calls its evaluation functionalities
                         * is able to evaluate a single model or multiple models for a set amount of runs
                         * evaluates the generated data based on the metrics presented in the thesis
                         */
                        config.timeEval -> {
                            RuntimeEvaluator(config).evaluateModelRuntime(config.evalIterations)
                        }
                        else -> {
                            EvaluationDataGenerator(config).runGeneration(config.evalIterations, 0)
                            val groundTruthPath = "${config.evaluationPath}groundTruth"
                            val syntheticPath = "${config.evaluationPath}timestampGenerators"
                            if (!config.evaluationSingle) {
                                TimestampEvaluator().evaluateAllModelTypes(syntheticPath, groundTruthPath)
                            } else {
                                TimestampEvaluator().evaluateModelType(
                                    config.timestampModelType,
                                    syntheticPath,
                                    groundTruthPath
                                )
                            }
                            val labelEvaluator = LabelEvaluator(config)
                            labelEvaluator.run()

                        }
                    }
                }
            }
            /*
            prints out resource usage for more insights
             */
            println("$totalTime ms needed")
            println(
                "${
                    (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
                        .freeMemory()) / (1024 * 1024)
                }MiByte ram usage"
            )
        }

    }
}
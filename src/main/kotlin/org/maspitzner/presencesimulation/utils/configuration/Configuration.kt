package org.maspitzner.presencesimulation.utils.configuration

import org.joda.time.DateTime

/**
 * data class to represent the commandline arguments
 * @param logProvider the type of LogProvider to use
 * @param weatherDataProvider the type of WeatherDataProvide to use
 * @param eventModelType the EventModel to use
 * @param timestampModelType the TimestampModel to use
 * @param logPath the path to the original log
 * @param weatherDataPath the path to the weather data to use
 * @param evaluation boolean indicating whether to run a evaluation run or not
 * @param evaluationSingle boolean indicating whether to evaluate a single model or all
 * @param evaluationPath path to which the results of an evaluation should be written. Default is ./simout/evaluation/
 * @param fileOutput boolean indicating whether to write the results of a simulation to a file
 * @param outputPath path to which the results of a simulation should be written
 * @param evalIterations number of iterations to run in a simulation
 * @param timeEval boolean indicating whether an evaluation run is for time measurement or not
 * @param evalReal boolean indicating whether an evaluation run uses a given log or should generate random logs
 */
data class Configuration(
    val logProvider: LogProviderTypes,
    val weatherDataProvider: WeatherDataProviderTypes,
    val eventModelType: EventModelTypes,
    val timestampModelType: TimestampModelTypes,
    val logPath: String,
    val weatherDataPath: String,
    val evaluation: Boolean,
    val evaluationSingle: Boolean,
    val evaluationPath: String,
    val fileOutput: Boolean,
    val outputPath: String,
    val evalIterations: Int,
    val timeEval: Boolean,
    val evalReal: Boolean,
    val numOfEvents: Int,
    val untilTime: DateTime?

) {
    /**
     * Overwrite of toString() to enable pretty printing
     */
    override fun toString(): String {
        return with(StringBuilder()) {
            this.append("logProvider=$logProvider\n")
            this.append("weatherDataProvider=$weatherDataProvider\n")
            this.append("eventModelType=$eventModelType\n")
            this.append("timestampModelType=$timestampModelType\n")
            this.append("logPath=$logPath\n")
            this.append("weatherDataPath=$weatherDataPath\n")
            this.append("evaluation=$evaluation\n")
            this.append("evaluationSingle=$evaluationSingle\n")
            this.append("fileOutput=$fileOutput\n")
            this.append("outputPath=$outputPath\n")
            this.append("evaluationPath=${evaluationPath}\n")
            this.append("evalIterations=$evalIterations\n")
            this.append("realEval=${evalReal}\n")
            this.append("timeEval=$timeEval\n")
            this.append("numberOfEvents=${numOfEvents}\n")
            this.append("untilTime=${untilTime}")
        }.toString()
    }
}

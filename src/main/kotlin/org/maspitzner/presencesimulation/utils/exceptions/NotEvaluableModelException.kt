package org.maspitzner.presencesimulation.utils.exceptions

/**
 * Custom exception to indicate a wrongly selected model for an evaluation
 */
class NotEvaluableModelException : Throwable() {
    override val message: String
        get() = "The selected Model isn't an evaluable Model\n" +
                "(choose MKM,CMKM,MLPM,LSTMM as EventModel or UTSM, PTSM, CPTSM, TFTSM or LSTMTSM as TimestampModel)"
}
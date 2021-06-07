package org.maspitzner.presencesimulation.utils.exceptions

class MissingParameterException(private val missingParameter: String) : Throwable() {
    override val message: String
        get() = "Missing non-optional parameter: $missingParameter"
}
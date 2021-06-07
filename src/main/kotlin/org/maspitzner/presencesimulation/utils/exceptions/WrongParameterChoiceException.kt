package org.maspitzner.presencesimulation.utils.exceptions

class WrongParameterChoiceException(private val Parameter: String, private val helpString: String) : Throwable() {
    override val message: String
        get() = "Wrong parameter choice: $Parameter ${if (helpString.isNotEmpty()) "\n" else ""} $helpString"
}

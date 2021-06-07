package org.maspitzner.presencesimulation.utils.exceptions


/**
 * Custom exception thrown if the log file is not readable or in a wrong format or simply not existing.
 */
class IllegalLogFilePathException(private val path: String) : Throwable() {
    override val message: String
        get() = "Invalid path to log file/s or missing permissions ($path)"
}

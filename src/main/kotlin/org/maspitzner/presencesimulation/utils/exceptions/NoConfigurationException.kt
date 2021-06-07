package org.maspitzner.presencesimulation.utils.exceptions

class NoConfigurationException : Throwable() {
    override val message: String
        get() = "No configuration parameter chosen. See Readme for how to call this Program."
}
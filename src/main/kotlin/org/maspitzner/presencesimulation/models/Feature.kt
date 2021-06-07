package org.maspitzner.presencesimulation.models

/**
 * enum class representing the different feature types to train a model
 * values can be [TIME_OF_DAY], [DAY_OF_YEAR], [REAL_TEMPERATURE], [FELT_TEMPERATURE], [CLOUDINESS], [HUMIDITY], [PRESSURE], [WIND_SPEED]
 * if other features are implemented, they need also be added here
 */

enum class Feature {

    TIME_OF_DAY, DAY_OF_YEAR, REAL_TEMPERATURE, FELT_TEMPERATURE, CLOUDINESS, HUMIDITY, PRESSURE, WIND_SPEED

}
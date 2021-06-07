package org.maspitzner.presencesimulation.utils.configuration


/**
 * Enum class to represent and determine different TimestampModels
 * Values can be [MOCK_TIMESTAMP_MODEL],[UTSM],[PTSM],[CPTSM],[TFTSM],[LSTMTSM],[EVAL]
 * When new types are added to the prototype the need to be included here
 */
enum class TimestampModelTypes {
    MOCK_TIMESTAMP_MODEL, UTSM, PTSM, CPTSM, TFTSM, LSTMTSM, EVAL
}
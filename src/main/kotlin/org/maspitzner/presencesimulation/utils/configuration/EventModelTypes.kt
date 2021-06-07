package org.maspitzner.presencesimulation.utils.configuration


/**
 * Enum class to represent and determine different LabelModels
 * Values can be [MOCK_EVENT_MODEL], [MKM], [CMKM], [MLPM], [LSTMM], [EVAL]
 * When new types are added to the prototype the need to be included here
 */
enum class EventModelTypes {
    MOCK_EVENT_MODEL, MKM, CMKM, MLPM, LSTMM, EVAL
}
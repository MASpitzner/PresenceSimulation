package org.maspitzner.presencesimulation.utils.configuration

/**
 * Enum class to represent and determine different LogProvider
 * Values can be [MOCK_PROVIDER],[OPENHAB_PROVIDER]
 * When new types are added to the prototype the need to be included here
 */
enum class LogProviderTypes {
    MOCK_PROVIDER, OPENHAB_PROVIDER
}
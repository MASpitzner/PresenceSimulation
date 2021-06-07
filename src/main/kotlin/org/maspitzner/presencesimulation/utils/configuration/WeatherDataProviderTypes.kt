package org.maspitzner.presencesimulation.utils.configuration

/**
 * Enum class to represent and determine different WeatherDataProvider
 * Values can be [MOCK_PROVIDER], [OPEN_WEATHER_MAP_PROVIDER]
 * When new types are added to the prototype the need to be included here
 */
enum class WeatherDataProviderTypes {
    MOCK_PROVIDER, OPEN_WEATHER_MAP_PROVIDER
}
package org.maspitzner.presencesimulation.models

import org.joda.time.DateTime

/**
 * Represents a log data point.
 * @param timeStamp Timestamp at which the given event happened.
 * @param selectedDevice Representing the label of a device in the log.
 * @param weekend Represents weather its weekend @timeStamp.
 * @param realTemperature The average temperature.
 * @param feltTemperature The felt temperature.
 * @param pressure The air pressure.
 * @param humidity The air humidity.
 * @param windSpeed The speed of wind.
 * @param cloudiness The percentage of cloudiness.
 * @param weather A categorical string which describes the current weather state.
 */
data class LogEntry(
    val timeStamp: DateTime,
    val selectedDevice: Int,
    val weekend: Double = if (timeStamp.dayOfWeek == 6 || timeStamp.dayOfWeek == 7) 1.0 else 0.0,
    val timeOfDay: Double = timeStamp.minuteOfDay.toDouble(),
    var timeOfDayScaled: Double = 0.0,
    val dayOfYear: Double = timeStamp.dayOfYear.toDouble(),
    var dayOfYearScaled: Double = 0.0,
    var realTemperature: Double = 0.0,
    var realTemperatureScaled: Double = 0.0,
    var feltTemperature: Double = 0.0,
    var feltTemperatureScaled: Double = 0.0,
    var pressure: Int = 0,
    var pressureScaled: Double = 0.0,
    var humidity: Double = 0.0,
    var humidityScaled: Double = 0.0,
    var windSpeed: Double = 0.0,
    var windSpeedScaled: Double = 0.0,
    var cloudiness: Double = 0.0,
    var cloudinessScaled: Double = 0.0,
    var weather: String = "",
    var weatherEncoding: Array<Double> = Array(0) { 0.0 }
) {
    /**
     * Sets the weather information for this LogEntry based on the given WeatherDataEntry.
     * @param weatherData The weather information
     * which where the current ones at the specific time stamp.
     */
    fun setWeather(weatherData: WeatherDataEntry) {
        realTemperature = weatherData.physicalWeatherData.temp
        feltTemperature = weatherData.physicalWeatherData.feels_like
        pressure = weatherData.physicalWeatherData.pressure
        humidity = weatherData.physicalWeatherData.humidity
        windSpeed = weatherData.wind?.speed ?: 0.0
        cloudiness = weatherData.clouds?.all ?: 0.0
        weather = weatherData.basicWeatherData[0].main
        copyDataToScaled()
    }

    /**
     * sets scaled features initially to the original values
     */
    private fun copyDataToScaled() {
        realTemperatureScaled = realTemperature
        feltTemperatureScaled = feltTemperature
        pressureScaled = pressure.toDouble()
        humidityScaled = humidity
        windSpeedScaled = windSpeed
        cloudinessScaled = cloudiness
    }

    /**
     * sets the scaled features for every feature type
     * @param features scaled features as map of type to value
     */
    fun setScaledFeatures(features: HashMap<Feature, Double>) {
        timeOfDayScaled = features[Feature.TIME_OF_DAY] ?: 0.0
        dayOfYearScaled = features[Feature.DAY_OF_YEAR] ?: 0.0
        realTemperatureScaled = features[Feature.REAL_TEMPERATURE] ?: 0.0
        feltTemperatureScaled = features[Feature.FELT_TEMPERATURE] ?: 0.0
        pressureScaled = features[Feature.PRESSURE] ?: 0.0
        humidityScaled = features[Feature.HUMIDITY] ?: 0.0
        windSpeedScaled = features[Feature.WIND_SPEED] ?: 0.0
        cloudinessScaled = features[Feature.CLOUDINESS] ?: 0.0
    }

    /**
     * returns the scaled features of a LogEntry
     * @return the scaled features as ArrayList<Double>
     */
    fun getFeatures(): ArrayList<Double> {
        return arrayListOf(
            timeOfDayScaled,
            dayOfYearScaled,
            realTemperatureScaled,
            feltTemperatureScaled,
            pressureScaled,
            humidityScaled,
            windSpeedScaled,
            cloudinessScaled,
            *weatherEncoding
        )
    }

    /**
     * overwrite of equals() needed since there are object types included
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LogEntry

        if (timeStamp != other.timeStamp) return false
        if (selectedDevice != other.selectedDevice) return false
        if (weekend != other.weekend) return false
        if (timeOfDay != other.timeOfDay) return false
        if (timeOfDayScaled != other.timeOfDayScaled) return false
        if (dayOfYear != other.dayOfYear) return false
        if (dayOfYearScaled != other.dayOfYearScaled) return false
        if (realTemperature != other.realTemperature) return false
        if (realTemperatureScaled != other.realTemperatureScaled) return false
        if (feltTemperature != other.feltTemperature) return false
        if (feltTemperatureScaled != other.feltTemperatureScaled) return false
        if (pressure != other.pressure) return false
        if (pressureScaled != other.pressureScaled) return false
        if (humidity != other.humidity) return false
        if (humidityScaled != other.humidityScaled) return false
        if (windSpeed != other.windSpeed) return false
        if (windSpeedScaled != other.windSpeedScaled) return false
        if (cloudiness != other.cloudiness) return false
        if (cloudinessScaled != other.cloudinessScaled) return false
        if (weather != other.weather) return false
        if (!weatherEncoding.contentEquals(other.weatherEncoding)) return false

        return true
    }

    /**
     * overwrite of hashCode() needed since there are object types included
     */
    override fun hashCode(): Int {
        var result = timeStamp.hashCode()
        result = 31 * result + selectedDevice
        result = 31 * result + weekend.hashCode()
        result = 31 * result + timeOfDay.hashCode()
        result = 31 * result + timeOfDayScaled.hashCode()
        result = 31 * result + dayOfYear.hashCode()
        result = 31 * result + dayOfYearScaled.hashCode()
        result = 31 * result + realTemperature.hashCode()
        result = 31 * result + realTemperatureScaled.hashCode()
        result = 31 * result + feltTemperature.hashCode()
        result = 31 * result + feltTemperatureScaled.hashCode()
        result = 31 * result + pressure
        result = 31 * result + pressureScaled.hashCode()
        result = 31 * result + humidity.hashCode()
        result = 31 * result + humidityScaled.hashCode()
        result = 31 * result + windSpeed.hashCode()
        result = 31 * result + windSpeedScaled.hashCode()
        result = 31 * result + cloudiness.hashCode()
        result = 31 * result + cloudinessScaled.hashCode()
        result = 31 * result + weather.hashCode()
        result = 31 * result + weatherEncoding.contentHashCode()
        return result
    }

    /**
     * returns a csv representation of the LogEntry
     * @param separator separator char for csv format
     * @return csv-string representation of this LogEntry
     */
    fun getCSVString(separator: Char): String {
        val result = StringBuilder()
        result.append("$timeStamp$separator")
        result.append("$selectedDevice$separator")
        result.append("$weekend$separator")
        result.append("$timeOfDay$separator")
        result.append("$timeOfDayScaled$separator")
        result.append("$dayOfYear$separator")
        result.append("$dayOfYearScaled$separator")
        result.append("$realTemperature$separator")
        result.append("$realTemperatureScaled$separator")
        result.append("$feltTemperature$separator")
        result.append("$feltTemperatureScaled$separator")
        result.append("$pressure$separator")
        result.append("$pressureScaled$separator")
        result.append("$humidity$separator")
        result.append("$humidityScaled$separator")
        result.append("$windSpeed$separator")
        result.append("$windSpeedScaled$separator")
        result.append("$cloudiness$separator")
        result.append("$cloudinessScaled$separator")
        result.append("$weather$separator")
        return result.toString()
    }

}
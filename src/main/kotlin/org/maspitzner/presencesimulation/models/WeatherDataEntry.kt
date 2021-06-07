package org.maspitzner.presencesimulation.models

/**
 * Classes needed for parsing OpenWeatherMap Historical Weather Data Bulk
 * with Gson.
 */


import com.google.gson.annotations.SerializedName


/**
 * A entry in a weather data bulk
 * @param city Place of weather record as string.
 * @param lat Geo position latitude.
 * @param lon Geo position longitude.
 * @param physicalWeatherData Temperature, pressure and humidity.
 * @param wind Wind information speed and direction.
 * @param rain Rain forecast 1h and 3h.
 * @param snow Snow forecast 1h and 3h.
 * @param clouds Cloudiness in %.
 * @param basicWeatherData A summary for the current weather.
 * @param dayTimeInMilliSec The time in milliseconds.
 * @param dayTimeIso The time in an iso format UTC.
 * @param timezone Information to the current timezone.
 */
data class WeatherDataEntry(
    @SerializedName("city_name") val city: String = "",
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lon") val lon: Double = 0.0,
    @SerializedName("main") val physicalWeatherData: PhysicalWeatherData,
    @SerializedName("wind") val wind: Wind?,
    @SerializedName("rain") val rain: Rain?,
    @SerializedName("snow") val snow: Snow?,
    @SerializedName("clouds") val clouds: Clouds?,
    @SerializedName("weather") var basicWeatherData: List<BasicWeatherData>,
    @SerializedName("dt") val dayTimeInMilliSec: Long?,
    @SerializedName("dt_iso") val dayTimeIso: String?,
    @SerializedName("timezone") val timezone: Int?

) {

    /**
     * Pretty prints a WeatherDataEntry Object.
     * @return String representation of the object.
     */
    override fun toString() = with(StringBuilder()) {
        append("WeatherDataEntry\n")
        append("${city}\n")
        append("$physicalWeatherData")
        append(wind?.toString() ?: "")
        append(rain?.toString() ?: "")
        append(snow?.toString() ?: "")
        append(clouds?.toString() ?: "")
        basicWeatherData.forEach { append(it.toString()) }
        append("$dayTimeIso")
    }.toString()

    /**
     * Transforms weather data to metric units.
     */
    fun transform() {
        physicalWeatherData.transform()
        clouds?.transform()
    }

    fun getJodaTimeFormat() = dayTimeIso
        ?.split(" ")
        ?.mapIndexed { index, token ->
            when {
                index == 0 -> {
                    "${token}T"
                }
                token.contains("UTC") -> {
                    token.replace("UTC", "")
                }
                else -> token
            }
        }
        ?.joinToString(separator = "")


}

/**
 * Weather information which represents temperature, pressure and humidity.
 *@param temp Mean temperature without transform K else degree celsius.
 *@param temp_min Minimum temperature without transform K else degree celsius.
 *@param temp_max Maximum temperature without transform K else degree celsius.
 *@param feels_like Felt temperature without transform K else degree celsius.
 *@param pressure Air pressure in hPa.
 *@param humidity Humidity in percent.
 */

data class PhysicalWeatherData(
    var temp: Double,
    var temp_min: Double,
    var temp_max: Double,
    var feels_like: Double,
    val pressure: Int,
    var humidity: Double
) {
    /**
     * Transforms the given data into deg celsius or a value between 0 and 1.
     */
    fun transform() {
        temp -= 273.15
        temp_min -= 273.15
        temp_max -= 273.15
        feels_like -= 273.15
        humidity /= 100.0
    }

    /**
     * Pretty prints a PhysicalWeatherData Object.
     * @return String representation of the object.
     */
    override fun toString() = with(StringBuilder()) {
        append("\tClouds\n")
        append("\t\tav. temp=$temp degrees\n")
        append("\t\tmin temp=$temp_min degrees\n")
        append("\t\tmax temp=$temp_max degrees\n")
        append("\t\tfelt temp=$feels_like degrees\n")
        append("\t\tair pressure=$pressure hPa\n")
        append("\t\thumidity=$humidity %\n")
    }.toString()
}

/**
 * Wind information represented by:
 * @param speed Wind speed in m/s.
 * @param deg Wind directions in degree.
 */
data class Wind(
    val speed: Double,
    val deg: Int
) {
    /**
     * Pretty prints a Wind Object,
     * @return String representation of the object.
     */
    override fun toString() = with(StringBuilder()) {
        append("\tClouds\n")
        append("\t\tspeed=$speed m/sec\n")
        append("\t\tdirection=$deg degrees\n")
    }.toString()
}

/**
 * Cloud information.
 * @param all Cloudiness in percent.
 */
data class Clouds(
    var all: Double

) {
    /**
     * Pretty prints a Clouds Object.
     * @return String representation of the object.
     */
    override fun toString() = with(StringBuilder()) {
        append("\tClouds\n")
        append("\t\tCloudiness=$all %\n")
    }.toString()

    /**
     * Transforms the data into a value between 0 and 1.
     */
    fun transform() {
        all /= 100.0
    }
}

/**
 * Rain information consists of:
 * @param threeHours Three hour forecast.
 * @param oneHour One hour forecast.
 */
data class Rain(
    @SerializedName("3h") val threeHours: Double,
    @SerializedName("1h") val oneHour: Double
) {
    /**
     * Pretty prints a Rain Object.
     * @return String representation of the object.
     */
    override fun toString() = with(StringBuilder()) {
        append("\tRain\n")
        append("\t\t1h=$oneHour\n")
        append("\t\t3h$threeHours\n")
    }.toString()
}

/**
 * Snow information consists of:
 * @param threeHours Three hour forecast.
 * @param oneHour One hour forecast.
 */

data class Snow(
    @SerializedName("3h") val threeHours: Double,
    @SerializedName("1h") val oneHour: Double
) {
    /**
     * Pretty prints a Snow Object.
     * @return String representation of the object.
     */
    override fun toString() = with(StringBuilder()) {
        append("\tSnow\n")
        append("\t\t1h=$oneHour\n")
        append("\t\t3h=$threeHours\n")
    }.toString()
}

/**
 * Weather summary consists of:
 * @param id Weather type id.
 * @param main One word describing weather type.
 * @param description Small summary of weather.
 * @param icon Icon id.
 */

data class BasicWeatherData(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String

) {
    /**
     * Pretty prints a BasicWeatherData Object.
     * @return String representation of the object.
     */
    override fun toString() = with(StringBuilder()) {
        append("\tWeatherInfo\n")
        append("\t\tstate=$main\n")
        append("\t\tdescription=$description\n")
    }.toString()
}



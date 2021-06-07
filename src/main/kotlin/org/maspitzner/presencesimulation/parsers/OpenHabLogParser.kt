package org.maspitzner.presencesimulation.parsers

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.maspitzner.presencesimulation.models.Log
import org.maspitzner.presencesimulation.utils.exceptions.IllegalLogFilePathException
import java.io.File

/**
 * Implements functionality needed to parse a openhab2 log filter it for relevant information.
 */
class OpenHabLogParser private constructor() {
    /**
     * All functionality can be implemented statically, no instantiation needed.
     */
    companion object {
        /**
         * Reads all log files found at the path into a Log Object.
         *
         * @param path Path to the log directory/file.
         * @param tokens Tokens to look for as device names.
         *
         * @return A log object consisting of all relevant log information
         * sorted by their timestamp.
         */
        fun parse(path: String, tokens: ArrayList<String> = ArrayList()): Log {
            val log = Log()
            try {

                val stringLog = retrieveLogString(path, tokens)
                stringLog.forEach { entry -> log.insertEntry(getTimestamp(entry), getDeviceName(entry, tokens)) }
            } catch (e: IllegalLogFilePathException) {
                throw e
            }
            log.sortByTimestamp()
            return log
        }

        /**
         *  Reads a directory or file of log entries
         *  and retrieves all entries which contain one of the name tokens.
         *
         * @param path Path to the log directory/file.
         * @param tokens Tokens to look for as device names.
         *
         * @return The content of the files as list of strings.
         */
        private fun retrieveLogString(path: String, tokens: ArrayList<String>): ArrayList<String> {
            val log = ArrayList<String>()
            val logFile = File(path)

            if (!logFile.exists()) {
                throw IllegalLogFilePathException(path)
            }
            if (logFile.isFile) {
                log.addAll(parseSingleFile(logFile))
            } else {
                logFile.listFiles().orEmpty().forEach {
                    log.addAll(parseSingleFile(it))
                }
            }

            return log.map { line -> line.toLowerCase() }
                .filter { line ->
                    tokens.any { token ->
                        line.contains(token)
                    }
                }.filter { line -> !line.contains("bridge") } as ArrayList<String>
        }

        /**
         * Reads as single file and returns its content as string.
         *
         * @param file The file object which is to contain the log information.
         *
         * @return A List of strings
         * where each entry represents an entry in the respective log file.
         */
        private fun parseSingleFile(file: File): List<String> {
            if (!file.canRead()) {
                throw IllegalLogFilePathException(file.absolutePath)
            }
            return file.readLines().map { it.trim() }
        }

        /**
         * Extracts the device name from a log line.
         *
         * @param line The line representing one event in the log.
         * @param tokens The tokens of which one should be contained in the device name.
         *
         * @return The name of the enabled or disabled device as a String.
         */
        private fun getDeviceName(line: String, tokens: ArrayList<String>): String {
            val lineContent = line.split(" ")
            val name = lineContent.find { currentToken -> tokens.any { currentToken.contains(it) } }.orEmpty()
                .replace("'", "")

            return if (name.contains("-")) name.split("-").first() else name

        }

        /**
         * Extracts the timestamp at which an event occurred from a log line.
         *
         * @param line The line representing one event in the log.
         *
         * @return The timestamp at which the given event occurred.
         */
        private fun getTimestamp(line: String): DateTime {
            val lineContent = line.split(" ")
            return DateTime("${lineContent[0]}T${lineContent[1]}").withZone(DateTimeZone.UTC)
        }
    }
}
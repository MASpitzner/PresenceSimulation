package org.maspitzner.presencesimulation.utils.filehandling

import java.io.File

/**
 * helper lambda to create directories if the dont exist
 */
val createDirIfNotExists: (String) -> (Unit) = {
    if (!File(it).exists()) {
        File(it).mkdirs()
    }
}


/**
 * helper lambda to create files if the dont exist
 * including the parent directories
 */

val createFileIfNotExists: (String) -> (Unit) = {
    val file = File(it)
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    if (!file.exists()) {
        file.createNewFile()
    }
}
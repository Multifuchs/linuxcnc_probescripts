package de.mf.cnc.probe

import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.system.exitProcess

fun main() {
    // Load properties.
    val propStreams = listOf("probe.properties", "probe-default.properties").map {
        try {
            Path(it).inputStream().buffered()
        } catch (e: Exception) {
            System.err.println("Cannot open property file $it")
            exitProcess(1)
        }
    }
    ProbeProps.loadProps(propStreams[0])
    ProbeProps.loadDefaults(propStreams[1])
    if (!ProbeProps.check()) {
        exitProcess(1)
    }
}
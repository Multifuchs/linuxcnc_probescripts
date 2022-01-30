package de.mf.cnc.probe

import de.mf.cnc.probe.scripts.CenterOfRectangle
import kotlin.io.path.*
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

    val scriptBuilders: List<ProbeScriptTemplate> = listOf(
        CenterOfRectangle()
    )

    val scriptDir = Path("scripts")
    scriptDir.createDirectories()

    scriptBuilders.forEach { pst ->
        val program = pst.build().toString()
        (scriptDir / "${pst.name}.ngc").writeText(
            program,
            Charsets.UTF_8
        )
        println()
        println("==== START OF ${pst.name} ===")
        println(program)
        println("==== END OF ${pst.name} ===")
    }
}
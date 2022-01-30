package de.mf.cnc.probe

import de.mf.cnc.ngc.NgcParameter
import java.io.InputStream
import java.util.*
import kotlin.properties.Delegates
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility

/** Global object for user properties. */
object ProbeProps {

    private var props = mapOf<String, String>()
    private var defaults = mapOf<String, String>()

    val offXleft by this
    val offXright by this
    val offYfront by this
    val offYback by this
    val offZbottom by this

    val zMaxG53 by this
    val zClearance by this
    val xyClearance by this
    val balldiam by this

    val slowFeed by this
    val slowBackFeed by this
    val fastFeed by this

    operator fun get(key: String): String? = props[key] ?: defaults[key]

    fun loadProps(stream: InputStream) {
        this.props = loadPropsStream(stream)
    }

    fun loadDefaults(stream: InputStream) {
        this.defaults = loadPropsStream(stream)
    }

    fun check(): Boolean {
        val missingProperties = ProbeProps::class.members.asSequence()
            .filterIsInstance<KProperty<String>>()
            .filter { it.visibility == KVisibility.PUBLIC && it.isFinal }
            .map { it.name }
            .filter { get(it) == null }
            .toList()

        if(missingProperties.isEmpty()) return true

        System.err.println("Missing entries in probe.properties or probe-default.properties: ")
        missingProperties.forEach { System.err.println(" - $it") }

        return false
    }

    private fun loadPropsStream(stream: InputStream): Map<String, String> = stream.use { s ->
        val p = Properties()
        p.load(stream)
        p.entries
            .mapNotNull {
                val k = it.key?.toString()?.trim()
                val v = it.value?.toString()?.trim()
                if(k != null && v != null) k to v
                else null
            }
            .toMap()
    }

    private operator fun getValue(thisRef: Any?, property: KProperty<*>): NgcParameter {
        val r = get(property.name)
        check(r != null) { "Required property ${property.name} not found in probe.properties or probe-default.properties." }
        return NgcParameter.Unsafe(r)
    }
}
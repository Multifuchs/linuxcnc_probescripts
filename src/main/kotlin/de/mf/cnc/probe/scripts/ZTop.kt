package de.mf.cnc.probe.scripts

import de.mf.cnc.ngc.minus
import de.mf.cnc.ngc.ngc
import de.mf.cnc.probe.AxisDirection
import de.mf.cnc.probe.GPoint
import de.mf.cnc.probe.ProbeScriptBuilder
import de.mf.cnc.probe.ProbeScriptTemplate

class ZTop : ProbeScriptTemplate("probe_ztop") {
    override val prolog: String = "Probes the height of the surface bellow the probe."

    override fun buildScript(builder: ProbeScriptBuilder) {
        builder.apply {
            PROBE(GPoint(z = startZ - 4.ngc()), AxisDirection.DOWN)
            MOVETO_FASTZ()
            // We are at fastZ
            L("G10 L20 P0 Z${fastZ - probedZ}")
        }
    }
}
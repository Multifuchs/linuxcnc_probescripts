package de.mf.cnc.probe.scripts

import de.mf.cnc.ngc.*
import de.mf.cnc.probe.*

class BoreCenter : ProbeScriptTemplate("probe_bore") {
    override val prolog: String = """
        Probes the center of a bore where the probe currently is inside of.
        Parameters:
        #1: Estimated diameter of the bore.
        """.trimIndent()

    override fun buildScript(builder: ProbeScriptBuilder) {
        builder.apply {
            val estDiam = LOCALPARAM("estDiam", NUMPARAM(1))
            val estR = LOCALPARAM("estR", estDiam / 2.0.ngc())
            val fastDist = LOCALPARAM("fastDist", estR - xyClearance)
            val doGoFast = LOCALPARAM("doGoFast", fastDist greaterThan 10.0.ngc())
            val gCenter = GPoint(startX, startY)

            IF(estDiam lowerThan 0.ngc()) {
                ABORT("Estimated diameter (param 1) must be >= 0")
            }
            ELSEIF(estDiam equal 0.ngc()) {
                MSG("No estimated diameter. All moves are slow af.")
            }

            val probeValA = LOCALPARAM("probeValA", NULL)
            val probeValB = LOCALPARAM("probeValB", NULL)

            NL()

            listOf(
                Axis.X,
                Axis.Y,
                Axis.X
            ).forEach { axis ->
                val directions = AxisDirection.values().filter { it.axis == axis }
                require(directions.size == 2)

                directions.forEachIndexed { index, dir ->
                    COMMENT("Probe ${dir.name.lowercase()}")
                    IF(doGoFast) {
                        val fastPoint = GPoint(
                            mapOf(
                                axis to gCenter[axis]!!.let {
                                    if (dir.isPositive) {
                                        it + fastDist
                                    } else {
                                        it - fastDist
                                    }
                                }
                            )
                        )
                        FASTMOVE(fastPoint)
                    }
                    val probePoint = GPoint(
                        mapOf(
                            axis to gCenter[axis]!!.let {
                                if (dir.isPositive) {
                                    it + (estR + xyClearance)
                                } else {
                                    it - (estR + xyClearance)
                                }
                            }
                        )
                    )
                    PROBE(probePoint, dir)
                    ASSIGN(
                        if(index == 0) probeValA else probeValB,
                        if(axis == Axis.X) probedX else probedY
                    )
                    FASTMOVE(gCenter)
                    NL()
                }

                COMMENT("Update center of axis ${axis.name.lowercase()}")
                ASSIGN(
                    if(axis == Axis.X) startX else startY,
                    .5.ngc() * (probeValA + probeValB)
                )

                NL()

            }

            FASTMOVE(gCenter)
            L("G10 L20 P0 X${startX.toString(true)} Y${startY.toString(true)}")

            MOVETO_FASTZ()
        }
    }
}
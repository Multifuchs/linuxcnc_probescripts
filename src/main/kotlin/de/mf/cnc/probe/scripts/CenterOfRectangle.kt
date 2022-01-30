package de.mf.cnc.probe.scripts

import de.mf.cnc.ngc.*
import de.mf.cnc.probe.*

class CenterOfRectangle : ProbeScriptTemplate("probe_rect_center") {

    override val prolog = """
        Parameters:
        #1: Estimated width (X)
        #2: Estimated depth (Y)
        #3: Startpoint (0=Center(default), 1: Left-front corner)
        #4: How much the probe should be bellow the top-surface, when probing sides. (default: 3mm)
    """.trimIndent()

    private val sides = listOf(AxisDirection.LEFT, AxisDirection.RIGHT, AxisDirection.FRONT, AxisDirection.BACK)

    override fun buildScript(builder: ProbeScriptBuilder) {
        builder.apply {

            COMMENT("estXXX: Estimated value, depending on position when starting the routine and its params.")

            val estWidth = LOCALPARAM("estWidth", NgcParameter.Numbered(1))
            val estDepth = LOCALPARAM("estDepth", NgcParameter.Numbered(2))
            val startPoint = LOCALPARAM("start", NgcParameter.Numbered(3))

            val estCenterX = LOCALPARAM("estCenterX", startX)
            val estCenterY = LOCALPARAM("estCenterY", startY)

            val estCenterGPoint = GPoint(estCenterX, estCenterY)

            val sideZOffset = LOCALPARAM("sideZOff", NgcParameter.Numbered(4))

            IF(estWidth lowerThan 1.0.ngc()) {
                ABORT("Estimated width (param #1) is too small.")
            }
            ELSEIF(estDepth lowerThan 1.0.ngc()) {
                ABORT("Estimated depth (param #2) is too small.")
            }

            IF(startPoint equal 0.ngc()) {
                MSG("Probe rectangle starting from its estimated center.")
            }
            ELSEIF(startPoint equal 1.ngc()) {
                MSG("Probe rectangle starting from its estimated left-front corner.")
                ASSIGN(estCenterX, estCenterX + (estWidth / 2.0.ngc()))
                ASSIGN(estCenterY, estCenterY + (estDepth / 2.0.ngc()))
            }
            ELSE {
                ABORT("Param #3 must be 0 (center) or 1 (Left-front-corner).")
            }

            IF(sideZOffset equal 0.ngc()) {
                ASSIGN(sideZOffset, 3.ngc())
            }
            ELSEIF(sideZOffset lowerThan 0.ngc()) {
                ABORT("Param #3 must be >= 0.")
            }
            NL()

            COMMENT("Variables for results.")
            val probedSides = mapOf(
                AxisDirection.UP to LOCALPARAM("probedTop", startZ),
                AxisDirection.LEFT to LOCALPARAM("probedLeft", startX),
                AxisDirection.RIGHT to LOCALPARAM("probedRight", startX),
                AxisDirection.FRONT to LOCALPARAM("probedFront", startY),
                AxisDirection.BACK to LOCALPARAM("probedBack", startY)
            )
            NL()

            val estCenterXY = GPoint(estCenterX, estCenterY)
            IF(startPoint notEqual 0.ngc()) {
                MOVETO_FASTZ()
            }
            ELSE {
                COMMENT("Probe top surface only if starting point is center.")
                PROBE(GPoint(z = startZ - zClearance), AxisDirection.DOWN)
                ASSIGN(probedSides[AxisDirection.UP]!!, probedZ)
                MOVETO_FASTZ()
            }
            NL()

            listOf(AxisDirection.LEFT, AxisDirection.RIGHT, AxisDirection.FRONT, AxisDirection.BACK).forEach { dir ->
                COMMENT("Probe ${dir.name.lowercase()} side")
                val lengthParam = if (dir.axis == Axis.X) estWidth else estDepth
                var dist: NgcExpression = (lengthParam / 2.ngc()) + xyClearance
                var probeDist: NgcExpression = (lengthParam / 2.ngc()) - xyClearance
                if (!dir.isPositive) {
                    dist = (-1.0).ngc() * dist
                    probeDist = (-1.0).ngc() * probeDist
                }
                val clearPoint = GPoint(
                    mapOf(dir.axis to (estCenterGPoint[dir.axis]!! + dist))
                )
                FASTMOVE(clearPoint)
                FASTMOVE(GPoint(z = probedSides[AxisDirection.UP]!! - sideZOffset))
                PROBE(
                    GPoint(
                        mapOf(dir.axis to (estCenterGPoint[dir.axis]!! + probeDist))
                    ), dir.opposite
                )
                ASSIGN(probedSides[dir]!!, if (dir.axis == Axis.X) probedX else probedY)
                FASTMOVE(clearPoint)
                MOVETO_FASTZ()
                FASTMOVE(estCenterXY)

                NL()
            }

            COMMENT("Compute center.")
            val centerX = LOCALPARAM(
                "centerX",
                .5.ngc() * (probedSides[AxisDirection.LEFT]!! + probedSides[AxisDirection.RIGHT]!!)
            )
            val centerY = LOCALPARAM(
                "centerY",
                .5.ngc() * (probedSides[AxisDirection.FRONT]!! + probedSides[AxisDirection.BACK]!!)
            )
            val width = LOCALPARAM(
                "width",
                NgcFunction.Abs(probedSides[AxisDirection.RIGHT]!! - probedSides[AxisDirection.LEFT]!!)
            )
            val depth = LOCALPARAM(
                "depth",
                NgcFunction.Abs(probedSides[AxisDirection.BACK]!! - probedSides[AxisDirection.FRONT]!!)
            )

            // We are at estCenterXY and fastZ
            L("G10 L20 P0 X${estCenterXY.x!! - centerX} Y[${estCenterXY.y!! - centerY}] Z${fastZ - probedSides[AxisDirection.UP]!!}")

            DEBUG {
                txt("W: ")
                param(width)
                txt(", D: ")
                param(depth)
            }
        }
    }
}
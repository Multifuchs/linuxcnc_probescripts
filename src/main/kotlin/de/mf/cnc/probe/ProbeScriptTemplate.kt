package de.mf.cnc.probe

import de.mf.cnc.ngc.*

abstract class ProbeScriptTemplate(val name: String) {

    abstract val prolog: String

    protected abstract fun buildScript(builder: ProbeScriptBuilder)

    fun build() = ngcProgram {

        SUB(name) {
            val psb = ProbeScriptBuilder(name, prolog)
            buildScript(psb)
            // Add result to ngcSubProgramBuilder
            psb.build().forEach { NGC_BLOCK(it) }
        }

        SUB("fast_move") {
            SUB("fast_move") {
                IF(
                    (GLOBALPARAM("x") notEqual NUMPARAM(1))
                            or (GLOBALPARAM("y") notEqual NUMPARAM(2))
                            or (GLOBALPARAM("z") notEqual NUMPARAM(3))
                ) {
                    L(
                        listOf(
                            "G38.3",
                            "X${NUMPARAM(1).toString(true)}",
                            "Y${NUMPARAM(2).toString(true)}",
                            "Z${NUMPARAM(3).toString(true)}",
                            "F${ProbeProps.fastFeed.toString(true)}"
                        ).joinToString(" ")
                    )
                    IF(ProbeScriptBuilder.probeSuccessParam) {
                        ABORT("Probe collision detected.")
                    }
                }
            }
        }


    }
}

class ProbeScriptBuilder(name: String, prolog: String) : NgcSubBuilder(name) {

    init {
        prolog.lineSequence().forEach {
            COMMENT(it)
        }
        NL()
        COMMENT("Parameter with value, which indicates no value")
    }

    val NULL = LOCALPARAM("NULL", -999999999.0)

    init {
        COMMENT("Remember position, where the routine started.")
    }

    val startX = LOCALPARAM("startX", GLOBALPARAM("x"))
    val startY = LOCALPARAM("startY", GLOBALPARAM("y"))
    val startZ = LOCALPARAM("startZ", GLOBALPARAM("z"))

    val startGPoint = GPoint(startX, startY, startZ)

    init {
        NL()
        COMMENT("Read configuration parameters.")
    }

    val xyClearance = LOCALPARAM("clearanceXY", ProbeProps.xyClearance)
    val zClearance = LOCALPARAM("clearanceZ", ProbeProps.zClearance)
    val slowFeed = LOCALPARAM("slowFeed", ProbeProps.slowFeed)
    val slowBackFeed = LOCALPARAM("slowBackFeed", ProbeProps.slowBackFeed)

    val offXleft = LOCALPARAM("offXleft", ProbeProps.offXleft)
    val offXright = LOCALPARAM("offXright", ProbeProps.offXright)
    val offYfront = LOCALPARAM("offYfront", ProbeProps.offYfront)
    val offYback = LOCALPARAM("offYback", ProbeProps.offYback)
    val offZbottom = LOCALPARAM("offZbottom", ProbeProps.offZbottom)
    //val ballRadius = LOCALPARAM("ballRadius", ProbeProps.balldiam / 2.0.ngc())

    init {
        NL()
        COMMENT("Z for fast moves is start point plus clearance.")
        COMMENT("If start Z is above Z-limit, use Z-limit instead.")
    }

    val fastZ = LOCALPARAM("fastZ", NULL)

    init {
        L("G28.1 (stores the current absolute position into parameters 5161-5166, 5163 is machine Z)")
        val curZg53 = NgcParameter.Numbered(5163)
        IF((curZg53 + zClearance) greaterThan ProbeProps.zMaxG53) {
            MSG("Use zMaxG53 as max Z Height")
            ASSIGN(fastZ, startZ + (ProbeProps.zMaxG53 - curZg53))
        }
        ELSE {
            ASSIGN(fastZ, startZ + zClearance)
        }
        COMMENT("Safety check.")
        IF((fastZ lowerThan startZ) or (fastZ equal NULL)) {
            ABORT("Something went wrong with computing fastZ.")
        }

        NL()
        COMMENT("Variables, where the location of the probed point is stored.")
    }

    val probedX = LOCALPARAM("probedX", NULL)
    val probedY = LOCALPARAM("probedY", NULL)
    val probedZ = LOCALPARAM("probedZ", NULL)

    init {
        NL()
    }

    val fastMove = "fast_move"

    init {
        NL()
        NL()
    }

    fun NgcBlocksBuilder.FASTMOVE(dest: GPoint) {
        CALL_SUB(fastMove, dest.x ?: GLOBALPARAM("x"), dest.y ?: GLOBALPARAM("y"), dest.z ?: GLOBALPARAM("z"))
    }

    fun NgcBlocksBuilder.MOVETO_FASTZ() {
        FASTMOVE(GPoint(z = fastZ))
    }

    fun NgcBlocksBuilder.PROBE(to: GPoint, direction: AxisDirection) {
        L("G38.2 $to F${slowFeed.toString(true)}")
        val backupPoint = GPoint(
            mapOf(
                direction.axis to (NgcParameter.Global(direction.axis.name.lowercase()).let {
                    val backOfDist = 3.0.ngc()
                    if (direction.isPositive) {
                        it - backOfDist
                    } else {
                        it + backOfDist
                    }
                })
            )
        )
        L("G38.4 $backupPoint F${slowBackFeed.toString(true)}")

        val correction = when (direction) {
            AxisDirection.LEFT -> offXleft
            AxisDirection.RIGHT -> offXright
            AxisDirection.UP -> throw Exception("no correction possible for upwards movement")
            AxisDirection.DOWN -> offZbottom
            AxisDirection.FRONT -> offYfront
            AxisDirection.BACK -> offYback
        }

        when (direction.axis) {
            Axis.X -> {
                ASSIGN(probedX, NgcParameter.Numbered(5061) + correction)
                ASSIGN(probedY, NgcParameter.Numbered(5062))
                ASSIGN(probedZ, NgcParameter.Numbered(5063))
            }
            Axis.Y -> {
                ASSIGN(probedX, NgcParameter.Numbered(5061))
                ASSIGN(probedY, NgcParameter.Numbered(5062) + correction)
                ASSIGN(probedZ, NgcParameter.Numbered(5063))
            }
            Axis.Z -> {
                ASSIGN(probedX, NgcParameter.Numbered(5061))
                ASSIGN(probedY, NgcParameter.Numbered(5062))
                ASSIGN(probedZ, NgcParameter.Numbered(5063) + correction)
            }
        }

    }

    companion object {
        val probeSuccessParam = NgcParameter.Numbered(5070)
    }
}

data class GPoint(
    val x: NgcExpression? = null,
    val y: NgcExpression? = null,
    val z: NgcExpression? = null,
) {

    constructor(axisMap: Map<Axis, NgcExpression>) : this(
        axisMap[Axis.X],
        axisMap[Axis.Y],
        axisMap[Axis.Z],
    )

    init {
        require(x != null || y != null || z != null)
    }

    override fun toString() = listOfNotNull(
        x?.let { "X${it.toString(true)}" },
        y?.let { "Y${it.toString(true)}" },
        z?.let { "Z${it.toString(true)}" },
    ).joinToString(" ")

    operator fun get(axis: Axis) = when (axis) {
        Axis.X -> this.x
        Axis.Y -> this.y
        Axis.Z -> this.z
    }
}

enum class Axis { X, Y, Z }

enum class AxisDirection(val axis: Axis, val isPositive: Boolean) {
    LEFT(Axis.X, false), RIGHT(Axis.X, true),
    FRONT(Axis.Y, false), BACK(Axis.Y, true),
    UP(Axis.Z, true), DOWN(Axis.Z, false);

    val opposite by lazy { values().first { it.axis == axis && it.isPositive != isPositive } }
}
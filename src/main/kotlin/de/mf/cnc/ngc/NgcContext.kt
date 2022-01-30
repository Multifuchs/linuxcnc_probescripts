package de.mf.cnc.ngc

class NgcContext {
    private var nextODef = 10

    fun nextOToken(): String {
        val oDef = nextODef
        nextODef += 10
        return "O$oDef"
    }
}
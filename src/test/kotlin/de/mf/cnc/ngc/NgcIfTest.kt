package de.mf.cnc.ngc

fun main() {
    val ngc = NgcProgram(buildNgcBlocks {
        val foo = LOCALPARAM("foo", 0.3.ngc() + 10.0.ngc())
        IF(foo lowerThan 20.0.ngc()) {
            MSG("Here we go!")
            DEBUG {
                txt("FOO IS")
                param(foo)
            }
            IF(foo lowerThan 10.0.ngc()) {
                MSG("Inner If")
            }
            ELSEIF(foo lowerThan 11.0.ngc()) {
                MSG("Inner If else")
            }
            ELSE {
                MSG("Inner ese")
                ASSIGN(foo, foo + 1.ngc())
            }
        }
        ELSE {
            MSG("OH NO!")
        }
    })
    println(ngc)
}
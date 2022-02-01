package de.mf.ngcpost

import org.junit.Test
import java.io.StringWriter
import kotlin.test.assertEquals

class NgcPostprocessorKtTest {
    @Test
    fun testPerfectFile() {
        val inFile = """%
            |o10 if [1 EQ 2 * .5]
            |  G1 X1
            |o10 elseif
            |  G1 X-1
            |  o20 if [2 EQ 1000]
            |    G1 Y-1
            |  o20 endif
            |o10 endif
            |%
        """.trimMargin()
        val str = StringWriter()
        postprocessFile(inFile.reader(), str, emptySet())

        assertEquals(inFile, str.toString())
    }

    @Test
    fun testIndent() {
        val inFile = """%
            | o10 if [1 EQ 2 * .5]
            |G1 X1
            |o10 elseif
            |G1 X-1
            |    o20 if [2 EQ 1000]
            |     G1 Y-1
            |o20 endif
            |o10 else
            | G1 X42
            |   o10 endif
            |   %
        """.trimMargin()
        val str = StringWriter()
        postprocessFile(inFile.reader(), str, emptySet())

        assertEquals(
            """%
            |o10 if [1 EQ 2 * .5]
            |  G1 X1
            |o10 elseif
            |  G1 X-1
            |  o20 if [2 EQ 1000]
            |    G1 Y-1
            |  o20 endif
            |o10 else
            |  G1 X42
            |o10 endif
            |%
        """.trimMargin(), str.toString()
        )
    }

    @Test
    fun testOLabel() {
        val inFile = """%
            |if [1 EQ 2 * .5]
            |G1 X1
            |elseif
            |G1 X-1
            |if [2 EQ 1000]
            |G1 Y-1
            |endif
            |endif
            |%
        """.trimMargin()
        val str = StringWriter()
        postprocessFile(inFile.reader(), str, emptySet())

        assertEquals(
            """%
            |o10 if [1 EQ 2 * .5]
            |  G1 X1
            |o10 elseif
            |  G1 X-1
            |  o20 if [2 EQ 1000]
            |    G1 Y-1
            |  o20 endif
            |o10 endif
            |%
        """.trimMargin(), str.toString()
        )
    }

    @Test
    fun testKeepNamedOLabel() {
        val inFile = """%
            |o<meinSub> sub
            |o<meinSub> endsub
            |%
        """.trimMargin()
        val str = StringWriter()
        postprocessFile(inFile.reader(), str, emptySet())

        assertEquals(inFile, str.toString())
    }
}
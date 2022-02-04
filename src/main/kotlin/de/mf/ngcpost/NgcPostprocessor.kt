package de.mf.ngcpost

import java.io.Reader
import java.io.Writer
import java.lang.Integer.max
import java.util.*

fun postprocessFile(
    src: Reader,
    dest: Writer,
    flags: Set<String>
) {
    val out = dest.buffered()
    val fixIndent = !flags.contains("keep-indent")
    val fixOLabels = !flags.contains("keep-olabels")
    val withPercent = flags.contains("with-percent")

    if (withPercent)
        out.appendLine("%")

    var indentLevel = 0

    var iLine = 0;
    var oCurNumCode = 0
    val oNumCodes = LinkedList<Int>()

    src.buffered().lineSequence()
        .onEach { iLine++ }
        .filter { it.trim() != "%" } // We always add that sign ourselves.
        .map { Line(it) }
        .onEach { it.out = if (fixIndent) it.out.trim() else it.out.trimEnd() }
        .onEach { l -> // Fix indentation.
            if (!fixIndent) return@onEach

            if (l.keywordLC in Keywords.unindent) indentLevel--
            l.out = "  ".repeat(max(0, indentLevel)) + l.out
            if (l.keywordLC in Keywords.indent) indentLevel++
        }
        .onEach { l -> // Fix o code Labels
            if (!fixOLabels) return@onEach

            val oCodeLine = l.oLine

            // Do not fix non-numeric labels.
            if (oCodeLine != null && oCodeLine.numLabel == null) return@onEach

            if (
                l.keywordLC !in Keywords.oLabelOwner
                || (oCodeLine != null && oCodeLine.numLabel == null) // Do not relabel non-numerical labels.
            ) return@onEach

            val numCodeNr = if (l.keywordLC in Keywords.startOLabel) {
                oCurNumCode += 10
                oNumCodes.addLast(oCurNumCode)
                oCurNumCode
            } else if (l.keywordLC in Keywords.endOLabel && oNumCodes.isNotEmpty()) {
                oNumCodes.removeLastOrNull()
            } else {
                oNumCodes.lastOrNull()
            }

            if (numCodeNr == null) {
                System.err.println("No OCode number found for line $iLine (${l.trimmed}). Infile structure is broken.")
                return@onEach
            }

            // Replace or add o-code to line.
            val i = l.out.lowercase().indexOf(l.keywordLC)
            val indentLen = l.out.takeWhile { it.isWhitespace() }.length
            l.out = l.out.replaceRange(indentLen until i, "o$numCodeNr ")
        }
//        .onEach { println("$iLine '${it.str}' -> '${it.out}'") }
        .forEach { out.appendLine(it.out) }

    if (withPercent)
        out.write("%")
    out.flush()
}

private object Keywords {
    val indent = setOf("if", "elseif", "else", "sub", "while")
    val unindent = setOf("elseif", "else", "endif", "endsub", "endwhile")
    val startOLabel = setOf("if", "sub", "while")
    val endOLabel = startOLabel.map { "end$it" }.toSet()
    val oLabelOwner = indent + unindent
}

val oLabelPattern = """[oO](?<label>\d+|<\w+>)\s+(?<keyword>\p{Alpha}+)\s*(?<remaining>.*)""".toPattern()

private data class Line(val str: String) {
    val trimmed = str.trim()
    val trimmedLC = trimmed.lowercase()
    val oLine = oLabelPattern.matcher(str.trim()).takeIf { it.matches() }
        ?.let { OCodeLine(it.group("label"), it.group("keyword"), it.group("remaining")) }
    val keywordLC = oLine?.keywordLC ?: trimmedLC.takeWhile { it.isLetter() }.lowercase()
    var out = str

    companion object {

    }
}

private data class OCodeLine(val label: String, val keyword: String, val remaining: String) {
    val numLabel = label.toIntOrNull()
    val keywordLC = keyword.lowercase()
}
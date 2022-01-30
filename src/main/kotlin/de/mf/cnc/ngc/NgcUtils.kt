package de.mf.cnc.ngc

fun String.indent() = " ".repeat(4) + this
fun List<String>.indent() = map { it.indent() }
fun MutableList<String>.addAllIndented(lines: List<String>) {
    lines.forEach { add(it.indent()) }
}
fun MutableList<String>.addBlocksIndented(ctx: NgcContext, blocks: List<NgcBlock>) {
    blocks.forEach { block ->
        addAllIndented(block.build(ctx))
    }
}

fun String.isValidIdentifier() = this == this.trim() && this.isNotEmpty()

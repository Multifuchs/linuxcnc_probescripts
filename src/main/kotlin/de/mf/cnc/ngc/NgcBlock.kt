package de.mf.cnc.ngc

sealed class NgcBlock {

    /** Builds all lines for this block. */
    abstract fun build(ctx: NgcContext): List<String>

    override fun toString(): String = build(NgcContext()).joinToString("\n")

}

abstract class NgcSingleLineBlock(private val line: String) : NgcBlock() {
    override fun build(ctx: NgcContext) = listOf(line)
}

class NgcProgram(val blocks: List<NgcBlock>) : NgcBlock() {
    override fun build(ctx: NgcContext): List<String> = buildList {
        add("%")
        blocks.forEach {
            add("")
            addAll(it.build(ctx))
        }
        add("")
        add("%")
    }
}

class NgcLine(line: String) : NgcSingleLineBlock(line)
class NgcAssign(parameter: NgcParameter, expression: NgcExpression) : NgcSingleLineBlock("$parameter = $expression")

private fun String.fixComment() = replace('(', '_').replace(')', '_')
class NgcComment(txt: String) : NgcSingleLineBlock("(${txt.fixComment()})")
class NgcMsg(txt: String) : NgcSingleLineBlock("(MSG, ${txt.fixComment()})")
data class NgcDebugValue(val text: String?, val parameter: NgcParameter?)
class NgcDebug(values: List<NgcDebugValue>) : NgcSingleLineBlock(buildString {
    append("(debug, ")
    append(values.flatMap { listOf(it.text, it.parameter?.toString()) }.filterNotNull().joinToString(" "))
    append(")")
})

data class NgcBranch(val condition: NgcExpression, val blocks: List<NgcBlock>)

class NgcIf(
    private val ifElseBranches: List<NgcBranch>,
    private val elseBranch: List<NgcBlock>?
) : NgcBlock() {

    init {
        require(ifElseBranches.isNotEmpty()) { "NgcIf without a single branch." }
    }

    override fun build(ctx: NgcContext): List<String> = buildList {
        val oToken = ctx.nextOToken()

        ifElseBranches.forEachIndexed { index, ngcBranch ->
            add("$oToken ${if (index == 0) "if" else "elseif"} ${ngcBranch.condition.toString(true)}")
            addBlocksIndented(ctx, ngcBranch.blocks)
        }
        if (elseBranch != null) {
            add("$oToken else")
            addBlocksIndented(ctx, elseBranch)
        }

        add("$oToken endif")
    }
}

class NgcWhile(private val branch: NgcBranch) : NgcBlock() {
    override fun build(ctx: NgcContext): List<String> = buildList {
        val oToken = ctx.nextOToken()
        add("$oToken while ${branch.condition.toString(true)}")
        addBlocksIndented(ctx, branch.blocks)
        add("$oToken endwhile")
    }
}

class NgcSubroutine(
    private val name: String,
    private val blocks: List<NgcBlock>,
    private val returnValue: NgcExpression?
) : NgcBlock() {

    override fun build(ctx: NgcContext): List<String> = buildList {
        add("o<$name> sub")
        addBlocksIndented(ctx, blocks)
        add(buildString {
            append("o<$name> endsub")
            if (returnValue != null) {
                append(" $returnValue")
            }
        })
    }
}

class NGC {
}
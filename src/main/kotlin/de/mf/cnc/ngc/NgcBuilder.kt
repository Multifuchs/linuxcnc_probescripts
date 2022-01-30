package de.mf.cnc.ngc


//@kotlin.internal.InlineOnly
//public inline fun <E> buildList(@BuilderInference builderAction: MutableList<E>.() -> Unit): List<E> {
//    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
//    return buildListInternal(builderAction)
//}

fun ngcProgram(builderAction: NgcBlocksBuilder.() -> Unit): NgcProgram {
    val blocksBuilder = NgcBlocksBuilder()
    blocksBuilder.builderAction()
    return NgcProgram(blocksBuilder.build())
}

open class NgcBlocksBuilder() {

    protected val parts = mutableListOf<BuilderBlock>()

    fun LOCALPARAM(name: String, value: Double) = declareParam(NgcParameter.Local(name), value)
    fun LOCALPARAM(name: String, expression: NgcExpression) = declareParam(NgcParameter.Local(name), expression)
    fun GLOBALPARAM(name: String, value: Double) = declareParam(NgcParameter.Global(name), value)
    fun GLOBALPARAM(name: String, expression: NgcExpression) = declareParam(NgcParameter.Global(name), expression)
    fun GLOBALPARAM(name: String) = NgcParameter.Global(name)
    fun NUMPARAM(num: Int, value: Double) = declareParam(NgcParameter.Numbered(num), value)
    fun NUMPARAM(num: Int, expression: NgcExpression) = declareParam(NgcParameter.Numbered(num), expression)
    fun NUMPARAM(num: Int) = NgcParameter.Numbered(num)

    private fun <T : NgcParameter> declareParam(parameter: T, value: Double): T = declareParam(
        parameter,
        NgcParameter.Constant(value)
    )

    private fun <T : NgcParameter> declareParam(parameter: T, expression: NgcExpression): T {
        ASSIGN(parameter, expression)
        return parameter
    }

    fun NGC_BLOCK(ngcBlock: NgcBlock) {
        parts += NormalBlock(ngcBlock)
    }

    fun L(line: String) {
        parts += NormalBlock(NgcLine(line))
    }

    fun NL() {
        parts += NormalBlock(NgcLine(""))
    }

    fun ASSIGN(parameter: NgcParameter, expression: NgcExpression) {
        parts += NormalBlock(NgcAssign(parameter, expression))
    }

    fun IF(expression: NgcExpression, builderAction: NgcBlocksBuilder.() -> Unit) {
        val innerBlockBuilder = NgcBlocksBuilder()
        innerBlockBuilder.builderAction()
        parts += IfBlock().also { it.branches += NgcBranch(expression, innerBlockBuilder.build()) }
    }

    fun ELSEIF(expression: NgcExpression, builderAction: NgcBlocksBuilder.() -> Unit) {
        val ifBlock = parts.lastOrNull() as? IfBlock
        require(ifBlock != null && ifBlock.elseBranch == null) { "ELSEIF must appear after IF or ELSEIF" }
        val innerBlockBuilder = NgcBlocksBuilder()
        innerBlockBuilder.builderAction()
        ifBlock.branches += NgcBranch(expression, innerBlockBuilder.build())
    }

    fun ELSE(builderAction: NgcBlocksBuilder.() -> Unit) {
        val ifBlock = parts.lastOrNull() as? IfBlock
        require(ifBlock != null && ifBlock.elseBranch == null) { "ELSE must appear after IF or ELSEIF" }

        val innerBlockBuilder = NgcBlocksBuilder()
        innerBlockBuilder.builderAction()
        ifBlock.elseBranch = innerBlockBuilder.build()
    }

    fun WHILE(expression: NgcExpression, builderAction: NgcBlocksBuilder.() -> Unit) {
        val innerBlockBuilder = NgcBlocksBuilder()
        innerBlockBuilder.builderAction()
        parts += NormalBlock(NgcWhile(NgcBranch(expression, innerBlockBuilder.build())))
    }

    fun COMMENT(txt: String) {
        parts += NormalBlock(NgcComment(txt))
    }

    fun MSG(txt: String) {
        parts += NormalBlock(NgcMsg(txt))
    }

    fun DEBUG(builderAction: NgcDebugBuilder.() -> Unit) {
        val builder = NgcDebugBuilder()
        builder.builderAction()
        parts += NormalBlock(builder.build())
    }

    fun ABORT(txt: String) {
        parts += NormalBlock(NgcComment("ABORT, $txt"))
        parts += NormalBlock(NgcLine("M2"))
    }

    fun SUB(name: String, builderAction: NgcSubBuilder.() -> Unit): String {
        val subBuilder = NgcSubBuilder(name)
        subBuilder.builderAction()

        parts += NormalBlock(
            NgcSubroutine(
                name,
                subBuilder.build(),
                subBuilder.returnValue
            )
        )

        return name
    }

    fun CALL_SUB(name: String, vararg params: NgcExpression) {
        L(buildString {
            append("o<$name> call")
            params.forEach { param ->
                append(" ", param.toString(true))
            }
        })
    }

    fun build(): List<NgcBlock> = parts.map { part ->
        when (part) {
            is NormalBlock -> part.ngcBlock
            is IfBlock -> NgcIf(
                part.branches.toList(),
                part.elseBranch
            )
        }
    }

    protected sealed class BuilderBlock
    protected class NormalBlock(val ngcBlock: NgcBlock) : BuilderBlock()
    protected class IfBlock() : BuilderBlock() {
        val branches = mutableListOf<NgcBranch>()
        var elseBranch: List<NgcBlock>? = null
    }
}

open class NgcSubBuilder(private val name: String) : NgcBlocksBuilder() {
    var returnValue: NgcExpression? = null

    fun RETURN(value: NgcExpression? = null) {
        L(buildString {
            append("o<$name> return")
            if (value != null) {
                append(" ", value.toString(true))
            }
        })
    }
}

class NgcDebugBuilder {

    private val values = mutableListOf<NgcDebugValue>()

    fun txt(txt: String) {
        values += NgcDebugValue(txt, null)
    }

    fun param(parameter: NgcParameter) {
        values += NgcDebugValue(null, parameter)
    }

    fun build() = NgcDebug(values.toList())
}

fun Double.ngc() = NgcParameter.Constant(this)
fun Int.ngc() = NgcParameter.Constant(this.toDouble())

fun NgcExpression.pow(other: NgcExpression) = NgcOperator.Power(this, other)
operator fun NgcExpression.plus(other: NgcExpression) = NgcOperator.Plus(this, other)
operator fun NgcExpression.minus(other: NgcExpression) = NgcOperator.Minus(this, other)
operator fun NgcExpression.times(other: NgcExpression) = NgcOperator.Multiply(this, other)
operator fun NgcExpression.div(other: NgcExpression) = NgcOperator.Divide(this, other)
fun NgcExpression.mod(other: NgcExpression) = NgcOperator.Mod(this, other)
infix fun NgcExpression.lowerThan(other: NgcExpression) = NgcOperator.LowerThan(this, other)
infix fun NgcExpression.lowerEqual(other: NgcExpression) = NgcOperator.LowerEquals(this, other)
infix fun NgcExpression.greaterThan(other: NgcExpression) = NgcOperator.GreaterThen(this, other)
infix fun NgcExpression.greaterEqual(other: NgcExpression) = NgcOperator.GreaterEquals(this, other)
infix fun NgcExpression.equal(other: NgcExpression) = NgcOperator.Equals(this, other)
infix fun NgcExpression.notEqual(other: NgcExpression) = NgcOperator.NotEquals(this, other)
infix fun NgcExpression.and(other: NgcExpression) = NgcOperator.And(this, other)
infix fun NgcExpression.or(other: NgcExpression) = NgcOperator.Or(this, other)
infix fun NgcExpression.xor(other: NgcExpression) = NgcOperator.Xor(this, other)
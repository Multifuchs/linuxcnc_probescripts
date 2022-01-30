package de.mf.cnc.ngc

import java.util.*

sealed class NgcExpression {
    fun toString(forceBrackets: Boolean): String {
        val r = toString()
        if (forceBrackets && !(r.startsWith("[") && r.endsWith("]"))) {
            return "[$r]"
        } else {
            return r
        }
    }
}

sealed class NgcParameter : NgcExpression() {
    data class Constant(val number: Double) : NgcParameter() {
        override fun toString() = String.format(Locale.US, "%.6f", number)
            .trimEnd('0')
            .trimEnd('.')
    }

    data class Numbered(val i: Int) : NgcParameter() {
        override fun toString() = "#$i"
    }

    data class Local(val name: String) : NgcParameter() {
        init {
            require(name == name.trim())
        }

        override fun toString() = "#<$name>"
    }

    data class Global(val name: String) : NgcParameter() {
        init {
            require(name.isValidIdentifier())
        }

        override fun toString() = "#<_$name>"
    }

    data class Ini(val section: String, val name: String) : NgcParameter() {
        init {
            require(section.isValidIdentifier())
            require(name.isValidIdentifier())
        }

        override fun toString() = "#<_ini[$section]$name>"
    }

    data class Hal(val halItem: String) : NgcParameter() {
        init {
            require(halItem.isValidIdentifier())
        }

        override fun toString() = "#<_hal[${halItem}]>"
    }
}

sealed class NgcOperator(val left: NgcExpression, val right: NgcExpression) : NgcExpression() {
    abstract val sign: String

    override fun toString() = "[$left $sign $right]"

    class Power(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "**"
    }

    class Multiply(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "*"
    }

    class Divide(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "/"
    }

    class Mod(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "MOD"
    }

    class Plus(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "+"
    }

    class Minus(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "-"
    }

    class Equals(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "EQ"
    }

    class NotEquals(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "NE"
    }

    class GreaterThen(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "GT"
    }

    class LowerThan(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "LT"
    }

    class GreaterEquals(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "GE"
    }

    class LowerEquals(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "LE"
    }

    class And(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "AND"
    }

    class Or(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "OR"
    }

    class Xor(left: NgcExpression, right: NgcExpression) : NgcOperator(left, right) {
        override val sign = "XOR"
    }
}

sealed class NgcFunction(private val param: NgcExpression) : NgcExpression() {
    val name = javaClass.kotlin.simpleName!!.uppercase()
    override fun toString() = "$name[$param]"

    class Atan(param: NgcExpression) : NgcFunction(param)
    class Abs(param: NgcExpression) : NgcFunction(param)
    class Acos(param: NgcExpression) : NgcFunction(param)
    class Asin(param: NgcExpression) : NgcFunction(param)
    class Cos(param: NgcExpression) : NgcFunction(param)
    class Exp(param: NgcExpression) : NgcFunction(param)
    class Fix(param: NgcExpression) : NgcFunction(param)
    class Fup(param: NgcExpression) : NgcFunction(param)
    class Round(param: NgcExpression) : NgcFunction(param)
    class Ln(param: NgcExpression) : NgcFunction(param)
    class Sin(param: NgcExpression) : NgcFunction(param)
    class Sqrt(param: NgcExpression) : NgcFunction(param)
    class Tan(param: NgcExpression) : NgcFunction(param)
    class Exists(param: NgcExpression) : NgcFunction(param)
}
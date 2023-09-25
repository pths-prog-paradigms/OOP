import java.lang.AssertionError
import java.text.ParseException

fun <A, AN : A, R> ((AN) -> R).casting(): (A) -> R = { this(it as AN) }
fun <A, B, AN : A, BN : B, R> ((AN, BN) -> R).casting(): (A, B) -> R = { a: A, b: B -> this(a as AN, b as BN) }

class Parser<ExprT>(
    val string: String,
    val conster: (Double) -> ExprT,
    val xer: () -> ExprT,
    val negater: (ExprT) -> ExprT,
    val exper: (ExprT) -> ExprT,
    val adder: (ExprT, ExprT) -> ExprT,
    val subtracter: (ExprT, ExprT) -> ExprT,
    val multiplier: (ExprT, ExprT) -> ExprT,
    val divider: (ExprT, ExprT) -> ExprT,
) {
    companion object {
        fun parseSolution(str: String) = Parser(
            str,
            ::Const,
            { X },
            ::Negate,
            ::Exp,
            ::Add,
            ::Subtract,
            ::Multiply,
            ::Divide,
        ).parseExpr()

        fun parseCustom(str: String) = Parser(
            str,
            ::CustomConst,
            ::CustomX,
            ::CustomNegate,
            ::CustomExp,
            ::CustomAdd,
            ::CustomSubtract,
            ::CustomMultiply,
            ::CustomDivide,
        ).parseExpr()

        fun parseErrorMessage(str: String) = Parser(
            str,
            { "Const($it)" },
            { "X" },
            { "Negate($it)" },
            { "Exp($it)" },
            { l, r -> "Add($l, $r)" },
            { l, r -> "Subtract($l, $r)" },
            { l, r -> "Multiply($l, $r)" },
            { l, r -> "Divide($l, $r)" },
        ).parseExpr()
    }

    var pos = 0

    private inline fun checkNotEnd() {
        if (pos >= string.length) throw ParseException("Unexpected EOF", pos)
    }

    private inline fun eof() = (pos >= string.length)

    private fun expect(c: Char) {
        if (pop() != c) {
            throw ParseException("Expected $c", pos - 1)
        }
    }

    private fun expect(string: String) {
        val from = pos
        var index = 0
        for (c in string) {
            if (peek() != c) throw ParseException("Expected $string, not ${string.take(index)}$c...", from)
            pop()
            index++
        }
    }

    private fun <T> expectAs(string: String, value: T): T {
        expect(string)
        return value
    }

    private fun peek(): Char {
        checkNotEnd()
        return string[pos]
    }

    private fun take(c: Char): Boolean {
        if (peek() == c) {
            pop()
            return true
        } else return false
    }

    private fun pop(): Char {
        checkNotEnd()
        return string[pos++]
    }

    private fun skipWs() {
        while (pos < string.length && Character.isWhitespace(peek())) pop()
    }

    private fun parseExpr(): ExprT {
        skipWs()
        var term = parseTerm()
        skipWs()
        while (!eof() && (peek() == '+' || peek() == '-')) {
            if (take('-')) {
                term = subtracter(term, parseTerm())
            } else if (take('+')) {
                term = adder(term, parseTerm())
            } else throw AssertionError()
            skipWs()
        }
        return term
    }

    private fun parseTerm(): ExprT {
        skipWs()
        var unit = parseUnit()
        skipWs()
        while (!eof() && (peek() == '*' || peek() == '/')) {
            if (take('*')) {
                unit = multiplier(unit, parseUnit())
            } else if (take('/')) {
                unit = divider(unit, parseUnit())
            } else throw AssertionError()
            skipWs()
        }
        return unit
    }

    private fun parseUnit(): ExprT {
        skipWs()
        val c = pop()
        return when (c) {
            '(' -> {
                val res = parseExpr()
                expect(')')
                skipWs()
                res
            }

            '-' -> negater(parseUnit())
            'e' -> {
                expect("xp")
                exper(parseUnit())
            }

            'x' -> {
                xer()
            }

            'I' -> {
                expect("nfinity")
                conster(Double.POSITIVE_INFINITY)
            }

            in '0'..'9', '.' -> {
                val sb = StringBuilder()
                sb.append(c)

                while (!eof() && (peek() in '0'..'9' || peek() == '.' || peek() == 'E' || peek() == 'e')) {
                    if (peek() == 'E' || peek() == 'e') sb.append(pop())
                    sb.append(pop())
                }
                conster(sb.toString().toDouble())
            }

            else -> throw ParseException("Unknown sequence", pos)
        }
    }

    override fun toString(): String = string.substring(0, pos) + " $ " + string.substring(pos)
}
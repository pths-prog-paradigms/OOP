import TestsUtil.Fail
import TestsUtil.Success
import java.io.File
import java.util.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToLong

const val ENABLE_HEAVY_TESTS = true

fun main(): Unit = TestsUtil().run {
    val seed = 5663023956630239L
    val rnd = Random(seed)
    fun generate(depth: Int): String = if (depth == 0) {
        if (rnd.nextBoolean()) "x" else rnd.nextDouble(-100.0, 100.0).toString()
    } else if (depth < 8) when (rnd.nextInt(11)) {
        0, 1, 2 -> "x"
        3, 4 -> rnd.nextDouble(-100.0, 100.0).toString()
        5 -> "-(${generate(depth - 1)})"
        6 -> "exp(${generate(depth - 1)})"
        7 -> "(${generate(depth - 1)}) + (${generate(depth - 1)})"
        8 -> "(${generate(depth - 1)}) - (${generate(depth - 1)})"
        9 -> "(${generate(depth - 1)}) * (${generate(depth - 1)})"
        10 -> "(${generate(depth - 1)}) / (${generate(depth - 1)})"
        else -> throw AssertionError()
    } else when (rnd.nextInt(6)) {
        0 -> "-(${generate(depth - 1)})"
        1 -> "exp(${generate(depth - 1)})"
        2 -> "(${generate(depth - 1)}) + (${generate(depth - 1)})"
        3 -> "(${generate(depth - 1)}) - (${generate(depth - 1)})"
        4 -> "(${generate(depth - 1)}) * (${generate(depth - 1)})"
        5 -> "(${generate(depth - 1)}) / (${generate(depth - 1)})"
        else -> throw AssertionError()
    }

    testBlock("Basic") {
        testGroup("Evaluation: Manual") {
            fun checkManual(repr: String) =
                checkExpression(repr, rnd, 5, verboseSuccess = true, verboseRepr = true, separate = true)
            checkManual("x")
            checkManual("1.0")
            checkManual("x + 2")
            checkManual("x * 2")
            checkManual("x / 2")
            checkManual("x - 2")
            checkManual("  1 + exp (x)")
            checkManual(" - 1 + exp (-x)")
            checkManual(" x + 2 * 3 + 4 ")
        }

        testGroup("Evaluation: Random light") {
            repeat(20) {
                checkExpression(generate(2), rnd, 10, verboseSuccess = true, verboseRepr = true, separate = false)
            }

            repeat(100) {
                checkExpression(generate(4), rnd, 100, verboseSuccess = false, verboseRepr = true, separate = false)
            }
        }

        testGroup("Evaluation: Random heavy", listOf("Evaluation: Manual", "Evaluation: Random light")) {
            repeat(100) {
                checkExpression(generate(10), rnd, 1000, verboseSuccess = false, verboseRepr = true, separate = false)
            }
        }

        testGroup(
            "Evaluation: Random impossible",
            listOf("Evaluation: Manual", "Evaluation: Random light", "Evaluation: Random heavy")
        ) {
            if (ENABLE_HEAVY_TESTS) {
                repeat(100) {
                    checkExpression(
                        generate(20),
                        rnd,
                        1000,
                        verboseSuccess = true,
                        verboseRepr = false,
                        separate = false
                    )
                }
            }
        }

        fun checkToString(repr: String, verboseSuccess: Boolean, verboseRes: Boolean) = expectEqual(
            Parser.parseErrorMessage(repr) + ".toString()",
            verboseSuccess = verboseSuccess,
            verboseRes = verboseRes,
            { Parser.parseSolution(repr).toString() },
            { Parser.parseCustom(repr).toString() },
            { expected, got -> "returned $got, but should've returned $expected" }
        )
        testGroup("toString: Manual") {
            checkToString("x", verboseSuccess = true, verboseRes = true)
            checkToString("1.0", verboseSuccess = true, verboseRes = true)
            checkToString("x + 2", verboseSuccess = true, verboseRes = true)
            checkToString("x * 2", verboseSuccess = true, verboseRes = true)
            checkToString("x / 2", verboseSuccess = true, verboseRes = true)
            checkToString("x - 2", verboseSuccess = true, verboseRes = true)
            checkToString("  1 + exp (x)", verboseSuccess = true, verboseRes = true)
            checkToString(" - 1 + exp (-x)", verboseSuccess = true, verboseRes = true)
            checkToString(" x + 2 * 3 + 4 ", verboseSuccess = true, verboseRes = true)
        }

        testGroup("toString: Random light") {
            repeat(20) {
                checkToString(generate(2), verboseSuccess = true, verboseRes = true)
            }

            repeat(100) {
                checkToString(generate(4), verboseSuccess = false, verboseRes = true)
            }
        }

        testGroup("toString: Random heavy", listOf("toString: Manual", "toString: Random light")) {
            repeat(100) {
                checkToString(generate(10), verboseSuccess = false, verboseRes = false)
            }
        }

        testGroup(
            "toString: Random impossible",
            listOf("toString: Manual", "toString: Random light", "toString: Random heavy")
        ) {
            if (ENABLE_HEAVY_TESTS) {
                repeat(100) {
                    checkToString(generate(23), verboseSuccess = false, verboseRes = false)
                }
            }
        }

    }
    testBlock("Advanced") {
        testGroup("Correctness check") {
            fun check(repr: String, expected: String) = expectEqual(
                action = Parser.parseErrorMessage(repr) + ".toMinimizedString()",
                verboseSuccess = true,
                verboseRes = true,
                expected = { expected },
                got = { Parser.parseSolution(repr).toMinimizedString() },
                comment = { expect, got -> "returned $got, but should've returned $expect" }
            )
            check("x", "x")
            check(".0", "0.0")
            check("x + 2", "x + 2.0")
            check("x - 2", "x - 2.0")
            check("x * 2", "x * 2.0")
            check("x / 2", "x / 2.0")
            check("2 + x", "2.0 + x")
            check("2 - x", "2.0 - x")
            check("2 * x", "2.0 * x")
            check("2 / x", "2.0 / x")
            check("(x + x)  + (x + x) ", "x + x + x + x")
            check("(x + x)  + (x - x) ", "x + x + x - x")
            check("(x + x)  + (x * x) ", "x + x + x * x")
            check("(x + x)  + (x / x) ", "x + x + x / x")
            check("(-x)     + (-x)    ", "-x + -x")
            check("(exp(x)) + (exp(x))", "exp(x) + exp(x)")
            check("(x + x)  - (x + x) ", "x + x - (x + x)")
            check("(x - x)  - (x - x) ", "x - x - (x - x)")
            check("(x * x)  - (x * x) ", "x * x - x * x")
            check("(x / x)  - (x / x) ", "x / x - x / x")
            check("(-x)     - (-x)    ", "-x - -x")
            check("(exp(x)) - (exp(x))", "exp(x) - exp(x)")
            check("(x + x)  * (x + x) ", "(x + x) * (x + x)")
            check("(x - x)  * (x - x) ", "(x - x) * (x - x)")
            check("(x * x)  * (x * x) ", "x * x * x * x")
            check("(x / x)  * (x / x) ", "x / x * x / x")
            check("(-x)     * (-x)    ", "-x * -x")
            check("(exp(x)) * (exp(x))", "exp(x) * exp(x)")
            check("(x + x)  / (x + x) ", "(x + x) / (x + x)")
            check("(x - x)  / (x - x) ", "(x - x) / (x - x)")
            check("(x * x)  / (x * x) ", "x * x / (x * x)")
            check("(x / x)  / (x / x) ", "x / x / (x / x)")
            check("(-x)     / (-x)    ", "-x / -x")
            check("(exp(x)) / (exp(x))", "exp(x) / exp(x)")
            check("- (x + x) ", "-(x + x)")
            check("- (x - x) ", "-(x - x)")
            check("- (x * x) ", "-(x * x)")
            check("- (x / x) ", "-(x / x)")
            check("- (-x)    ", "-(-x)")
            check("- (exp(x))", "-exp(x)")
            check("- 2", "-2.0")
            check("exp(2)", "exp(2.0)")
            check("exp(-2)", "exp(-2.0)")
        }

        fun checkMinimized(
            initial: String,
            on: List<Double>,
            verboseSuccess: Boolean,
            verboseRepr: Boolean,
        ) = expectSuccess(
            (if (verboseRepr) Parser.parseErrorMessage(initial) else "(...)") + ".toMinimizedString(), where x = $on",
            verboseSuccess = verboseSuccess
        ) {
            val minimString = Parser.parseSolution(initial).toMinimizedString()
            val minimized = Parser.parseSolution(minimString)
            for (x in on) {
                val expectedRes = minimized(x)
                val gotRes = Parser.parseCustom(initial)(x)
                if (!isEqual(expectedRes, gotRes, 1e-6)) return@expectSuccess Fail(
                    "($initial)|x=$x is $expectedRes, " +
                            "but your ($minimString)|x=$x is $gotRes"
                )
            }
            Success()

        }
        testGroup("Random light", listOf("Correctness check")) {
            repeat(20) {
                checkMinimized(
                    generate(4),
                    List(5) { rnd.nextInt2(-1000, 1000).toDouble() },
                    verboseSuccess = true,
                    verboseRepr = true
                )
            }
            repeat(20) {
                checkMinimized(
                    generate(4),
                    List(5) { rnd.nextDouble(-1000.0, 1000.0) },
                    verboseSuccess = false,
                    verboseRepr = true
                )
            }
        }
        testGroup("Random heavy", listOf("Correctness check")) {
            repeat(100) {
                checkMinimized(
                    generate(10),
                    List(20) { rnd.nextInt2(-1000, 1000).toDouble() },
                    verboseSuccess = false,
                    verboseRepr = true
                )
            }
        }
        Multiply(
            Divide(
                Divide(X, X),
                Negate(Divide(Divide(X, Const(57.615311256727466)), X))
            ),
            Add(Add(X, Const(49.62629308977063)), Negate(X))
        )
        Multiply(
            Divide(Divide(X, X), Negate(Divide(Divide(X, Const(57.615311256727466)), X))),
            Add(Add(X, Const(49.62629308977063)), Negate(X))
        )
        testGroup("Random impossible", listOf("Correctness check", "Random light", "Random heavy")) {
            repeat(100) {
                checkMinimized(
                    generate(10),
                    List(20) { rnd.nextInt2(-1000, 1000).toDouble() },
                    verboseSuccess = false,
                    verboseRepr = false
                )
            }
        }
    }
    testBlock("Bonus") {
        Unit
    }

    println()
    println("Results: ")
    fun res(key: String): Double {
        val passed = passed.filterKeys { it.first == key }.values.sum()
        val of = total.filterKeys { it.first == key }.values.sum()
        return if (of == 0) 1.0 else passed * 1.0 / of
    }


    indent++
    println("Basic   : ${((res("Basic") * 1000).roundToLong() / 10)}%")
    println("Advanced: ${((res("Advanced") * 1000).roundToLong() / 10)}%")
    println("Bonus   : ${((res("Bonus") * 1000).roundToLong() / 10)}%")
    indent--
//    val file = File("res.txt")
//    if (file.exists()) file.delete()
//    file.createNewFile()
//    file.writeText(
//        "Basic   : ${((res("Basic") * 1000).roundToLong() / 10)}%" + "\n" +
//                "Advanced: ${((res("Advanced") * 1000).roundToLong() / 10)}%" + "\n" +
//                "Bonus   : ${((res("Bonus") * 1000).roundToLong() / 10)}%" + "\n"
//    )

}

data class CustomExpr(val func: (Double) -> Double, val s: String) : (Double) -> Double {
    override operator fun invoke(x: Double): Double = func(x)
    override fun toString(): String = s
}


fun CustomConst(c: Double) = CustomExpr({ c }, c.toString())
fun CustomX() = CustomExpr({ it }, "x")
fun CustomNegate(argument: CustomExpr) = CustomExpr({ -argument(it) }, "-($argument)")
fun CustomExp(argument: CustomExpr) = CustomExpr({ exp(argument(it)) }, "exp($argument)")
fun CustomAdd(left: CustomExpr, right: CustomExpr) = CustomExpr({ left(it) + right(it) }, "($left) + ($right)")
fun CustomMultiply(left: CustomExpr, right: CustomExpr) = CustomExpr({ left(it) * right(it) }, "($left) * ($right)")
fun CustomSubtract(left: CustomExpr, right: CustomExpr) = CustomExpr({ left(it) - right(it) }, "($left) - ($right)")
fun CustomDivide(left: CustomExpr, right: CustomExpr) = CustomExpr({ left(it) / right(it) }, "($left) / ($right)")


fun Random.nextInt2(from: Int, to: Int) = nextInt(to - from) - from

fun isEqual(expected: Double, actual: Double, precision: Double): Boolean {
    val error = abs(actual - expected)
    return actual.isNaN() && precision.isNaN() ||
            actual == expected ||
            error <= precision ||
            error <= precision * abs(expected) ||
            !java.lang.Double.isFinite(expected) ||
            abs(expected) > 1e100 ||
            abs(expected) < precision && !java.lang.Double.isFinite(actual)
}


fun TestsUtil.checkExpression(
    view: String,
    expr: Expr,
    customExpr: CustomExpr,
    rnd: Random,
    inpSize: Int,
    verboseSuccess: Boolean,
    verboseRepr: Boolean,
    separate: Boolean
) {
    val inputs = listOf(
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        -.0, .0, 1.0, -1.0
    ) + List(inpSize) { rnd.nextDouble() }
    if (separate) {
        for (x in inputs) {
            expectSuccess(if (verboseRepr) "$view.invoke($x)" else "(...).invoke($x)", verboseSuccess) {
                val expected = customExpr(x)
                val got = expr(x)
                if (isEqual(expected, got, 1e-9)) {
                    TestsUtil.Success()
                } else {
                    TestsUtil.Fail("returned $got, but should've returned $expected")
                }
            }
        }
    } else expectSuccess(if (verboseRepr) "$view.invoke(..)" else "(...).invoke(..)", verboseSuccess) {
        for (x in inputs) {
            val expected = customExpr(x)
            val got = expr(x)
            if (!isEqual(expected, got, 1e-9)) {
                TestsUtil.Fail("for x = $x returned $got, but should've returned $expected")
            }
        }
        TestsUtil.Success()
    }
}

fun TestsUtil.checkExpression(
    repr: String,
    rnd: Random,
    inpSize: Int,
    verboseSuccess: Boolean,
    verboseRepr: Boolean,
    separate: Boolean
) = checkExpression(
    Parser.parseErrorMessage(repr),
    Parser.parseSolution(repr),
    Parser.parseCustom(repr),
    rnd, inpSize, verboseSuccess, verboseRepr, separate
)


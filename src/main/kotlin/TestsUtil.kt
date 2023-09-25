@file:Suppress("unused")


class TestsUtil {
    val ignoreStackSince = Thread.getAllStackTraces()[Thread.currentThread()]!!
        .run { this[size - 2].className + "$" + this[4].methodName + "$" }
    val passed = mutableMapOf<Pair<String, String>, Int>()
    val total = mutableMapOf<Pair<String, String>, Int>()
    var indent = 0
    fun println(x: Any?) {
        kotlin.io.println("    ".repeat(indent) + x)
    }

    private var blockName: String? = null
    fun testBlock(name: String, runnable: () -> Unit) {
        blockName = name
        println("$name:")
        indent++
        runnable()
        indent--
        blockName = null
    }

    private var suppressTesting = false
    fun testGroup(name: String, runnable: () -> Unit) = testGroup(name, listOf(), runnable)

    private var groupName: String? = null
    fun testGroup(name: String, needs: List<String>, runnable: () -> Unit) {
        groupName = name
        println("$name:")
        indent++
        if (needs.all { passed[blockName to it] == total[blockName to it] }) {
            runnable()
            flushSuccesses()
        } else {
            println("Requires successful completion of tests ${needs.joinToString(", ")}")
            suppressTesting = true
            runnable()
            suppressTesting = false
        }
        indent--
        groupName = null
    }

    private val stackTraces = mutableListOf<List<StackTraceElement>>()

    private var successes = 0
    private fun flushSuccesses() {
        if (successes != 0) {
            println("$successes more successes")
            successes = 0
        }
    }


    public abstract class TestResult(val comment: String?) {
        override fun toString(): String = if (comment != null) "($comment)" else ""
    }

    public class Success(comment: String? = null) : TestResult(comment)
    public class Fail(comment: String? = null) : TestResult(comment)

    private fun succeed(line: String, verboseSuccess: Boolean) {
        passed[blockName!! to groupName!!] = (passed[blockName to groupName] ?: 0) + 1
        if (verboseSuccess) {
            flushSuccesses()
            println(line)
        } else {
            successes++
        }
    }

    private fun printStack(e: Throwable) {
        val important = e.stackTrace.takeWhile { !it.className.startsWith(ignoreStackSince) }
        if (important !in stackTraces) {
            e.printStackTrace(System.out)
            stackTraces.add(important)
        }
    }

    fun expectSuccess(action: String, verboseSuccess: Boolean = true, runnable: () -> TestResult) {
        total[blockName!! to groupName!!] = (total[blockName to groupName] ?: 0) + 1
        if (!suppressTesting) {
            try {
                val res = runnable()
                if (res is Success) {
                    succeed("Succeeded $action $res", verboseSuccess)
                } else {
                    flushSuccesses()
                    println("Failed $action $res")
                }
            } catch (e: Throwable) {
                flushSuccesses()
                println("Unexpected [${e.javaClass.name} : ${e.message}] during $action")
                printStack(e)
            }
        }
    }

    fun <T> expectEqual(
        action: String,
        verboseSuccess: Boolean = true,
        verboseRes: Boolean = true,
        expected: () -> T,
        got: () -> T,
        comment: (T, T) -> String?
    ) = expectSuccess(action, verboseSuccess) {
        val expectedRes = expected()
        val gotRes = got()
        if (gotRes == expectedRes) {
            Success()
        } else Fail(comment(expectedRes, gotRes))
    }


    fun expectFailure(
        action: String,
        expected: Class<Throwable>,
        runnable: () -> Any?,
        verboseSuccess: Boolean = true
    ) {
        total[blockName!! to groupName!!] = (total[blockName to groupName] ?: 0) + 1
        if (!suppressTesting) {
            try {
                val res = runnable()
                flushSuccesses()
                println("$action expected to throw ${expected.name}, but successfully returned $res")
            } catch (e: Throwable) {
                if (e.javaClass == expected) {
                    succeed("$action passed failure test", verboseSuccess)
                } else {
                    flushSuccesses()
                    println("$action expected to throw ${expected.name}, but threw [${e.javaClass.name} : ${e.message}]")
                    printStack(e)
                }
            }
        }
    }

    fun <A, R> compare(
        correct: (A) -> R, tested: (A) -> R, a: A, funcName: String,
        silentArgs: Boolean = false,
        silentRes: Boolean = false,
        verboseSuccess: Boolean = true
    ) = expectSuccess(if (silentArgs) "$funcName(...)" else "$funcName($a)", verboseSuccess) {
        val expected = correct(a)
        val got = tested(a)
        if (got == expected) {
            Success()
        } else {
            Fail(if (silentRes) "returned $got" else "returned $got, but should've returned $expected")
        }
    }

}


open class BFn {
    open operator fun invoke(): Any? = throw IllegalAccessException("Incorrect arity 0")
    open operator fun invoke(arg0: Any?): Any? = throw IllegalAccessException("Incorrect arity 1")
    open operator fun invoke(arg0: Any?, arg1: Any?): Any? = throw IllegalAccessException("Incorrect arity 2")
    open operator fun invoke(arg0: Any?, arg1: Any?, arg2: Any?): Any? =
        throw IllegalAccessException("Incorrect arity 3")

    open operator fun invoke(arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?): Any? =
        throw IllegalAccessException("Incorrect arity 4")

    open operator fun invoke(arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?): Any? =
        throw IllegalAccessException("Incorrect arity 5")

    open operator fun invoke(arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?): Any? =
        throw IllegalAccessException("Incorrect arity 6")

    /**
     * First argument is always ignored forasmuch this[] is impossible. Pass Unit as first argument.
     */
    operator fun get(vararg args: Any?): Any? = when (args.size) {
        1 -> this()
        2 -> this(args[0])
        3 -> this(args[0], args[1])
        4 -> this(args[0], args[1], args[2])
        5 -> this(args[0], args[1], args[2], args[3])
        6 -> this(args[0], args[1], args[2], args[3], args[4])
        7 -> this(args[0], args[1], args[2], args[3], args[4], args[5])
        else -> throw IllegalAccessException("Incorrect arity ${args.size - 1}")
    }
}

fun <R> BFn(func: () -> R) = object : BFn() {
    override fun invoke(): Any? = func()
}

fun <A, R> BFn(func: (A) -> R) = object : BFn() {
    override fun invoke(a: Any?): Any? = func(a as A)
}

fun <A, B, R> BFn(func: (A, B) -> R) = object : BFn() {
    override fun invoke(a: Any?, b: Any?): Any? = func(a as A, b as B)
}

fun <A, B, C, R> BFn(func: (A, B, C) -> R) = object : BFn() {
    override fun invoke(a: Any?, b: Any?, c: Any?): Any? = func(a as A, b as B, c as C)
}

fun <A, B, C, D, R> BFn(func: (A, B, C, D) -> R) = object : BFn() {
    override fun invoke(a: Any?, b: Any?, c: Any?, d: Any?): Any? = func(a as A, b as B, c as C, d as D)
}

fun <A, B, C, D, E, R> BFn(func: (A, B, C, D, E) -> R) = object : BFn() {
    override fun invoke(a: Any?, b: Any?, c: Any?, d: Any?, e: Any?): Any? =
        func(a as A, b as B, c as C, d as D, e as E)
}

fun <A, B, C, D, E, F, R> BFn(func: (A, B, C, D, E, F) -> R) = object : BFn() {
    override fun invoke(a: Any?, b: Any?, c: Any?, d: Any?, e: Any?, f: Any?): Any? =
        func(a as A, b as B, c as C, d as D, e as E, f as F)
}


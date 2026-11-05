package com.kodomo.app.domain.engine

import com.kodomo.app.core.util.PersianDigits
import kotlin.random.Random

/**
 * A dynamic arithmetic challenge shown together with the parent PIN. Problems are intentionally
 * beyond what a 2-8 year old can solve (two-digit sums, differences and products) yet trivial for adults.
 */
data class ParentGateChallenge(val left: Int, val right: Int, val operator: Operator) {

    enum class Operator(val symbol: String) { PLUS("+"), MINUS("−"), TIMES("×") }

    val answer: Int
        get() = when (operator) {
            Operator.PLUS -> left + right
            Operator.MINUS -> left - right
            Operator.TIMES -> left * right
        }

    /** e.g. «۲۴ + ۱۷ = ؟», wrapped in a left-to-right isolate so the equation is not mirrored inside RTL text. */
    val questionPersian: String
        get() = "\u2066${PersianDigits.toPersian(left)} ${operator.symbol} ${PersianDigits.toPersian(right)} = ؟\u2069"

    val questionLatin: String get() = "$left ${operator.symbol} $right = ?"

    /** Accepts Persian, Arabic-Indic or Latin digits, with or without surrounding spaces. */
    fun isCorrect(input: String): Boolean = PersianDigits.parseIntOrNull(input) == answer

    companion object {
        fun random(random: Random = Random.Default): ParentGateChallenge =
            when (Operator.entries[random.nextInt(Operator.entries.size)]) {
                Operator.PLUS -> ParentGateChallenge(random.nextInt(12, 49), random.nextInt(11, 39), Operator.PLUS)
                Operator.MINUS -> {
                    val a = random.nextInt(40, 99)
                    ParentGateChallenge(a, random.nextInt(11, a - 10), Operator.MINUS)
                }
                Operator.TIMES -> ParentGateChallenge(random.nextInt(6, 13), random.nextInt(6, 13), Operator.TIMES)
            }
    }
}

/** Throttles brute-force attempts on the parent gate: 5 failures lock the gate for 60 seconds, doubling after. */
class ParentGateThrottle(private val nowMillis: () -> Long = System::currentTimeMillis) {
    private var failures = 0
    private var lockedUntil = 0L
    private var lockStreak = 0

    fun isLocked(): Boolean = nowMillis() < lockedUntil

    fun remainingLockSeconds(): Long = ((lockedUntil - nowMillis()).coerceAtLeast(0) + 999) / 1000

    fun registerFailure() {
        failures++
        if (failures >= MAX_FAILURES) {
            lockStreak++
            lockedUntil = nowMillis() + BASE_LOCK_MILLIS * (1L shl (lockStreak - 1).coerceAtMost(4))
            failures = 0
        }
    }

    fun registerSuccess() {
        failures = 0
        lockStreak = 0
        lockedUntil = 0
    }

    private companion object {
        const val MAX_FAILURES = 5
        const val BASE_LOCK_MILLIS = 60_000L
    }
}

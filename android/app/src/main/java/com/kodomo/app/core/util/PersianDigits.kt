package com.kodomo.app.core.util

/**
 * Conversion helpers between Persian (۰-۹), Arabic-Indic (٠-٩) and Latin (0-9) digits.
 * Every numeric input in Kodomo (PIN, parent-gate answers, custom time) goes through [normalize]
 * so that the user can type with either keyboard layout.
 */
object PersianDigits {
    private const val PERSIAN = "۰۱۲۳۴۵۶۷۸۹"
    private const val ARABIC = "٠١٢٣٤٥٦٧٨٩"

    fun normalize(input: String): String = buildString(input.length) {
        for (c in input) {
            val p = PERSIAN.indexOf(c)
            val a = ARABIC.indexOf(c)
            when {
                p >= 0 -> append('0' + p)
                a >= 0 -> append('0' + a)
                else -> append(c)
            }
        }
    }

    fun toPersian(input: String): String = buildString(input.length) {
        for (c in input) append(if (c in '0'..'9') PERSIAN[c - '0'] else c)
    }

    fun toPersian(value: Int): String = toPersian(value.toString())

    fun toPersian(value: Long): String = toPersian(value.toString())

    fun parseIntOrNull(input: String): Int? = normalize(input).trim().toIntOrNull()

    fun digitsOnly(input: String): String = normalize(input).filter { it.isDigit() }
}

fun Int.fa(): String = PersianDigits.toPersian(this)
fun Long.fa(): String = PersianDigits.toPersian(this)
fun String.fa(): String = PersianDigits.toPersian(this)

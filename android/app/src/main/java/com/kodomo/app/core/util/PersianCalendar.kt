package com.kodomo.app.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Gregorian <-> Solar Hijri (Jalali) conversion using the standard arithmetic algorithm. */
object PersianCalendar {

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    fun monthName(month: Int): String = monthNames[(month - 1).coerceIn(0, 11)]

    fun toJalali(date: LocalDate): JalaliDate {
        val gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + (days % 31)
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        val jy2 = jy + 1595
        var days = -355668 + (365 * jy2) + ((jy2 / 33) * 8) + (((jy2 % 33) + 3) / 4) + jd +
            if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val monthDays = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 1
        while (gm <= 12 && gd > monthDays[gm]) {
            gd -= monthDays[gm]
            gm++
        }
        return LocalDate.of(gy, gm, gd)
    }

    fun daysInJalaliMonth(year: Int, month: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        else -> if (toGregorian(year, 12, 30).let { toJalali(it).month == 12 }) 30 else 29
    }

    fun format(date: LocalDate): String {
        val j = toJalali(date)
        return "${j.day.fa()} ${monthName(j.month)} ${j.year.fa()}"
    }

    fun formatShort(date: LocalDate): String {
        val j = toJalali(date)
        return "${j.year.fa()}/${j.month.toString().padStart(2, '0').fa()}/${j.day.toString().padStart(2, '0').fa()}"
    }

    fun dayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.SATURDAY -> "شنبه"
        DayOfWeek.SUNDAY -> "یکشنبه"
        DayOfWeek.MONDAY -> "دوشنبه"
        DayOfWeek.TUESDAY -> "سه‌شنبه"
        DayOfWeek.WEDNESDAY -> "چهارشنبه"
        DayOfWeek.THURSDAY -> "پنجشنبه"
        DayOfWeek.FRIDAY -> "جمعه"
    }

    /** Iranian week order, starting on Saturday. */
    val iranianWeek: List<DayOfWeek> = listOf(
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )

    fun formatMinutesOfDay(minutes: Int): String {
        val t = LocalTime.of((minutes / 60) % 24, minutes % 60)
        return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}".fa()
    }

    fun formatDuration(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        return if (minutes >= 60) "${(minutes / 60).fa()} ساعت و ${(minutes % 60).fa()} دقیقه" else "${minutes.fa()} دقیقه"
    }
}

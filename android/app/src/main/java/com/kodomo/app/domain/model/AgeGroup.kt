package com.kodomo.app.domain.model

import java.time.LocalDate
import java.time.Period

/** The three age tiers that drive the Age-Adaptive UI Engine. */
enum class AgeGroup(val label: String, val range: IntRange) {
    TODDLER("نوپا", 0..4),
    PRIMARY_KID("کودک", 5..8),
    PRE_TEEN("نوجوان", 9..18),
    ;

    companion object {
        /** «مدرسه من» activates automatically from this age. */
        const val SCHOOL_MODULE_MIN_AGE = 7

        fun ageInYears(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Int =
            Period.between(birthDate, today).years.coerceAtLeast(0)

        fun fromAge(age: Int): AgeGroup = when {
            age <= 4 -> TODDLER
            age <= 8 -> PRIMARY_KID
            else -> PRE_TEEN
        }
    }
}

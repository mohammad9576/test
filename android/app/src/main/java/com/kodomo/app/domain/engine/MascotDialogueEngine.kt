package com.kodomo.app.domain.engine

import com.kodomo.app.domain.model.AgeGroup
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.MascotMood
import java.time.LocalTime
import kotlin.random.Random

/**
 * [text] is shown on screen (personalised with the child's name); [speech] is the spoken variant. Spoken variants never
 * contain names so that each of them has a pre-recorded narration clip and works on phones without Persian TTS.
 */
data class MascotLine(val text: String, val mood: MascotMood, val speech: List<String> = listOf(text))

/** Generates the Kodomo robot's context-aware lines: greetings by time of day & interests, gentle time warnings. */
class MascotDialogueEngine(private val random: Random = Random.Default) {

    private fun saluteFor(hour: Int): Pair<String, MascotMood> = when (hour) {
        in 5..10 -> "صبح بخیر دوست من! ☀️" to MascotMood.EXCITED
        in 11..13 -> "ظهرت بخیر! ناهار خوشمزه خوردی؟" to MascotMood.HAPPY
        in 14..17 -> "عصر بخیر! آماده‌ی یک ماجراجویی تازه‌ای؟" to MascotMood.EXCITED
        in 18..20 -> "شبت بخیر! امروز چه کارهای قشنگی کردی؟" to MascotMood.HAPPY
        else -> "دیر وقته... من کم‌کم خوابم میاد 😴" to MascotMood.SLEEPY
    }

    fun greeting(child: Child, now: LocalTime = LocalTime.now()): MascotLine {
        val (salute, mood) = saluteFor(now.hour)
        val hello = "سلام ${child.name} جان!"
        if (child.ageGroup() == AgeGroup.TODDLER) return MascotLine("$hello $salute", mood, listOf(salute))
        val suggestion = interestSuggestion(child)
        return MascotLine("$hello $salute $suggestion", mood, listOf(salute, suggestion))
    }

    fun interestSuggestion(child: Child): String {
        val interest = child.interests.takeIf { it.isNotEmpty() }?.random(random) ?: return GENERIC_SUGGESTION
        return suggestions.getValue(interest)
    }

    fun fiveMinuteWarning(child: Child): MascotLine {
        val speech = warnings.random(random)
        return MascotLine("${child.name} جان، $speech", MascotMood.THINKING, listOf(speech))
    }

    fun timeIsUp(child: Child): MascotLine = MascotLine("آفرین ${child.name}! $TIME_UP", MascotMood.SLEEPY, listOf(TIME_UP))

    fun bedtime(child: Child): MascotLine = MascotLine("شب بخیر ${child.name} جان 🌙 $BEDTIME", MascotMood.SLEEPY, listOf(BEDTIME))

    fun celebration(child: Child, reason: String): MascotLine = MascotLine("هورااا ${child.name}! $reason 🎉", MascotMood.EXCITED, listOf("هورااا!", reason))

    fun thinking(): MascotLine = MascotLine("بذار فکر کنم... 🤔", MascotMood.THINKING)

    companion object {
        const val GENERIC_SUGGESTION = "بیا با هم یک چیز جدید کشف کنیم!"
        const val TIME_UP = "امروز کلی چیز یاد گرفتی. حالا وقت استراحت چشم‌هاست. فردا دوباره می‌بینمت! 💚"
        const val BEDTIME = "وقت خوابه. خواب‌های رنگی ببینی!"

        val suggestions: Map<Interest, String> = mapOf(
            Interest.SPORT to "امروز یک بازی حرکتی باحال برات دارم! 🏃",
            Interest.ASTRONOMY to "می‌دونستی روی سیاره‌ی زهره یک روز از یک سال طولانی‌تره؟ بیا در مرورگر امن ببینیم! 🪐",
            Interest.ART to "یک کاردستی و نقاشی رنگارنگ منتظر توئه! 🎨",
            Interest.GAMES to "یک بازی فکری تازه پیدا کردم! 🧩",
            Interest.SCIENCE to "بیا یک آزمایش علمی ساده انجام بدیم! 🔬",
            Interest.ANIMALS to "حیوانات دوست‌داشتنی در بخش کشف منتظرتند! 🦊",
            Interest.MUSIC to "یک آهنگ شاد برات آماده کردم! 🎵",
            Interest.BOOKS to "یک کتاب قصه‌ی جدید رسیده! 📚",
            Interest.MOVIES to "یک انیمیشن قشنگ برای امروز داریم! 🎬",
        )

        val warnings = listOf(
            "فقط ۵ دقیقه‌ی دیگه با هم وقت داریم. بیا کار فعلی‌مون رو قشنگ تموم کنیم! 🌟",
            "۵ دقیقه مونده! بعدش وقت بازی و استراحت بیرون از گوشیه 🌳",
        )

        /** Every sentence the robot can say aloud – exported at build time for narration recording. */
        fun allSpeechPhrases(): List<String> =
            listOf(5, 11, 14, 18, 23).map { saluteForStatic(it) } +
                suggestions.values + GENERIC_SUGGESTION + warnings + TIME_UP + BEDTIME + "هورااا!" + "بذار فکر کنم... 🤔"

        private fun saluteForStatic(hour: Int): String = MascotDialogueEngine().saluteFor(hour).first
    }
}

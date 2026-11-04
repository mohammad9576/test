package com.kodomo.app.domain.engine

import com.kodomo.app.domain.model.AgeGroup
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.MascotMood
import java.time.LocalTime
import kotlin.random.Random

data class MascotLine(val text: String, val mood: MascotMood)

/** Generates the Kodomo robot's context-aware lines: greetings by time of day & interests, gentle time warnings. */
class MascotDialogueEngine(private val random: Random = Random.Default) {

    fun greeting(child: Child, now: LocalTime = LocalTime.now()): MascotLine {
        val name = child.name
        val (salute, mood) = when (now.hour) {
            in 5..10 -> "صبح بخیر $name جان! ☀️" to MascotMood.EXCITED
            in 11..13 -> "ظهرت بخیر $name! ناهار خوشمزه خوردی؟" to MascotMood.HAPPY
            in 14..17 -> "عصر بخیر $name! آماده‌ی یک ماجراجویی تازه‌ای؟" to MascotMood.EXCITED
            in 18..20 -> "شبت بخیر $name! امروز چه کارهای قشنگی کردی؟" to MascotMood.HAPPY
            else -> "$name جان، دیر وقته... من کم‌کم خوابم میاد 😴" to MascotMood.SLEEPY
        }
        val suggestion = interestSuggestion(child)
        return MascotLine(if (child.ageGroup() == AgeGroup.TODDLER) salute else "$salute $suggestion".trim(), mood)
    }

    fun interestSuggestion(child: Child): String {
        val interest = child.interests.takeIf { it.isNotEmpty() }?.random(random) ?: return "بیا با هم یک چیز جدید کشف کنیم!"
        return when (interest) {
            Interest.SPORT -> "امروز یک بازی حرکتی باحال برات دارم! 🏃"
            Interest.ASTRONOMY -> "می‌دونستی روی سیاره‌ی زهره یک روز از یک سال طولانی‌تره؟ بیا در مرورگر امن ببینیم! 🪐"
            Interest.ART -> "یک کاردستی رنگارنگ منتظر توئه! 🎨"
            Interest.GAMES -> "یک بازی فکری تازه پیدا کردم! 🧩"
            Interest.SCIENCE -> "بیا یک آزمایش علمی ساده انجام بدیم! 🔬"
            Interest.ANIMALS -> "حیوانات دوست‌داشتنی در بخش کشف منتظرتند! 🦊"
            Interest.MUSIC -> "یک آهنگ شاد برات آماده کردم! 🎵"
            Interest.BOOKS -> "یک کتاب قصه‌ی جدید رسیده! 📚"
            Interest.MOVIES -> "یک انیمیشن قشنگ برای امروز داریم! 🎬"
        }
    }

    fun fiveMinuteWarning(child: Child): MascotLine = MascotLine(
        listOf(
            "${child.name} جان، فقط ۵ دقیقه‌ی دیگه با هم وقت داریم. بیا کار فعلی‌مون رو قشنگ تموم کنیم! 🌟",
            "دوست خوبم ${child.name}، ۵ دقیقه مونده! بعدش وقت بازی و استراحت بیرون از گوشیه 🌳",
        ).random(random),
        MascotMood.THINKING,
    )

    fun timeIsUp(child: Child): MascotLine = MascotLine(
        "آفرین ${child.name}! امروز کلی چیز یاد گرفتی. حالا وقت استراحت چشم‌هاست. فردا دوباره می‌بینمت! 💚",
        MascotMood.SLEEPY,
    )

    fun bedtime(child: Child): MascotLine = MascotLine(
        "شب بخیر ${child.name} جان 🌙 وقت خوابه. خواب‌های رنگی ببینی!",
        MascotMood.SLEEPY,
    )

    fun celebration(child: Child, reason: String): MascotLine = MascotLine("هورااا ${child.name}! $reason 🎉", MascotMood.EXCITED)

    fun thinking(): MascotLine = MascotLine("بذار فکر کنم... 🤔", MascotMood.THINKING)
}

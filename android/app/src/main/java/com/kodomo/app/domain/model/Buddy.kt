package com.kodomo.app.domain.model

/**
 * The child's companion character (shown wherever the mascot appears). Original Kodomo characters – the robot is the
 * default; the child may swap it for an animal friend they like.
 */
enum class Buddy(val title: String, val emoji: String, val tagline: String, val colors: Pair<Long, Long>) {
    ROBOT("بیپ، ربات کدومو", "🤖", "عاشق کشف و آزمایش!", 0xFFB9E4CBL to 0xFF2E7D5BL),
    PANDA("پاپو، پاندای ورزشکار", "🐼", "هر روز تمرین می‌کنم و قوی‌تر می‌شوم!", 0xFFE8E8E8L to 0xFF555555L),
    LAMB("پشمالو، بره‌ی مهربان", "🐑", "با هم مهربانی را یاد بگیریم!", 0xFFFFF4E0L to 0xFFE0B27AL),
    DOLPHIN("دلی، دلفین شاد", "🐬", "بیا در دریای دانستنی‌ها شنا کنیم!", 0xFFBFE3FFL to 0xFF1E88E5L),
    LION("شیرو، شیر شجاع", "🦁", "شجاع باش، من کنارت هستم!", 0xFFFFE6A7L to 0xFFFFB648L),
    BUNNY("خرگوشک تیزپا", "🐰", "بپر و بازی کن!", 0xFFFFD1DCL to 0xFFF28AA6L),
    DRAGON("اژی، اژدهای کوچولو", "🐲", "رؤیاهای بزرگ ببین!", 0xFFC8F7C5L to 0xFF43A047L),
    CAT("پیشی، گربه‌ی کنجکاو", "🐱", "همه چیز را می‌خواهم بدانم!", 0xFFFFE0B2L to 0xFFFB8C00L),
    UNICORN("رنگین‌کمان، اسب تک‌شاخ", "🦄", "با رنگ‌ها جادو کنیم!", 0xFFE2D6FFL to 0xFF9B7FE6L),
    OWL("هوهو، جغد دانا", "🦉", "سؤال بپرس، با هم جواب را پیدا کنیم!", 0xFFD7CCC8L to 0xFF6D4C41L),
}

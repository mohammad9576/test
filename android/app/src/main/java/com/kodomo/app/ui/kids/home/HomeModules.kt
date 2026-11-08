package com.kodomo.app.ui.kids.home

import com.kodomo.app.domain.model.AgeGroup
import com.kodomo.app.domain.model.Child
import com.kodomo.app.ui.navigation.Routes

/** A section of the kids home. [key] is what parents switch on/off per child. */
data class HomeModule(
    val key: String,
    val title: String,
    val emoji: String,
    val colors: Pair<Long, Long>,
    val route: String,
    val hint: String,
)

object HomeModules {
    /** Every section that suits the child's age, in home-screen order (before the parent's choices). */
    fun forChild(child: Child): List<HomeModule> {
        val id = child.id
        val group = child.ageGroup()
        val toddler = group == AgeGroup.TODDLER
        return buildList {
            add(HomeModule("videos", "فیلم و کارتون", "🎬", 0xFF9ED8FF to 0xFF3D8BD9, Routes.kidVideos(id), "کارتون‌های تأییدشده از تلوبیون، آپارات کودک، فیلیمو، نماوا و…"))
            add(HomeModule("games", "بازی", "🤸", 0xFFFFB5C8 to 0xFFE45D8C, Routes.kidGames(id), "بازی با بدن، فکری، نقاشی و رنگ‌آمیزی"))
            add(HomeModule("books", "کتاب", "📚", 0xFFFFD98A to 0xFFF29D1F, Routes.kidBooks(id), "کتاب‌های مصور، صوتی و کتاب‌های طاقچه و فیدیبو"))
            add(HomeModule("study", "درس و تمرین", "📝", 0xFFFFF08A to 0xFFE8B500, Routes.kidStudy(id), "درس‌های هر پایه با آزمون و جایزه"))
            add(HomeModule("english", "انگلیسی", "🔤", 0xFFCDB8FF to 0xFF7650D8, Routes.kidEnglish(id), "انگلیسی از صفر با تلفظ"))
            add(HomeModule("livetv", "شبکه پویا", "📺", 0xFFFFCB94 to 0xFFF07A12, Routes.kidLiveTv(id), "پخش زنده‌ی شبکه‌ی کودک"))
            add(HomeModule("music", "موسیقی", "🎵", 0xFFD9CCFF to 0xFF8E6BE8, Routes.kidMusic(id), "لالایی، جعبه موسیقی و زایلوفون"))
            if (!toddler) add(HomeModule("discover", "تفکر و یادگیری", "🔬", 0xFFA8E6C3 to 0xFF35A869, Routes.kidDiscover(id), "مهارت‌های زندگی، دانستنی‌ها و دانشنامه"))
            add(HomeModule("crafts", "کاردستی", "✂️", 0xFFF7CBA6 to 0xFFD9824A, Routes.kidCrafts(id), "اوریگامی و کاردستی قدم‌به‌قدم"))
            add(HomeModule("camera", "دوربین من", "📸", 0xFFB3E5FC to 0xFF0288D1, Routes.kidCamera(id), "عکس و فیلم با گالری مخصوص کودک"))
            add(HomeModule("call", "تماس", "📞", 0xFFC8F0C8 to 0xFF2E9E4F, Routes.kidCall(id), "زنگ زدن به مخاطبان تأییدشده"))
            if (!toddler) add(HomeModule("planner", "برنامه‌ی من", "🗓️", 0xFFC9D8F2 to 0xFF3F62A8, Routes.kidSchool(id), "برنامه‌ی درسی و روزانه، تکلیف و یادآور"))
            add(HomeModule("chores", "کارهای من", "🧹", 0xFFD6EDB8 to 0xFF6BAA2C, Routes.kidChores(id), "کارهای خوب ستاره و وقت می‌آورند، کارهای بد کم می‌کنند"))
            add(HomeModule("apps", "برنامه‌های من", "📱", 0xFFDCD0FF to 0xFF6F4BC4, Routes.kidApps(id), "برنامه‌هایی که والدین اجازه داده‌اند"))
            add(HomeModule("rewards", "جایزه‌ها", "🏆", 0xFFFFEDA8 to 0xFFF2B705, Routes.kidRewards(id), "ستاره، مدال و جایزه‌ی خانوادگی"))
            if (!toddler) add(HomeModule("interests", "علایق من", "💚", 0xFFCFF2DD to 0xFF2F9E62, Routes.kidInterests(id), "پیشنهادهای مخصوص علاقه‌ها"))
        }
    }

    /** Sections hidden by default until a parent turns them on. */
    val hiddenByDefault = setOf("apps", "interests")
}

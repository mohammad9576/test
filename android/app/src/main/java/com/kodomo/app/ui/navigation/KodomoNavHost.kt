package com.kodomo.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kodomo.app.domain.model.MotionGame
import com.kodomo.app.ui.kids.KidSpaceFrame
import com.kodomo.app.ui.kids.books.BookListScreen
import com.kodomo.app.ui.kids.books.BookReaderScreen
import com.kodomo.app.ui.kids.browser.EncyclopediaArticleScreen
import com.kodomo.app.ui.kids.browser.SafeBrowserScreen
import com.kodomo.app.ui.kids.apps.MyAppsScreen
import com.kodomo.app.ui.kids.explore.ExploreDetailScreen
import com.kodomo.app.ui.kids.explore.ExploreScreen
import com.kodomo.app.ui.kids.games.GamesScreen
import com.kodomo.app.ui.kids.games.MotionGameScreen
import com.kodomo.app.ui.kids.home.InterestsScreen
import com.kodomo.app.ui.kids.home.KidHomeScreen
import com.kodomo.app.ui.kids.learning.LearningScreen
import com.kodomo.app.ui.kids.learning.LessonScreen
import com.kodomo.app.ui.kids.music.MusicScreen
import com.kodomo.app.ui.kids.projects.ProjectWizardScreen
import com.kodomo.app.ui.kids.projects.ProjectsScreen
import com.kodomo.app.ui.kids.rewards.RewardsScreen
import com.kodomo.app.ui.kids.school.SchoolScreen
import com.kodomo.app.ui.kids.video.FeedbackScreen
import com.kodomo.app.ui.kids.video.VideoListScreen
import com.kodomo.app.ui.kids.video.VideoPlayerScreen
import com.kodomo.app.ui.onboarding.OnboardingScreen
import com.kodomo.app.ui.parent.ParentHubScreen
import com.kodomo.app.ui.parent.apps.AllowedAppsScreen
import com.kodomo.app.ui.parent.cloud.CloudAccountScreen
import com.kodomo.app.ui.parent.community.ForumTopicScreen
import com.kodomo.app.ui.parent.community.ParentArticleScreen
import com.kodomo.app.ui.parent.family.ChildEditorScreen
import com.kodomo.app.ui.parent.family.MemoriesGalleryScreen
import com.kodomo.app.ui.parent.ratings.ContentPassportScreen
import com.kodomo.app.ui.profiles.ProfilePickerScreen

@Composable
fun KodomoNavHost(navController: NavHostController, startDestination: String) {
    val back: () -> Unit = { navController.popBackStack() }
    val toProfiles: () -> Unit = {
        navController.navigate(Routes.PROFILES) { popUpTo(0) { inclusive = true } }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(380)) { -it / 3 } +
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(380)) +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(380), initialScale = 0.94f)
        },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)) },
        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(300), initialScale = 1.04f) },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(300)) { it / 3 } +
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300))
        },
    ) {
        composable(Routes.ONBOARDING) { OnboardingScreen(onFinished = toProfiles) }

        composable(Routes.PROFILES) {
            ProfilePickerScreen(
                onChild = { id -> navController.navigate(Routes.kidHome(id)) { popUpTo(Routes.PROFILES) } },
                onParentVerified = { navController.navigate(Routes.PARENT_HUB) },
            )
        }

        // ---------------- Kids space ----------------
        kid(Routes.KID_HOME, toProfiles) { childId, _ ->
            KidHomeScreen(
                onOpen = { route -> navController.navigate(route) },
                childId = childId,
            )
        }
        kid(Routes.KID_INTERESTS, toProfiles) { _, _ -> InterestsScreen(onBack = back) }
        kid(Routes.KID_VIDEOS, toProfiles) { childId, _ ->
            VideoListScreen(onBack = back, onPlay = { id -> navController.navigate(Routes.kidVideo(childId, id)) })
        }
        kid(Routes.KID_VIDEO_PLAYER, toProfiles, listOf(stringArg(Routes.ARG_ID))) { childId, entry ->
            val id = entry.stringArg(Routes.ARG_ID)
            VideoPlayerScreen(
                contentId = id,
                onBack = back,
                onFinished = { navController.navigate(Routes.kidFeedback(childId, id)) { popUpTo(Routes.KID_VIDEOS) } },
            )
        }
        kid(Routes.KID_FEEDBACK, toProfiles, listOf(stringArg(Routes.ARG_ID))) { _, entry ->
            FeedbackScreen(contentId = entry.stringArg(Routes.ARG_ID), onDone = back)
        }
        kid(Routes.KID_LIVE_TV, toProfiles) { _, _ -> com.kodomo.app.ui.kids.video.LiveTvScreen(onBack = back) }
        kid(Routes.KID_GAMES, toProfiles) { childId, _ ->
            GamesScreen(onBack = back, onPlay = { game -> navController.navigate(Routes.kidGame(childId, game)) })
        }
        kid(Routes.KID_GAME, toProfiles, listOf(stringArg(Routes.ARG_ID))) { _, entry ->
            val game = MotionGame.valueOf(entry.stringArg(Routes.ARG_ID))
            MotionGameScreen(game = game, onBack = back)
        }
        kid(Routes.KID_BOOKS, toProfiles) { childId, _ ->
            BookListScreen(onBack = back, onOpen = { id, mode -> navController.navigate(Routes.kidBook(childId, id, mode.name)) })
        }
        kid(Routes.KID_BOOK_READER, toProfiles, listOf(stringArg(Routes.ARG_ID), stringArg("mode"))) { _, entry ->
            BookReaderScreen(
                contentId = entry.stringArg(Routes.ARG_ID),
                mode = ReadingMode.valueOf(entry.stringArg("mode")),
                onBack = back,
            )
        }
        kid(Routes.KID_MUSIC, toProfiles) { _, _ -> MusicScreen(onBack = back) }
        kid(Routes.KID_LEARNING, toProfiles) { childId, _ ->
            LearningScreen(onBack = back, onOpen = { id -> navController.navigate(Routes.kidLesson(childId, id)) })
        }
        kid(Routes.KID_LESSON, toProfiles, listOf(stringArg(Routes.ARG_ID))) { _, entry ->
            LessonScreen(contentId = entry.stringArg(Routes.ARG_ID), onBack = back)
        }
        kid(Routes.KID_SCHOOL, toProfiles) { _, _ -> SchoolScreen(onBack = back) }
        kid(Routes.KID_EXPLORE, toProfiles) { childId, _ ->
            ExploreScreen(onBack = back, onOpen = { id -> navController.navigate(Routes.kidExploreItem(childId, id)) })
        }
        kid(Routes.KID_EXPLORE_ITEM, toProfiles, listOf(stringArg(Routes.ARG_ID))) { _, entry ->
            ExploreDetailScreen(contentId = entry.stringArg(Routes.ARG_ID), onBack = back)
        }
        kid(Routes.KID_APPS, toProfiles) { _, _ -> MyAppsScreen(onBack = back) }
        kid(Routes.KID_BROWSER, toProfiles) { childId, _ ->
            SafeBrowserScreen(onBack = back, onOpen = { id -> navController.navigate(Routes.kidArticle(childId, id)) })
        }
        kid(Routes.KID_ARTICLE, toProfiles, listOf(navArgument(Routes.ARG_ID) { type = NavType.IntType })) { _, entry ->
            EncyclopediaArticleScreen(articleId = entry.arguments?.getInt(Routes.ARG_ID) ?: 0, onBack = back)
        }
        kid(Routes.KID_PROJECTS, toProfiles) { childId, _ ->
            ProjectsScreen(onBack = back, onOpen = { id -> navController.navigate(Routes.kidProject(childId, id)) })
        }
        kid(Routes.KID_PROJECT, toProfiles, listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType })) { _, entry ->
            ProjectWizardScreen(projectId = entry.arguments?.getLong(Routes.ARG_ID) ?: 0L, onBack = back)
        }
        kid(Routes.KID_REWARDS, toProfiles) { _, _ -> RewardsScreen(onBack = back) }

        // ---------------- Parent hub ----------------
        composable(Routes.PARENT_HUB) {
            ParentHubScreen(
                onExit = toProfiles,
                onOpenContent = { id -> navController.navigate(Routes.parentContent(id)) },
                onOpenArticle = { id -> navController.navigate(Routes.parentArticle(id)) },
                onOpenTopic = { id -> navController.navigate(Routes.parentTopic(id)) },
                onEditChild = { id -> navController.navigate(Routes.parentChildEdit(id)) },
                onAddChild = { navController.navigate(Routes.PARENT_ADD_CHILD) },
                onOpenGallery = { navController.navigate(Routes.PARENT_GALLERY) },
                onOpenCloud = { navController.navigate(Routes.PARENT_CLOUD) },
                onOpenAllowedApps = { navController.navigate(Routes.PARENT_ALLOWED_APPS) },
                onOpenTelewebion = { navController.navigate(Routes.PARENT_TELEWEBION) },
            )
        }
        composable(Routes.PARENT_TELEWEBION) { com.kodomo.app.ui.parent.ratings.TelewebionPickerScreen(onBack = back) }
        composable(Routes.PARENT_CONTENT, arguments = listOf(stringArg(Routes.ARG_ID))) { entry ->
            ContentPassportScreen(contentId = entry.stringArg(Routes.ARG_ID), onBack = back)
        }
        composable(Routes.PARENT_ARTICLE, arguments = listOf(stringArg(Routes.ARG_ID))) { entry ->
            ParentArticleScreen(contentId = entry.stringArg(Routes.ARG_ID), onBack = back)
        }
        composable(Routes.PARENT_TOPIC, arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType })) { entry ->
            ForumTopicScreen(topicId = entry.arguments?.getLong(Routes.ARG_ID) ?: 0L, onBack = back)
        }
        composable(Routes.PARENT_CHILD_EDIT, arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType })) { entry ->
            ChildEditorScreen(childId = entry.arguments?.getLong(Routes.ARG_ID), onBack = back)
        }
        composable(Routes.PARENT_ADD_CHILD) { ChildEditorScreen(childId = null, onBack = back) }
        composable(Routes.PARENT_GALLERY) { MemoriesGalleryScreen(onBack = back) }
        composable(Routes.PARENT_CLOUD) { CloudAccountScreen(onBack = back) }
        composable(Routes.PARENT_ALLOWED_APPS) { AllowedAppsScreen(onBack = back) }
    }
}

private fun stringArg(name: String): NamedNavArgument = navArgument(name) { type = NavType.StringType }

private fun NavBackStackEntry.stringArg(name: String): String = Uri.decode(arguments?.getString(name).orEmpty())

private fun NavGraphBuilder.kid(
    route: String,
    onParentExit: () -> Unit,
    extraArgs: List<NamedNavArgument> = emptyList(),
    content: @Composable (childId: Long, entry: NavBackStackEntry) -> Unit,
) {
    composable(route, arguments = listOf(navArgument(Routes.ARG_CHILD) { type = NavType.LongType }) + extraArgs) { entry ->
        val childId = entry.arguments?.getLong(Routes.ARG_CHILD) ?: 0L
        KidSpaceFrame(childId = childId, onParentExit = onParentExit) { content(childId, entry) }
    }
}

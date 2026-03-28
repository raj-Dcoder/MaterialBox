package com.rajveer.materialbox.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rajveer.materialbox.ui.screens.addmaterial.AddMaterialScreen
import com.rajveer.materialbox.ui.screens.addsubject.AddSubjectScreen
import com.rajveer.materialbox.ui.screens.addtopic.AddTopicScreen
import com.rajveer.materialbox.ui.screens.addyoutubefeed.AddYoutubeFeedScreen
import com.rajveer.materialbox.ui.screens.home.HomeScreen
import com.rajveer.materialbox.ui.screens.materialdetail.MaterialDetailScreen
import com.rajveer.materialbox.ui.screens.roadmap.RoadmapScreen
import com.rajveer.materialbox.ui.screens.splash.SplashScreen
import com.rajveer.materialbox.ui.screens.subjectdetail.SubjectDetailScreen
import com.rajveer.materialbox.ui.screens.topicdetail.TopicDetailScreen
import com.rajveer.materialbox.ui.screens.youtubefeeddetail.YoutubeFeedDetailScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            )
        },
        // Going BACK: no animation = nothing to preview during swipe
        // Back only triggers when you lift your finger (predictive back)
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(
            route = Screen.Splash.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(
            route = Screen.SubjectDetail.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
            SubjectDetailScreen(
                navController = navController,
                subjectId = subjectId
            )
        }

        composable(Screen.AddSubject.route) {
            AddSubjectScreen(navController)
        }

        composable(
            route = Screen.Roadmap.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
            RoadmapScreen(
                navController = navController,
                subjectId = subjectId
            )
        }

        composable(
            route = Screen.AddTopic.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
            AddTopicScreen(
                navController = navController,
                subjectId = subjectId
            )
        }

        composable(
            route = Screen.TopicDetail.route,
            arguments = listOf(navArgument("topicId") { type = NavType.LongType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getLong("topicId") ?: return@composable
            TopicDetailScreen(
                navController = navController,
                topicId = topicId
            )
        }

        composable(
            route = Screen.AddMaterial.route,
            arguments = listOf(
                navArgument("topicId") { type = NavType.LongType },
                navArgument("materialType") { type = NavType.StringType; nullable = true },
            )
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getLong("topicId") ?: return@composable
            val materialType = backStackEntry.arguments?.getString("materialType")
            AddMaterialScreen(
                navController = navController,
                materialType = materialType
            )
        }

        composable(
            route = Screen.MaterialDetail.route,
            arguments = listOf(navArgument("materialId") { type = NavType.LongType })
        ) { backStackEntry ->
            val materialId = backStackEntry.arguments?.getLong("materialId") ?: return@composable
            MaterialDetailScreen(
                navController = navController,
                materialId = materialId
            )
        }

        composable(
            route = Screen.AddYoutubeFeed.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { 
            AddYoutubeFeedScreen(navController = navController)
        }

        composable(
            route = Screen.YoutubeFeedDetail.route,
            arguments = listOf(navArgument("feedId") { type = NavType.LongType })
        ) { 
            YoutubeFeedDetailScreen(
                navController = navController
            )
        }
    }
} 
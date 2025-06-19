package com.rajveer.materialbox.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rajveer.materialbox.ui.screens.addmaterial.AddMaterialScreen
import com.rajveer.materialbox.ui.screens.addsubject.AddSubjectScreen
import com.rajveer.materialbox.ui.screens.addtopic.AddTopicScreen
import com.rajveer.materialbox.ui.screens.home.HomeScreen
import com.rajveer.materialbox.ui.screens.materialdetail.MaterialDetailScreen
import com.rajveer.materialbox.ui.screens.subjectdetail.SubjectDetailScreen
import com.rajveer.materialbox.ui.screens.subjectdetail.SubjectDetailViewModel
import com.rajveer.materialbox.ui.screens.topicdetail.TopicDetailScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
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
                topicId = topicId,
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
    }
} 
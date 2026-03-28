package com.rajveer.materialbox.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object AddSubject : Screen("add_subject")
    
    object SubjectDetail : Screen("subject/{subjectId}") {
        fun createRoute(subjectId: Long) = "subject/$subjectId"
    }

    object Roadmap : Screen("roadmap/{subjectId}") {
        fun createRoute(subjectId: Long) = "roadmap/$subjectId"
    }
    
    object AddTopic : Screen("add_topic/{subjectId}") {
        fun createRoute(subjectId: Long) = "add_topic/$subjectId"
    }
    
    object TopicDetail : Screen("topic/{topicId}") {
        fun createRoute(topicId: Long) = "topic/$topicId"
    }
    
    object AddMaterial : Screen("add_material/{topicId}?materialType={materialType}&filePath={filePath}") {
        fun createRoute(topicId: Long) = "add_material/$topicId"
        fun createRoute(topicId: Long, materialType: String, filePath: String) = "add_material/$topicId?materialType=$materialType&filePath=${Uri.encode(filePath)}"
    }
    
    object MaterialDetail : Screen("material/{materialId}") {
        fun createRoute(materialId: Long) = "material/$materialId"
    }

    object AddYoutubeFeed : Screen("add_youtube_feed/{subjectId}") {
        fun createRoute(subjectId: Long) = "add_youtube_feed/$subjectId"
    }

    object YoutubeFeedDetail : Screen("youtube_feed/{feedId}") {
        fun createRoute(feedId: Long) = "youtube_feed/$feedId"
    }
}
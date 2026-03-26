package com.rajveer.materialbox.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.R
import com.rajveer.materialbox.navigation.Screen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()  // triggers background data preloading
) {

    // Scale animation: 0.6 → 1.0
    val scale = remember { Animatable(0.6f) }
    // Fade animation: 0 → 1
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Run scale + fade concurrently using coroutineScope
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                )
            }
        }

        // Hold at full opacity briefly
        delay(900)

        // Fade out before navigating
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = LinearEasing)
        )

        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "MaterialBox",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your study companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

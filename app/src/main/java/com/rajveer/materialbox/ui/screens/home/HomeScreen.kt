package com.rajveer.materialbox.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.MaterialCard
import com.rajveer.materialbox.ui.components.SubjectCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val subjects by viewModel.subjects.collectAsState()
    val materials by viewModel.materials.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MaterialBox") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddSubject.route) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(subjects) { subject ->
                SubjectCard(
                    subject = subject,
                    onClick = {
                        navController.navigate(Screen.SubjectDetail.createRoute(subject.id))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recent Materials",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(materials) { material ->
                MaterialCard(
                    material = material,
                    onClick = { 
                        if (material.type == MaterialType.LINK) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.pathOrUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            navController.navigate(Screen.MaterialDetail.createRoute(material.id))
                        }
                    },
                    onLongPress = {},
                    onDelete = {}
                )
            }
        }
    }
} 
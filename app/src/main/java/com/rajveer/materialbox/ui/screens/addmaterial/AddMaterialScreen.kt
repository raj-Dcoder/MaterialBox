package com.rajveer.materialbox.ui.screens.addmaterial

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.MaterialType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaterialScreen(
    navController: NavController,
    viewModel: AddMaterialViewModel = hiltViewModel(),
    topicId: Long,
    materialType: String? = null
) {
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMaterialType by viewModel.type.collectAsState()

    LaunchedEffect(materialType) {
        if (materialType != null) {
            viewModel.setType(MaterialType.valueOf(materialType))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Material") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveMaterial { navController.navigateUp() } },
                        enabled = title.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                label = { Text(if (currentMaterialType == MaterialType.NOTE) "Content" else "URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

 
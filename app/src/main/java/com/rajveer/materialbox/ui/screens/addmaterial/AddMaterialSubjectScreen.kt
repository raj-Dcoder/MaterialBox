package com.rajveer.materialbox.ui.screens.addmaterial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rajveer.materialbox.util.HapticUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.MaterialType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaterialSubjectScreen(
    navController: NavController,
    viewModel: AddMaterialSubjectViewModel = hiltViewModel(),
    materialType: String? = null
) {
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMaterialType by viewModel.type.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(materialType) {
        if (materialType != null) {
            viewModel.setType(MaterialType.valueOf(materialType))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Material") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            HapticUtils.playHeavyClick(context)
                            viewModel.saveMaterial { navController.navigateUp() }
                        },
                        enabled = title.isNotBlank() && !isLoading &&
                                (currentMaterialType != MaterialType.LINK || content.isNotBlank())
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                label = { Text(if (currentMaterialType == MaterialType.NOTE) "Markdown content" else "URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (currentMaterialType == MaterialType.NOTE) 220.dp else 56.dp)
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

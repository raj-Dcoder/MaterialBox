package com.rajveer.materialbox.ui.screens.addyoutubefeed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.util.HapticUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddYoutubeFeedScreen(
    navController: NavController,
    viewModel: AddYoutubeFeedViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var channelUrls by remember { mutableStateOf(mutableListOf("")) }
    val channelNames by viewModel.channelNames.collectAsState()
    val context = LocalContext.current

    // Observe changes to channelUrls to trigger resolution
    LaunchedEffect(channelUrls) {
        channelUrls.forEach { url ->
            if (url.isNotBlank()) {
                viewModel.resolveChannelName(url)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add YouTube Feed") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (name.isNotBlank() && channelUrls.any { it.isNotBlank() }) {
                        HapticUtils.playClick(context)
                        viewModel.addYoutubeFeed(
                            name = name,
                            channelUrls = channelUrls.filter { it.isNotBlank() },
                            onComplete = { navController.navigateUp() }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && channelUrls.any { it.isNotBlank() }
            ) {
                Text("Add Feed")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Feed Name (e.g., Android Dev)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text(
                    text = "Channel URLs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(channelUrls) { index, url ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { newUrl ->
                                val newList = channelUrls.toMutableList()
                                newList[index] = newUrl
                                channelUrls = newList
                            },
                            label = { Text("Channel or Playlist URL") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Channel or playlist link") }
                        )
                        if (channelUrls.size > 1) {
                            IconButton(onClick = {
                                val newList = channelUrls.toMutableList()
                                newList.removeAt(index)
                                channelUrls = newList
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                    
                    val nameResolved = channelNames[url]
                    if (nameResolved != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = nameResolved,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = {
                        val newList = channelUrls.toMutableList()
                        newList.add("")
                        channelUrls = newList
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Another Channel")
                }
            }
        }
    }
}

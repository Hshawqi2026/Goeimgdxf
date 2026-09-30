package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ProjectRepository
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.HomeViewModel

sealed class AppScreen {
    data object Home : AppScreen()
    data class Editor(
        val projectId: String? = null,
        val initialImageUri: Uri? = null,
        val loadSample: Boolean = false
    ) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ProjectRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
                    val homeViewModel = remember { HomeViewModel(repository) }
                    val recentProjects by homeViewModel.recentProjects.collectAsStateWithLifecycle()

                    when (val screen = currentScreen) {
                        is AppScreen.Home -> {
                            HomeScreen(
                                recentProjects = recentProjects,
                                onOpenProject = { id ->
                                    currentScreen = AppScreen.Editor(projectId = id)
                                },
                                onNewProject = {
                                    currentScreen = AppScreen.Editor()
                                },
                                onImportImage = { uri ->
                                    currentScreen = AppScreen.Editor(initialImageUri = uri)
                                },
                                onLoadSample = {
                                    currentScreen = AppScreen.Editor(loadSample = true)
                                },
                                onDeleteProject = { id ->
                                    homeViewModel.deleteProject(id)
                                }
                            )
                        }
                        is AppScreen.Editor -> {
                            val editorViewModel = remember(screen.projectId, screen.initialImageUri, screen.loadSample) {
                                val vm = EditorViewModel(repository, screen.projectId)
                                if (screen.initialImageUri != null) {
                                    vm.importImageFromUri(this@MainActivity, screen.initialImageUri)
                                } else if (screen.loadSample) {
                                    vm.loadSampleAerial(this@MainActivity)
                                }
                                vm
                            }

                            EditorScreen(
                                viewModel = editorViewModel,
                                onNavigateBack = {
                                    currentScreen = AppScreen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

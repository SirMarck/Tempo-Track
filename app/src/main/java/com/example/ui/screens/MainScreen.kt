package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.MiniTimerBar
import com.example.ui.theme.*
import com.example.viewmodel.TimeTrackerViewModel

/**
 * Shell Principal de Navegação (Fase 3 & Fase 9)
 * Contém a barra inferior Tech Sóbrio, Mini Timer persistente flutuante e roteamento completo.
 */
@Composable
fun MainScreen(viewModel: TimeTrackerViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activeSession by viewModel.activeSession.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val projects by viewModel.projects.collectAsState()

    val isFocusScreen = currentRoute == Screen.TimerFocus.route
    val isTodayRoute = currentRoute == null || currentRoute == Screen.Today.route || currentRoute == "dashboard"

    Scaffold(
        containerColor = TempoBgBase,
        bottomBar = {
            if (!isFocusScreen) {
                Column {
                    // Mini Timer Bar flutuante quando há sessão ativa APENAS em outras abas (não duplica na aba Hoje)
                    if (!isTodayRoute) {
                        MiniTimerBar(
                            session = activeSession,
                            clients = clients,
                            projects = projects,
                            onOpenFocus = {
                                navController.navigate(Screen.Today.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            onPause = { viewModel.pauseActiveSession() },
                            onResume = { viewModel.resumeActiveSession() }
                        )
                    }

                    // Barra de Navegação Inferior
                    NavigationBar(
                        containerColor = if (isTodayRoute && activeSession != null) TempoSurface1.copy(alpha = 0.82f) else TempoSurface1,
                        contentColor = TempoTextPrimary
                    ) {
                        // 1. Hoje
                        val isTodaySelected = currentRoute == Screen.Today.route || currentRoute == "dashboard"
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Schedule, contentDescription = "Hoje") },
                            label = { Text("Hoje") },
                            selected = isTodaySelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TempoAccent,
                                selectedTextColor = TempoAccent,
                                indicatorColor = TempoSurface3,
                                unselectedIconColor = TempoTextMuted,
                                unselectedTextColor = TempoTextMuted
                            ),
                            onClick = {
                                navController.navigate(Screen.Today.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )

                        // 2. Histórico
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
                            label = { Text("Histórico") },
                            selected = currentRoute == Screen.History.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TempoAccent,
                                selectedTextColor = TempoAccent,
                                indicatorColor = TempoSurface3,
                                unselectedIconColor = TempoTextMuted,
                                unselectedTextColor = TempoTextMuted
                            ),
                            onClick = {
                                navController.navigate(Screen.History.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )

                        // 3. Clientes
                        val isProjectsSelected = currentRoute == Screen.Projects.route || currentRoute == "clients" || currentRoute?.startsWith("client_detail") == true || currentRoute?.startsWith("project_detail") == true
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Clientes") },
                            label = { Text("Clientes") },
                            selected = isProjectsSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TempoAccent,
                                selectedTextColor = TempoAccent,
                                indicatorColor = TempoSurface3,
                                unselectedIconColor = TempoTextMuted,
                                unselectedTextColor = TempoTextMuted
                            ),
                            onClick = {
                                navController.navigate(Screen.Projects.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )

                        // 4. Relatórios
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Relatórios") },
                            label = { Text("Relatórios") },
                            selected = currentRoute == Screen.Reports.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TempoAccent,
                                selectedTextColor = TempoAccent,
                                indicatorColor = TempoSurface3,
                                unselectedIconColor = TempoTextMuted,
                                unselectedTextColor = TempoTextMuted
                            ),
                            onClick = {
                                navController.navigate(Screen.Reports.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.97f, animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.97f, animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
            }
        ) {
            // Rotas Principais
            composable(Screen.Today.route) {
                TodayScreen(
                    viewModel = viewModel,
                    onNavigateToFocus = { /* Desabilitado: o relógio com efeitos cósmicos fica diretamente na tela Hoje */ },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(viewModel = viewModel)
            }

            composable(Screen.Projects.route) {
                ProjectsScreen(
                    viewModel = viewModel,
                    onNavigateToProjectDetail = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    },
                    onNavigateToClientDetail = { clientId ->
                        navController.navigate(Screen.ClientDetail.createRoute(clientId))
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateToClientDetail = { clientId ->
                        navController.navigate(Screen.ClientDetail.createRoute(clientId))
                    }
                )
            }

            // Rota de Foco Total 3D (Transição Firme & Rápida Container Transform)
            composable(
                route = Screen.TimerFocus.route,
                enterTransition = {
                    scaleIn(
                        initialScale = 0.90f,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 180))
                },
                popExitTransition = {
                    scaleOut(
                        targetScale = 0.90f,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 160))
                }
            ) {
                TimerFocusScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Rotas de Detalhes
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                ProjectDetailScreen(
                    projectId = projectId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWork = { clientId, projId ->
                        viewModel.startSession(clientId = clientId, projectId = projId)
                        navController.navigate(Screen.Today.route)
                    }
                )
            }

            composable(
                route = Screen.ClientDetail.route,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                ClientDetailScreen(
                    clientId = clientId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProjectDetail = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    }
                )
            }

            // Rotas legadas para compatibilidade retroativa
            composable("dashboard") {
                TodayScreen(
                    viewModel = viewModel,
                    onNavigateToFocus = { /* Desabilitado: o relógio com efeitos cósmicos fica diretamente na tela Hoje */ },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable("clients") {
                ProjectsScreen(
                    viewModel = viewModel,
                    onNavigateToProjectDetail = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    },
                    onNavigateToClientDetail = { clientId ->
                        navController.navigate(Screen.ClientDetail.createRoute(clientId))
                    }
                )
            }
        }
    }
}

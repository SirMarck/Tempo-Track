package com.example.ui.screens

sealed class Screen(val route: String, val title: String) {
    object Today : Screen("today", "Hoje")
    object History : Screen("history", "Histórico")
    object Projects : Screen("projects", "Projetos")
    object Reports : Screen("reports", "Relatórios")
    object TimerFocus : Screen("timer_focus", "Foco")

    // Rotas de detalhe
    object ProjectDetail : Screen("project_detail/{projectId}", "Detalhe do Projeto") {
        fun createRoute(projectId: Long) = "project_detail/$projectId"
    }

    object ClientDetail : Screen("client_detail/{clientId}", "Detalhe do Cliente") {
        fun createRoute(clientId: Long) = "client_detail/$clientId"
    }

    // Compatibilidade para referências existentes
    object Dashboard : Screen("today", "Hoje")
    object Clients : Screen("projects", "Projetos")
}



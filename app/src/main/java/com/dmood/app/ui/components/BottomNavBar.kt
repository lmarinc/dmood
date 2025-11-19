package com.dmood.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.dmood.app.ui.navigation.Screen

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Hoy") }
        )

        NavigationBarItem(
            selected = currentRoute == Screen.WeeklySummary.route,
            onClick = { onNavigate(Screen.WeeklySummary.route) },
            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            label = { Text("Resumen") }
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Ajustes") }
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Faq.route,
            onClick = { onNavigate(Screen.Faq.route) },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            label = { Text("Guía") }
        )
    }
}

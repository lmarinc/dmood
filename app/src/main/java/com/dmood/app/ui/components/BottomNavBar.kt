package com.dmood.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.dmood.app.ui.navigation.Screen

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    NavigationBar(
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        tonalElevation = 0.dp // Remove shadow/elevation effect
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.onPrimaryContainer,
            selectedTextColor = colors.onPrimaryContainer,
            indicatorColor = colors.primary,
            unselectedIconColor = colors.onPrimaryContainer.copy(alpha = 0.7f),
            unselectedTextColor = colors.onPrimaryContainer.copy(alpha = 0.7f)
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Hoy") },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentRoute == Screen.WeeklySummary.route,
            onClick = { onNavigate(Screen.WeeklySummary.route) },
            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            label = { Text("Resumen") },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Ajustes") },
            colors = itemColors
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Faq.route,
            onClick = { onNavigate(Screen.Faq.route) },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            label = { Text("Guía") },
            colors = itemColors
        )
    }
}

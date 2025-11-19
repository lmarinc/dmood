package com.dmood.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dmood.app.di.DmoodServiceLocator
import com.dmood.app.ui.components.BottomNavBar
import com.dmood.app.ui.navigation.AppNavHost
import com.dmood.app.ui.navigation.Screen
import com.dmood.app.ui.theme.DmoodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DmoodServiceLocator.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            DmoodTheme {
                val locator = DmoodServiceLocator
                val startDestination by produceState<String?>(initialValue = null) {
                    value = if (locator.userPreferencesRepository.hasSeenOnboarding()) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }
                }

                if (startDestination == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando tu espacio emocional…")
                    }
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val bottomRoutes = setOf(
                        Screen.Home.route,
                        Screen.WeeklySummary.route,
                        Screen.Settings.route,
                        Screen.Faq.route
                    )
                    val showBottomBar = currentRoute != null && bottomRoutes.any { currentRoute == it }

                    if (showBottomBar) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                BottomNavBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        if (route != currentRoute) {
                                            navController.navigate(route) {
                                                popUpTo(Screen.Home.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AppNavHost(
                                    navController = navController,
                                    startDestination = startDestination!!
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppNavHost(
                                navController = navController,
                                startDestination = startDestination!!
                            )
                        }
                    }
                }
            }
        }
    }
}

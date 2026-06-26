package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.BuatKeluargaScreen
import com.example.ui.screens.DetailAnggotaScreen
import com.example.ui.screens.GabungKeluargaScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KeluargaScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfilScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GuardianViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GuardianFamilyApp()
                }
            }
        }
    }
}

enum class MainTab {
    Home, Keluarga, Profil
}

@Composable
fun GuardianFamilyApp() {
    val navController = rememberNavController()
    val viewModel: GuardianViewModel = viewModel()
    val currentUser by viewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(
                isLoggedIn = currentUser != null,
                onNavigateNext = {
                    if (currentUser != null) {
                        navController.navigate("main") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Login Screen
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Register Screen
        composable("register") {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        // Main Tabbed Container Screen
        composable("main") {
            var selectedTab by remember { mutableStateOf(MainTab.Home) }

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Tab 1: Home
                        NavigationBarItem(
                            selected = selectedTab == MainTab.Home,
                            onClick = { selectedTab = MainTab.Home },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == MainTab.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Beranda"
                                )
                            },
                            label = {
                                Text(
                                    "Home",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == MainTab.Home) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )

                        // Tab 2: Keluarga
                        NavigationBarItem(
                            selected = selectedTab == MainTab.Keluarga,
                            onClick = { selectedTab = MainTab.Keluarga },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == MainTab.Keluarga) Icons.Filled.People else Icons.Outlined.People,
                                    contentDescription = "Keluarga"
                                )
                            },
                            label = {
                                Text(
                                    "Keluarga",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == MainTab.Keluarga) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )

                        // Tab 3: Profil
                        NavigationBarItem(
                            selected = selectedTab == MainTab.Profil,
                            onClick = { selectedTab = MainTab.Profil },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == MainTab.Profil) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = "Profil"
                                )
                            },
                            label = {
                                Text(
                                    "Profil",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == MainTab.Profil) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (selectedTab) {
                        MainTab.Home -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToCreateFamily = { navController.navigate("create_family") },
                                onNavigateToJoinFamily = { navController.navigate("join_family") },
                                onNavigateToFamilyTab = { selectedTab = MainTab.Keluarga },
                                onNavigateToMemberDetail = { member ->
                                    viewModel.selectMember(member)
                                    navController.navigate("member_detail")
                                }
                            )
                        }
                        MainTab.Keluarga -> {
                            KeluargaScreen(
                                viewModel = viewModel,
                                onNavigateToCreateFamily = { navController.navigate("create_family") },
                                onNavigateToJoinFamily = { navController.navigate("join_family") },
                                onNavigateToMemberDetail = { member ->
                                    viewModel.selectMember(member)
                                    navController.navigate("member_detail")
                                }
                            )
                        }
                        MainTab.Profil -> {
                            ProfilScreen(
                                viewModel = viewModel,
                                onLogoutSuccess = {
                                    navController.navigate("login") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                },
                                onLeaveFamilySuccess = {
                                    selectedTab = MainTab.Home
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sub Screen: Create Family
        composable("create_family") {
            BuatKeluargaScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCreateSuccess = {
                    navController.navigate("main") {
                        popUpTo("create_family") { inclusive = true }
                    }
                }
            )
        }

        // Sub Screen: Join Family
        composable("join_family") {
            GabungKeluargaScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onJoinSuccess = {
                    navController.navigate("main") {
                        popUpTo("join_family") { inclusive = true }
                    }
                }
            )
        }

        // Sub Screen: Member Details
        composable("member_detail") {
            DetailAnggotaScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

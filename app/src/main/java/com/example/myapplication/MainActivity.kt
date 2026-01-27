/**
 * 应用主入口。
 *
 * 职责：
 * - 初始化应用导航（NavHost）。
 * - 管理全局 UI 状态（如底部导航栏的显示/隐藏）。
 * - 协调不同功能页面的切换（空间、搜索、清单、设置、引导页）。
 *
 * 上层用途：
 * - 整个应用的运行起点，持有 `SpaceViewModel` 并分发给各个 Screen 组件。
 */
package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.work.WorkScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val viewModel: SpaceViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        WorkScheduler.scheduleExpiryCheck(context)
    }

    val settings by viewModel.settings.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == "spaces" || currentRoute == "search" || currentRoute == "lists" || currentRoute == "settings"

    var suppressOnboardingUntil by remember { mutableLongStateOf(0L) }
    var wasOnboarding by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        if (currentRoute == "onboarding") {
            wasOnboarding = true
        } else if (wasOnboarding) {
            wasOnboarding = false
            suppressOnboardingUntil = System.currentTimeMillis() + 1500L
        }
    }

    LaunchedEffect(settings.hasSeenOnboarding, currentRoute) {
        val now = System.currentTimeMillis()
        if (!settings.hasSeenOnboarding && currentRoute != "onboarding" && now >= suppressOnboardingUntil) {
            navController.navigate("onboarding") { launchSingleTop = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "spaces",
                        onClick = { navController.navigate("spaces") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("空间") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "search",
                        onClick = { navController.navigate("search") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("搜索") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "lists",
                        onClick = { navController.navigate("lists") { launchSingleTop = true } },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("清单") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "spaces",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("spaces") {
                SpacesScreen(
                    viewModel = viewModel,
                    onSpaceClick = { id -> navController.navigate("space_detail/$id") }
                )
            }
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onOpenResult = { r ->
                        navController.navigate(
                            "space_detail/${Uri.encode(r.spaceId)}?spotId=${Uri.encode(r.spotId)}&itemId=${Uri.encode(r.itemId)}"
                        )
                    }
                )
            }
            composable("lists") {
                ListsScreen(
                    viewModel = viewModel,
                    onOpenList = { listId -> navController.navigate("list_detail/$listId") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenOnboarding = { navController.navigate("onboarding") { launchSingleTop = true } }
                )
            }
            composable("onboarding") {
                OnboardingScreen(
                    viewModel = viewModel,
                    onFinish = {
                        navController.navigate("spaces") {
                            popUpTo("onboarding") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = "list_detail/{listId}",
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) {
                ListDetailScreen(
                    viewModel = viewModel,
                    listId = it.arguments?.getString("listId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "space_detail/{spaceId}?spotId={spotId}&itemId={itemId}",
                arguments = listOf(
                    navArgument("spaceId") { type = NavType.StringType },
                    navArgument("spotId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null },
                )
            ) {
                SpaceDetailScreen(
                    viewModel = viewModel,
                    spaceId = it.arguments?.getString("spaceId") ?: "",
                    initialSpotId = it.arguments?.getString("spotId"),
                    highlightItemId = it.arguments?.getString("itemId"),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

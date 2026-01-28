/**
 * 应用主入口。
 *
 * 职责：
 * - 初始化应用导航（NavHost）。
 * - 管理全局 UI 状态（如底部导航栏的显示/隐藏）。
 * - 协调不同功能页面的切换（场所、搜索、清单、设置、引导页）。
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
    
    // 判断是否显示底部导航栏
    val showBottomBar = currentRoute == "locations" || 
                       currentRoute == "search" || 
                       currentRoute == "lists" || 
                       currentRoute == "settings"

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
                        selected = currentRoute == "locations",
                        onClick = { navController.navigate("locations") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("场所") }
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
            startDestination = "locations",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 场所列表页面（首页）
            composable("locations") {
                LocationsScreen(
                    viewModel = viewModel,
                    onLocationClick = { locationId -> 
                        navController.navigate("folder_browser/$locationId") 
                    }
                )
            }
            
            // 文件夹浏览器（场所根目录）
            composable(
                route = "folder_browser/{locationId}",
                arguments = listOf(navArgument("locationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val locationId = backStackEntry.arguments?.getString("locationId") ?: ""
                FolderBrowserScreen(
                    viewModel = viewModel,
                    locationId = locationId,
                    folderId = null,
                    onBack = { navController.popBackStack() },
                    onNavigateToFolder = { folderId ->
                        navController.navigate("folder_browser/$locationId/$folderId")
                    },
                    onOpenItem = { item ->
                        // 物品详情已在 FolderBrowserScreen 内部处理
                    }
                )
            }
            
            // 文件夹浏览器（子文件夹）
            composable(
                route = "folder_browser/{locationId}/{folderId}",
                arguments = listOf(
                    navArgument("locationId") { type = NavType.StringType },
                    navArgument("folderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val locationId = backStackEntry.arguments?.getString("locationId") ?: ""
                val folderId = backStackEntry.arguments?.getString("folderId")
                FolderBrowserScreen(
                    viewModel = viewModel,
                    locationId = locationId,
                    folderId = folderId,
                    onBack = { navController.popBackStack() },
                    onNavigateToFolder = { newFolderId ->
                        navController.navigate("folder_browser/$locationId/$newFolderId")
                    },
                    onOpenItem = { item ->
                        // 物品详情已在 FolderBrowserScreen 内部处理
                    }
                )
            }
            
            // 搜索页面
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onOpenResult = { result ->
                        // 导航到物品所在的文件夹
                        navController.navigate(
                            "folder_browser/${Uri.encode(result.locationId)}/${Uri.encode(result.folderId)}"
                        )
                    }
                )
            }
            
            // 清单列表页面
            composable("lists") {
                ListsScreen(
                    viewModel = viewModel,
                    onOpenList = { listId -> navController.navigate("list_detail/$listId") }
                )
            }
            
            // 设置页面
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenOnboarding = { navController.navigate("onboarding") { launchSingleTop = true } }
                )
            }
            
            // 引导页面
            composable("onboarding") {
                OnboardingScreen(
                    viewModel = viewModel,
                    onFinish = {
                        navController.navigate("locations") {
                            popUpTo("onboarding") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            // 清单详情页面
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
        }
    }
}

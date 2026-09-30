package com.ultimatetool.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ultimatetool.app.presentation.aichat.AiChatScreen
import com.ultimatetool.app.presentation.auth.LoginScreen
import com.ultimatetool.app.presentation.community.CommunityScreen
import com.ultimatetool.app.presentation.home.HomeScreen
import com.ultimatetool.app.presentation.linux.LinuxGuiScreen
import com.ultimatetool.app.presentation.linux.LinuxScreen
import com.ultimatetool.app.presentation.linux.LinuxTerminalScreen
import com.ultimatetool.app.presentation.plugins.PluginEditorScreen
import com.ultimatetool.app.presentation.plugins.PluginsScreen
import com.ultimatetool.app.presentation.profile.ProfileScreen
import com.ultimatetool.app.presentation.settings.SettingsScreen
import com.ultimatetool.app.presentation.splash.SplashScreen
import com.ultimatetool.app.presentation.tools.ToolScreen

/** Корневой граф навигации приложения. */
@Composable
fun UltimateToolNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onAuthed = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNeedLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onToolClick = { toolId -> navController.navigate(Routes.tool(toolId)) },
                onOpenAiChat = { navController.navigate(Routes.AI_CHAT) },
                onOpenPlugins = { navController.navigate(Routes.PLUGINS) },
                onOpenCommunity = { navController.navigate(Routes.COMMUNITY) },
                onOpenLinux = { navController.navigate(Routes.LINUX) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.LINUX) {
            LinuxScreen(
                onBack = { navController.popBackStack() },
                onTerminal = { distroId -> navController.navigate(Routes.linuxTerminal(distroId)) },
                onGui = { distroId -> navController.navigate(Routes.linuxGui(distroId)) }
            )
        }

        composable(Routes.LINUX_TERMINAL) {
            LinuxTerminalScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LINUX_GUI) { entry ->
            LinuxGuiScreen(
                distroId = entry.arguments?.getString("distroId"),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AI_CHAT) {
            AiChatScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PLUGINS) {
            PluginsScreen(
                onBack = { navController.popBackStack() },
                onCreate = { navController.navigate(Routes.pluginEditor(null)) },
                onEdit = { pluginId -> navController.navigate(Routes.pluginEditor(pluginId)) }
            )
        }

        composable(
            route = Routes.PLUGIN_EDITOR,
            arguments = listOf(
                navArgument("pluginId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            PluginEditorScreen(
                pluginId = entry.arguments?.getString("pluginId"),
                onDone = { navController.popBackStack() }
            )
        }

        composable(Routes.TOOL) { entry ->
            ToolScreen(
                toolId = entry.arguments?.getString("toolId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.COMMUNITY) {
            CommunityScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
